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
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.abavilla.fpi.fw.exceptions.FPISvcEx;
import com.abavilla.fpi.fw.service.AbsSvc;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.dto.load.LoadRespDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsReqDto;
import com.abavilla.fpi.load.engine.load.LoadEngine;
import com.abavilla.fpi.load.entity.enums.ApiStatus;
import com.abavilla.fpi.load.entity.load.PromoSku;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.mapper.load.LoadReqEntityMapper;
import com.abavilla.fpi.load.mapper.load.RewardsTransStatusMapper;
import com.abavilla.fpi.load.util.LoadConst;
import com.abavilla.fpi.load.util.LoadUtil;
import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class RewardsSvc extends AbsSvc<GLRewardsReqDto, RewardsTransStatus> {

  @Inject
  LoadReqEntityMapper loadReqMapper;

  @Inject
  RewardsTransStatusMapper dtoToEntityMapper;

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

      var loadReq = loadReqMapper.mapToEntity(loadReqDto);
      log.setLoadRequest(loadReq);

      if (loadSvc != null) {
        log.setLoadProvider(loadSvc.getProviderName());
        log.setDateUpdated(DateUtil.now());
        return repo.persist(log)
            .chain(reloadAndUpdateDb(loadReqDto, promo.get(), loadSvc));
      } else {
        return buildRejectedResponse();
      }
    });
  }

  private Function<RewardsTransStatus, Uni<? extends LoadRespDto>> reloadAndUpdateDb(LoadReqDto loadReqDto, PromoSku promo, ILoadProviderSvc loadSvcProvider) {
    return savedLog -> {
      loadReqDto.setTransactionId(savedLog.getId().toString()); // map mongo id to load request
      return loadSvcProvider.reload(loadReqDto, promo)
          .chain(updateRequestInDb(savedLog));
    };
  }

  private Uni<LoadRespDto> buildRejectedResponse() {
    var resp = new LoadRespDto();
    resp.setStatus(ApiStatus.REJ);
    resp.setError(LoadConst.NO_LOAD_PROVIDER_AVAILABLE);
    var ex = new FPISvcEx(LoadConst.NO_LOAD_PROVIDER_AVAILABLE, Response.Status.BAD_REQUEST.getStatusCode());
    ex.setEntity(resp);
    return Uni.createFrom().failure(ex);
  }

//  private Function<LoadRespDto, Response> determineReloadResponse() {
//    return loadRespDto ->
//        Response.status(loadRespDto.getError() == null ?
//                Response.Status.CREATED : Response.Status.NOT_ACCEPTABLE)
//            .entity(loadRespDto)
//            .build();
//  }

  private Function<LoadRespDto, Uni<? extends LoadRespDto>> updateRequestInDb(
      RewardsTransStatus logEntity) {
    return loadRespDto -> {
      dtoToEntityMapper.mapLoadRespDtoToEntity(
          loadRespDto, logEntity
      );

      if (loadRespDto.getStatus() == ApiStatus.WAIT ||
          loadRespDto.getStatus() == ApiStatus.CREATED) {
        // only generate a load sms id if there was no error encountered
        // during load transaction during external load api call
        logEntity.setLoadSmsId(LoadUtil.encodeId(logEntity.getLoadProvider(), logEntity.getTransactionId()));
      }

      logEntity.setDateUpdated(DateUtil.now());
      return repo.persistOrUpdate(logEntity)
          .map(res -> {
            loadRespDto.setSmsTransactionId(res.getLoadSmsId());
            return loadRespDto;
          });
    };
  }

//  private Function<Throwable, LoadRespDto> handleReloadException(RewardsTransStatus logEntity) {
//    return apiEx -> {
//      var errorResp = new LoadRespDto();
//      errorResp.setError(apiEx.getMessage());
//      errorResp.setTimestamp(DateUtil.nowAsStr());
//      errorResp.setTransactionId(logEntity.getTransactionId());
//      errorResp.setStatus(ApiStatus.REJ);
//      return errorResp;
//    };
//  }

}
