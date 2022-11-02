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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.abavilla.fpi.fw.entity.mongo.AbsMongoItem;
import com.abavilla.fpi.fw.exceptions.ApiSvcEx;
import com.abavilla.fpi.fw.service.AbsSvc;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.load.dtone.DVSCallbackDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsCallbackDto;
import com.abavilla.fpi.load.entity.load.CallBack;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.mapper.load.dtone.DTOneMapper;
import com.abavilla.fpi.load.mapper.load.gl.GLMapper;
import com.abavilla.fpi.load.repo.load.RewardsLeakRepo;
import com.abavilla.fpi.load.repo.load.RewardsTransRepo;
import com.abavilla.fpi.load.util.LoadConst;
import com.abavilla.fpi.login.ext.rest.UserApi;
import com.abavilla.fpi.sms.ext.dto.MsgReqDto;
import com.abavilla.fpi.sms.ext.rest.SmsApi;
import com.abavilla.fpi.telco.ext.entity.enums.ApiStatus;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
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

  @Inject
  PhoneNumberUtil phoneNumberUtil;

  public Uni<Void> storeCallback(GLRewardsCallbackDto dto) {
    ApiStatus status;

    if (StringUtils.equals(dto.getBody().getStatus(),
        LoadConst.GL_SUCCESS_STS)) {
      status = ApiStatus.DEL;
    } else if (StringUtils.equals(dto.getBody().getStatus(),
        LoadConst.GL_FAILED_STS)) {
      status = ApiStatus.REJ;
    } else {
      status = ApiStatus.fromValue(dto.getBody().getStatus());
    }

    return storeCallback(glMapper.mapGLCallbackDtoToEntity(dto),
        status, LoadConst.PROV_GL, dto.getBody().getTransactionId());
  }

  public Uni<Void> storeCallback(DVSCallbackDto dto) {
    ApiStatus status;

    if (dto.getStatus().getId() == LoadConst.DT_SUCCESS_STS) {
      status = ApiStatus.DEL;
    } else if (dto.getStatus().getId() == LoadConst.DT_INVPREPAID_STS) {
      // postpaid number is reloaded with prepaid credits
      status = ApiStatus.INV;
    }else if (dto.getStatus().getId() == LoadConst.DT_OPMISMATCH_STS) {
      // operator and mobile number mismatch
      status = ApiStatus.REJ;
    } else {
      status = ApiStatus.fromValue(dto.getStatus().getMessage());
    }

    return storeCallback(dtOneMapper.mapDTOneRespToEntity(dto),
        status, LoadConst.PROV_DTONE, dto.getDtOneId());
  }

  private Uni<Void> storeCallback(AbsMongoItem field, ApiStatus status,
                                  String provider, Long transactionId) {
    var byTransId = advRepo.findByRespTransIdAndProvider(
        String.valueOf(transactionId), provider);

    byTransId.chain(transPulled -> checkIfTxExists(transPulled.orElse(null), transactionId))
      .onFailure(ApiSvcEx.class).retry().withBackOff(
          Duration.ofSeconds(3)).withJitter(0.2)
      .atMost(5) // Retry for item not found and nothing else
      .chain(transFound -> updateTransWithCallback(transFound, field, status))
      .chain(updatedTrans -> sendLoaderAckMsg(updatedTrans, status))
      .chain(updatedTrans -> sendFPIAckMsg(updatedTrans, status)).onFailure()
      .call(ex -> saveCallbackAsLeak(ex, field, transactionId))
      .subscribe().with(ignored->{});

    return Uni.createFrom().voidItem();
  }

  /**
   * Checks if rewards transaction have been logged in the db, if not, throw an {@link ApiSvcEx} exception.
   *
   * @param rewardsTransStatus Entity in DB found
   * @param transactionId External transaction id
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
   * @param field Callback status
   * @param status Status of transaction
   * @return {@link Function} callback
   */
  private Uni<? extends RewardsTransStatus> updateTransWithCallback(RewardsTransStatus rewardsTrans, AbsMongoItem field, ApiStatus status) {
    CallBack callBack = new CallBack();
    callBack.setContent(field);
    callBack.setDateReceived(DateUtil.now());
    callBack.setStatus(status);
    rewardsTrans.getApiCallback().add(callBack);
    rewardsTrans.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
    return repo.persistOrUpdate(rewardsTrans);
  }

  /**
   * Logs the load transaction as a failed in the database
   *
   * @param ex Failed transaction
   * @param field Load transaction
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
   * Sends an acknowledgement message for successful load transfer
   *
   * @param rewardsTransStatus Rewards transaction
   * @param status Load status
   * @return {@link Function} callback
   */
  private Uni<?> sendFPIAckMsg(RewardsTransStatus rewardsTransStatus, ApiStatus status) {
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
        req.setContent(
            String.format("""
                %s was loaded to your account.
                Thank you for visiting Florenz Pension Inn!
                For reservations, message us at https://m.me/florenzpensioninn
                
                Ref: %s""", rewardsTransStatus.getLoadRequest().getSku(),
                rewardsTransStatus.getLoadSmsId()));
        return smsApi.sendSms(req);
      } else {
        return Uni.createFrom().voidItem();
      }
  }

  private Uni<? extends RewardsTransStatus> sendLoaderAckMsg(RewardsTransStatus rewardsTransStatus,
                                                             ApiStatus status) {
    Log.info("Sending ack message to loader: " + rewardsTransStatus);
    String fpiUser = rewardsTransStatus.getFpiUser();
    return userApi.getByMetaId(fpiUser).chain(resp -> {
      if (StringUtils.isNotBlank(resp.getResp().getMobile())) {
        var msg = new MsgReqDto();
        msg.setMobileNumber(resp.getResp().getMobile());
        msg.setContent("""
          Loaded %s to %s
          
          S: %s
          Ref: %s""".formatted(
            rewardsTransStatus.getLoadRequest().getSku(),
            StringUtils.isBlank(rewardsTransStatus.getLoadRequest().getAccountNo()) ?
              rewardsTransStatus.getLoadRequest().getMobile() : rewardsTransStatus.getLoadRequest().getAccountNo(),
            String.valueOf(status), rewardsTransStatus.getLoadSmsId()));
        return smsApi.sendSms(msg);
      }
      return Uni.createFrom().voidItem();
    }).chain(() -> Uni.createFrom().item(rewardsTransStatus));
  }

}
