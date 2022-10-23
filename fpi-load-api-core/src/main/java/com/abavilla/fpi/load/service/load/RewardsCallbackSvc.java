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
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.abavilla.fpi.fw.entity.mongo.AbsMongoItem;
import com.abavilla.fpi.fw.exceptions.ApiSvcEx;
import com.abavilla.fpi.fw.service.AbsSvc;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.load.dtone.DVSCallbackDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsCallbackDto;
import com.abavilla.fpi.load.dto.sms.MsgReqDto;
import com.abavilla.fpi.load.entity.load.CallBack;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.ext.entity.enums.ApiStatus;
import com.abavilla.fpi.load.mapper.load.dtone.DTOneMapper;
import com.abavilla.fpi.load.mapper.load.gl.GLMapper;
import com.abavilla.fpi.load.repo.load.RewardsLeakRepo;
import com.abavilla.fpi.load.repo.load.RewardsTransRepo;
import com.abavilla.fpi.load.rest.sms.SmsApi;
import com.abavilla.fpi.load.util.LoadConst;
import com.abavilla.fpi.login.ext.rest.UserApi;
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
  SmsApi smsRepo;

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

    byTransId.chain(checkIfTxExists(transactionId))
      .onFailure(ApiSvcEx.class).retry().withBackOff(
          Duration.ofSeconds(3)).withJitter(0.2)
      .atMost(5) // Retry for item not found and nothing else
      .chain(updateTransWithCallback(field, status))
      .chain(sendFPIAckMsg(status)).onFailure()
      .call(saveCallbackAsLeak(field, transactionId))
      .subscribe().with(ignored->{});

    return Uni.createFrom().voidItem();
  }

  /**
   * Checks if rewards transaction have been logged in the db, if not, throw an {@link ApiSvcEx} exception.
   *
   * @param transactionId External transaction id
   * @return @return {@link Function} callback
   */
  private static Function<Optional<RewardsTransStatus>, Uni<? extends RewardsTransStatus>> checkIfTxExists(Long transactionId) {
    return rewardsTransStatusOpt -> {
      if (rewardsTransStatusOpt.isPresent()) {
        return Uni.createFrom().item(rewardsTransStatusOpt.get());
      } else {
        throw new ApiSvcEx("Trans Id for rewards callback not found: " + transactionId);
      }
    };
  }

  /**
   * Updates the rewards transaction with the callback status.
   *
   * @param field Rewards transaction
   * @param status Status of transaction
   * @return {@link Function} callback
   */
  private Function<RewardsTransStatus, Uni<? extends RewardsTransStatus>> updateTransWithCallback(AbsMongoItem field, ApiStatus status) {
    return rewardsTrans -> {
      //rewardsMapper.mapCallbackDtoToEntity(dto, rewardsTrans);
      CallBack callBack = new CallBack();
      callBack.setContent(field);
      callBack.setDateReceived(DateUtil.now());
      callBack.setStatus(status);
      rewardsTrans.getApiCallback().add(callBack);
      rewardsTrans.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
      return repo.persistOrUpdate(rewardsTrans);
    };
  }

  /**
   * Logs the load transaction as a failed in the database
   *
   * @param field Load transaction
   * @param transactionId External transaction id
   * @return {@link Function} callback
   */
  private Function<Throwable, Uni<?>> saveCallbackAsLeak(AbsMongoItem field, Long transactionId) {
    return ex -> { // leaks/delay
      Log.error("Rewards leak " + transactionId, ex);
      field.setDateCreated(DateUtil.now());
      field.setDateUpdated(DateUtil.now());
      return leakRepo.persist(field)
          .onFailure().recoverWithNull();
    };
  }

  /**
   * Sends an acknowledgement message for successful load transfer
   *
   * @param status Load status
   * @return {@link Function} callback
   */
  private Function<RewardsTransStatus, Uni<?>> sendFPIAckMsg(ApiStatus status) {
    return rewardsTransStatus -> {
      Log.info("Received callback for " +
          rewardsTransStatus.getLoadSmsId() + " apiStatus: " + status);
      if (status == ApiStatus.DEL) {
        var req = new MsgReqDto();
        try {
          var number = phoneNumberUtil.parse(rewardsTransStatus.getLoadRequest().getMobile(),
              LoadConst.PH_REGION_CODE);
          req.setMobileNumber(phoneNumberUtil
              .format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
          return Uni.createFrom().failure(e);
        }
        req.setContent(
            String.format(
                "%s was loaded to your account. Thank you for visiting Florenz Pension Inn! " +
                "For reservations, message us at https://m.me/florenzpensioninn" +
                "\n\nRef: %s", rewardsTransStatus.getLoadRequest().getSku(),
                rewardsTransStatus.getLoadSmsId()));
        return smsRepo.sendSms(req);
      } else {
        return Uni.createFrom().voidItem();
      }
    };
  }
}
