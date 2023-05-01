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

import java.util.Optional;

import com.abavilla.fpi.fw.exceptions.FPISvcEx;
import com.abavilla.fpi.fw.service.AbsSvc;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsReqDto;
import com.abavilla.fpi.load.engine.load.LoadEngine;
import com.abavilla.fpi.load.entity.load.PromoSku;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.ext.dto.LoadRespDto;
import com.abavilla.fpi.load.mapper.load.RewardsTransStatusMapper;
import com.abavilla.fpi.load.util.LoadConst;
import com.abavilla.fpi.load.util.LoadUtil;
import com.abavilla.fpi.telco.ext.enums.ApiStatus;
import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.RestResponse;

@ApplicationScoped
public class RewardsSvc extends AbsSvc<GLRewardsReqDto, RewardsTransStatus> {

  @Inject
  RewardsTransStatusMapper rewardsMapper;

  @Inject
  PromoSkuSvc promoSkuSvc;

  @Inject
  LoadEngine loadEngine;

  @Inject
  SecurityIdentity identity;

  public Uni<LoadRespDto> reloadNumber(LoadReqDto loadReqDto) {
    Log.info("Charging credits to :" + loadReqDto);
    // create log to db
    var log = new RewardsTransStatus();
    rewardsMapper.mapLoadReqToEntity(loadReqDto, log);
    log.setFpiUser(identity.getPrincipal().getName());
    log.setDateCreated(DateUtil.now());

    Uni<Optional<PromoSku>> skuLookup;
    if (StringUtils.isBlank(loadReqDto.getTelco())) {
      // if no telco is provided in request, use the promo found first using keywords
      skuLookup = promoSkuSvc.findSkuByDefaultOperator(loadReqDto);
    } else {
      skuLookup = promoSkuSvc.findSku(loadReqDto);
    }

    return skuLookup.chain(promo -> {
      ILoadProviderSvc loadSvc = promo
          .map(promoSku -> loadEngine.getProvider(promoSku))
          .orElse(null);

      if (loadSvc != null) {
        log.setLoadProvider(loadSvc.getProviderName());
        log.setDateUpdated(DateUtil.now());
        return repo.persist(log)
            .chain(savedLog -> reloadAndUpdateDb(savedLog, loadReqDto, promo.get(), loadSvc));
      } else {
        return buildRejectedResponse();
      }
    });
  }

  private Uni<? extends LoadRespDto> reloadAndUpdateDb(RewardsTransStatus savedLog, LoadReqDto loadReqDto,
                                                       PromoSku promo, ILoadProviderSvc loadSvcProvider) {
    loadReqDto.setTransactionId(savedLog.getId().toString()); // map mongo id to load request
    return loadSvcProvider.reload(loadReqDto, promo)
        .chain(resp -> updateRequestInDb(resp, savedLog));
  }

  private Uni<LoadRespDto> buildRejectedResponse() {
    var resp = new LoadRespDto();
    resp.setStatus(ApiStatus.REJ);
    resp.setError(LoadConst.NO_LOAD_PROVIDER_AVAILABLE);
    var ex = new FPISvcEx(LoadConst.NO_LOAD_PROVIDER_AVAILABLE,
      RestResponse.StatusCode.BAD_REQUEST, resp);
    return Uni.createFrom().failure(ex);
  }

  private Uni<? extends LoadRespDto> updateRequestInDb(
      LoadRespDto loadRespDto,
      RewardsTransStatus logEntity) {
      rewardsMapper.mapLoadRespDtoToEntity(
        loadRespDto, logEntity);

      if (loadRespDto.getStatus() == ApiStatus.WAIT ||
          loadRespDto.getStatus() == ApiStatus.CREATED) {
        // only generate a load sms id if there was no error encountered
        // during load transaction during external load api call
        logEntity.setLoadSmsId(LoadUtil.encodeId(logEntity.getLoadProvider(), logEntity.getTransactionId()));
      }

      logEntity.setDateUpdated(DateUtil.now());
      return repo.update(logEntity)
        .map(res -> {
          loadRespDto.setSmsTransactionId(res.getLoadSmsId());
          return loadRespDto;
        });
  }

}
