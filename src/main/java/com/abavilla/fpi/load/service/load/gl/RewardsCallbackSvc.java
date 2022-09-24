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

package com.abavilla.fpi.load.service.load.gl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.abavilla.fpi.fw.entity.mongo.AbsMongoItem;
import com.abavilla.fpi.fw.exceptions.ApiSvcEx;
import com.abavilla.fpi.fw.service.AbsSvc;
import com.abavilla.fpi.load.dto.load.dtone.DVSCallbackDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsCallbackDto;
import com.abavilla.fpi.load.dto.sms.MsgReqDto;
import com.abavilla.fpi.load.entity.enums.ApiStatus;
import com.abavilla.fpi.load.entity.load.CallBack;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.mapper.load.dtone.DTOneMapper;
import com.abavilla.fpi.load.mapper.load.gl.GLMapper;
import com.abavilla.fpi.load.repo.load.RewardsLeakRepo;
import com.abavilla.fpi.load.repo.load.RewardsTransRepo;
import com.abavilla.fpi.load.repo.sms.SmsRepo;
import com.abavilla.fpi.load.util.LoadConst;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class RewardsCallbackSvc extends AbsSvc<GLRewardsCallbackDto, RewardsTransStatus> {

  @Inject
  RewardsTransRepo advRepo;

  @Inject
  RewardsLeakRepo leakRepo;

  @Inject
  DTOneMapper dtOneMapper;

  @Inject
  GLMapper glMapper;

  /**
   * Service for sending SMS
   */
  @RestClient
  SmsRepo smsRepo;

  @Inject
  PhoneNumberUtil phoneNumberUtil;

  /**
   * Runs background tasks from webhook
   */
  @Inject
  ManagedExecutor executor;

  public Uni<Void> storeCallback(GLRewardsCallbackDto dto) {
    ApiStatus status = ApiStatus.ACK;
    return storeCallback(glMapper.mapGLCallbackDtoToEntity(dto),
        status, LoadConst.PROV_GL, dto.getBody().getTransactionId());
  }

  public Uni<Void> storeCallback(DVSCallbackDto dto) {
    ApiStatus status = ApiStatus.ACK;
    return storeCallback(dtOneMapper.mapDTOneRespToEntity(dto),
        status, LoadConst.PROV_DTONE, dto.getDtOneId());
  }

  private Uni<Void> storeCallback(AbsMongoItem field, ApiStatus status,
                                  String provider, Long transactionId) {
    var byTransId = advRepo.findByRespTransIdAndProvider(
        String.valueOf(transactionId), provider);

    return byTransId.chain(rewardsTransStatusOpt -> {
            if (rewardsTransStatusOpt.isPresent()) {
              return Uni.createFrom().item(rewardsTransStatusOpt.get());
            } else {
              throw new ApiSvcEx("Trans Id for rewards callback not found: " + transactionId);
            }
          })
          .onFailure(ApiSvcEx.class).retry().withBackOff(Duration.ofSeconds(3)).withJitter(0.2)
          .atMost(5) // Retry for item not found and nothing else
          .chain(rewardsTrans -> {
            //rewardsMapper.mapCallbackDtoToEntity(dto, rewardsTrans);
            CallBack callBack = new CallBack();
            callBack.setContent(field);
            callBack.setDateReceived(LocalDateTime.now(ZoneOffset.UTC));
            callBack.setStatus(status);
            rewardsTrans.getApiCallback().add(callBack);
            rewardsTrans.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
            return repo.persistOrUpdate(rewardsTrans);
          })
          .chain(rewardsTransStatus -> {
            var req = new MsgReqDto();
            try {
              var number = phoneNumberUtil.parse(rewardsTransStatus.getLoadRequest().getMobile(),
                  LoadConst.PH_REGION_CODE);
              req.setMobileNumber(phoneNumberUtil
                  .format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
            } catch (NumberParseException e) {
              throw new RuntimeException(e);
            }
            req.setContent("Thank you for visiting Florenz Pension Inn!");
            return smsRepo.sendSms(req);
          })
          .onFailure().call(ex -> { // leaks/delay
            Log.error("Rewards leak " + transactionId, ex);
            field.setDateCreated(LocalDateTime.now(ZoneOffset.UTC));
            field.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
            return leakRepo.persist(field);
          }).onFailure().recoverWithNull().replaceWithVoid();
  }

  @Override
  public GLRewardsCallbackDto mapToDto(RewardsTransStatus entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RewardsTransStatus mapToEntity(GLRewardsCallbackDto dto) {
    //return rewardsMapper.mapCallbackDtoToEntity(dto);
    throw new UnsupportedOperationException();
  }
}
