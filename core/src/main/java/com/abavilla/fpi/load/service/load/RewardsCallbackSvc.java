/******************************************************************************
 * FPI Application - Abavilla                                                 *
 * Copyright (C) 2022  Vince Jerald Villamora                                 *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.     *
 ******************************************************************************/

package com.abavilla.fpi.load.service.load;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;

import com.abavilla.fpi.fw.dto.impl.NullDto;
import com.abavilla.fpi.fw.dto.impl.RespDto;
import com.abavilla.fpi.fw.entity.mongo.AbsMongoItem;
import com.abavilla.fpi.fw.exceptions.ApiSvcEx;
import com.abavilla.fpi.fw.service.AbsSvc;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsCallbackDto;
import com.abavilla.fpi.load.entity.dtone.DVSCallback;
import com.abavilla.fpi.load.entity.load.CallBack;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.mapper.load.dtone.DTOneMapper;
import com.abavilla.fpi.load.mapper.load.gl.GLMapper;
import com.abavilla.fpi.load.repo.load.RewardsLeakRepo;
import com.abavilla.fpi.load.repo.load.RewardsTransRepo;
import com.abavilla.fpi.load.util.LoadConst;
import com.abavilla.fpi.login.ext.dto.UserDto;
import com.abavilla.fpi.login.ext.rest.UserApi;
import com.abavilla.fpi.msgr.ext.dto.MsgrMsgReqDto;
import com.abavilla.fpi.msgr.ext.rest.MsgrReqApi;
import com.abavilla.fpi.msgr.ext.rest.TelegramReqApi;
import com.abavilla.fpi.msgr.ext.rest.ViberReqApi;
import com.abavilla.fpi.sms.ext.dto.MsgReqDto;
import com.abavilla.fpi.sms.ext.rest.SmsApi;
import com.abavilla.fpi.telco.ext.enums.ApiStatus;
import com.dtone.dvs.dto.Transaction;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class RewardsCallbackSvc extends AbsSvc<GLRewardsCallbackDto, RewardsTransStatus> {

  @Inject
  RewardsTransRepo advRepo;

  @Inject
  RewardsLeakRepo leakRepo;

  @RestClient
  UserApi userApi;

  @Inject
  DTOneMapper dtOneMapper;

  @Inject
  GLMapper glMapper;

  /**
   * Service for sending SMS
   */
  @RestClient
  SmsApi smsApi;

  @RestClient
  MsgrReqApi msgrApi;

  @RestClient
  TelegramReqApi telegramReqApi;

  @RestClient
  ViberReqApi viberReqApi;

  @Inject
  PhoneNumberUtil phoneNumberUtil;

  public Uni<RespDto<NullDto>> storeCallback(GLRewardsCallbackDto callbackDto) {
    return storeCallback(
      glMapper.mapGLCallbackDtoToEntity(callbackDto),
      ApiStatus.fromGL(callbackDto.getBody().getStatus()),
      LoadConst.PROV_GL, callbackDto.getBody().getTransactionId());
  }

  public Uni<RespDto<NullDto>> storeCallback(Transaction dvsCallbackTransaction) {
    var dvsCallbackDto = dtOneMapper.mapDTOneTransactionToCallbackDto(dvsCallbackTransaction);
    return storeCallback(
      dtOneMapper.mapDTOneRespToEntity(dvsCallbackDto),
      ApiStatus.fromDtOne(dvsCallbackDto.getStatus().getId()),
      LoadConst.PROV_DTONE, dvsCallbackDto.getDtOneId());
  }

  private Uni<RespDto<NullDto>> storeCallback(AbsMongoItem callbackResponse, ApiStatus status,
                                  String provider, Long transactionId) {
    var byTransId = advRepo.findByRespTransIdAndProvider(
      String.valueOf(transactionId), provider);

    byTransId.chain(transPulled -> checkIfTxExists(transPulled.orElse(null), transactionId))
      .onFailure(ApiSvcEx.class).retry().withBackOff(
        Duration.ofSeconds(3)).withJitter(0.2)
      .atMost(5) // Retry for item not found and nothing else
      .chain(transFound -> updateTransWithCallback(transFound, callbackResponse, status))
      .chain(updatedTrans -> sendLoaderAckMsg(updatedTrans, callbackResponse, status))
      .chain(updatedTrans -> sendFPIAckMsg(updatedTrans, callbackResponse, status)).onFailure()
      .call(ex -> saveCallbackAsLeak(ex, callbackResponse, transactionId))
      .subscribe().with(ignored -> {
      });

    return Uni.createFrom().item(this::buildAckResponse);
  }

  /**
   * Checks if rewards transaction have been logged in the db, if not, throw an {@link ApiSvcEx} exception.
   *
   * @param rewardsTransStatus Entity in DB found
   * @param transactionId      External transaction id
   * @return @return {@link Function} callback
   */
  private Uni<? extends RewardsTransStatus> checkIfTxExists(RewardsTransStatus rewardsTransStatus, Long transactionId) {
    if (rewardsTransStatus != null) {
      return Uni.createFrom().item(rewardsTransStatus);
    } else {
      throw new ApiSvcEx("Trans Id for rewards callback not found: " + transactionId);
    }
  }

  /**
   * Updates the rewards transaction with the callback status.
   *
   * @param rewardsTrans Rewards transaction
   * @param field        Callback status
   * @param status       Status of transaction
   * @return {@link Function} callback
   */
  private Uni<? extends RewardsTransStatus> updateTransWithCallback(RewardsTransStatus rewardsTrans, AbsMongoItem field,
                                                                    ApiStatus status) {
    CallBack callBack = new CallBack();
    callBack.setContent(field);
    callBack.setDateReceived(DateUtil.now());
    callBack.setStatus(status);
    rewardsTrans.getApiCallback().add(callBack);
    rewardsTrans.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
    return repo.update(rewardsTrans);
  }

  /**
   * Logs the load transaction as a failed in the database
   *
   * @param ex            Failed transaction
   * @param field         Load transaction
   * @param transactionId External transaction id
   * @return {@link Function} callback
   */
  private Uni<?> saveCallbackAsLeak(Throwable ex, AbsMongoItem field, Long transactionId) {
    Log.error("Rewards leak " + transactionId, ex);
    field.setDateCreated(DateUtil.now());
    field.setDateUpdated(DateUtil.now());
    return leakRepo.persist(field)
      .onFailure().recoverWithNull();
  }

  /**
   * Determines if transaction requires an acknowledgement message for successful load transfer
   *
   * @param rewardsTransStatus Rewards transaction
   * @param status             Load status
   * @return {@link Function} callback
   */
  private Uni<?> sendFPIAckMsg(RewardsTransStatus rewardsTransStatus, AbsMongoItem callbackResponse,
                               ApiStatus status) {
    if (rewardsTransStatus.getLoadRequest() != null &&
      rewardsTransStatus.getLoadRequest().getSendAckMsg()) {
      Log.info("Sending FPI acknowledgement message " +
        rewardsTransStatus.getLoadSmsId() + " apiStatus: " + status);
      if (status == ApiStatus.DEL) {
        var req = new MsgReqDto();
        try {
          var number = phoneNumberUtil.parse(rewardsTransStatus.getLoadRequest().getMobile(),
            LoadConst.PH_REGION_CODE);
          req.setMobileNumber(phoneNumberUtil
            .format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
          Log.warn("Invalid recipient number, not sending ack message: " +
            rewardsTransStatus.getLoadRequest().getMobile());
          return Uni.createFrom().voidItem();
        }
        var pin = retrievePinFromCallBack(rewardsTransStatus, callbackResponse);
        String msgContent;
        if (StringUtils.isBlank(pin)) {
          msgContent = String.format("""
              %s was loaded to your account.
              Thank you for visiting Florenz Pension Inn!
              For reservations, message us at https://m.me/florenzpensioninn
                              
              Ref: %s""", rewardsTransStatus.getLoadRequest().getSku(),
            rewardsTransStatus.getLoadSmsId());
        } else {
          msgContent = String.format("""
              %s was loaded to your account.
              Thank you for visiting Florenz Pension Inn!
              For reservations, message us at https://m.me/florenzpensioninn
                              
              ePIN: %s
              Ref: %s""",
            rewardsTransStatus.getLoadRequest().getSku(),
            pin, rewardsTransStatus.getLoadSmsId());
        }
        req.setContent(msgContent);
        return smsApi.sendSms(req);
      } else {
        return Uni.createFrom().voidItem();
      }
    } else {
      Log.info("Skipping FPI acknowledgement message for " +
        rewardsTransStatus.getLoadSmsId() + " apiStatus: " + status);
      return Uni.createFrom().voidItem();
    }
  }

  private String retrievePinFromCallBack(RewardsTransStatus rewardsTransStatus, AbsMongoItem callbackResponse) {
    var pin = StringUtils.EMPTY;
    if (StringUtils.equals(rewardsTransStatus.getLoadProvider(), LoadConst.PROV_DTONE)) {
      var dvsCallback = (DVSCallback) callbackResponse;
      if (dvsCallback.getPin() != null && StringUtils.isNotBlank(dvsCallback.getPin().getCode())) {
        pin = dvsCallback.getPin().getCode();
      }
    }
    return pin;
  }

  private Uni<? extends RewardsTransStatus> sendLoaderAckMsg(RewardsTransStatus rewardsTransStatus,
                                                             AbsMongoItem callbackResponse,
                                                             ApiStatus status) {
    Log.info("Sending ack message to loader: " + rewardsTransStatus);
    String fpiUser = rewardsTransStatus.getFpiUser();
    String msgContentFormat;
    var pin = retrievePinFromCallBack(rewardsTransStatus, callbackResponse);

    if (StringUtils.isBlank(pin)) {
      msgContentFormat = """
        Loaded %s to %s
                  
        S: %s
        Ref: %s""".formatted(
        rewardsTransStatus.getLoadRequest().getSku(),
        StringUtils.isBlank(rewardsTransStatus.getLoadRequest().getAccountNo()) ?
          rewardsTransStatus.getLoadRequest().getMobile() : rewardsTransStatus.getLoadRequest().getAccountNo(),
        String.valueOf(status), rewardsTransStatus.getLoadSmsId());
    } else {
      msgContentFormat = """
        Loaded %s to %s
                  
        ePIN: %s
        S: %s
        Ref: %s""".formatted(
        rewardsTransStatus.getLoadRequest().getSku(),
        StringUtils.isBlank(rewardsTransStatus.getLoadRequest().getAccountNo()) ?
          rewardsTransStatus.getLoadRequest().getMobile() : rewardsTransStatus.getLoadRequest().getAccountNo(),
        pin, String.valueOf(status), rewardsTransStatus.getLoadSmsId());
    }

    final var msgContent = msgContentFormat; // force to final for lambda usage
    return userApi.getById(fpiUser).chain(resp -> {
      var botMsg = new MsgrMsgReqDto();
      botMsg.setContent(msgContent);
      var sendLoaderMsg = sendToBotSource(rewardsTransStatus, resp.getResp(), botMsg);
      if (StringUtils.isNotBlank(resp.getResp().getMobile())) {
        var msg = new MsgReqDto();
        msg.setContent(msgContent);
        msg.setMobileNumber(resp.getResp().getMobile());
        sendLoaderMsg = sendLoaderMsg.chain(() -> smsApi.sendSms(msg));
      }
      return sendLoaderMsg;
    }).chain(() -> Uni.createFrom().item(rewardsTransStatus));
  }

  private Uni<?> sendToBotSource(RewardsTransStatus rewardsTransStatus, UserDto user, MsgrMsgReqDto msgrMsg) {
    return switch (rewardsTransStatus.getSource()) {
      case FB_MSGR -> {
        msgrMsg.setRecipient(user.getMetaId());
        yield msgrApi.toggleTyping(user.getMetaId(), true)
          .chain(() -> msgrApi.sendMsg(msgrMsg, user.getId()))
          .chain(() -> msgrApi.toggleTyping(user.getMetaId(), false));
      }
      case TELEGRAM -> {
        msgrMsg.setRecipient(user.getTelegramId());
        yield telegramReqApi.toggleTyping(user.getTelegramId())
          .chain(() -> telegramReqApi.sendMsg(msgrMsg, user.getId()));
      }
      case VIBER -> {
        msgrMsg.setRecipient(user.getViberId());
        yield viberReqApi.sendMsg(msgrMsg, user.getId());
      }
      default -> Uni.createFrom().voidItem();
    };
  }

  public RespDto<NullDto> buildAckResponse() {
    RespDto<NullDto> ackResp = new RespDto<>();
    ackResp.setTimestamp(DateUtil.nowAsStr());
    ackResp.setStatus(HttpResponseStatus.OK.reasonPhrase());
    return ackResp;
  }

}
