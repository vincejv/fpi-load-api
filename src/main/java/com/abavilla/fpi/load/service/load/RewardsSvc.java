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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.abavilla.fpi.fw.exceptions.ApiSvcEx;
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
import com.abavilla.fpi.load.util.LoadUtils;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.NotImplementedException;

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

  public Uni<Response> reloadNumber(LoadReqDto loadReqDto) {

    // create log to db
    var log = new RewardsTransStatus();
    log.setDateCreated(LocalDateTime.now(ZoneOffset.UTC));

    return promoSkuSvc.findSku(loadReqDto).chain(promo -> {
      ILoadProviderSvc loadSvc = promo
          .map(promoSku -> loadEngine.getProvider(promoSku))
          .orElse(null);

      var loadReq = loadReqMapper.mapToEntity(loadReqDto);
      log.setLoadRequest(loadReq);

      if (loadSvc != null) {
        log.setLoadProvider(loadSvc.getProviderName());
        log.setDateUpdated(DateUtil.now());
        return repo.persist(log)
            .chain(reloadAndUpdateDb(loadReqDto, log, promo.get(), loadSvc))
            .map(determineReloadResponse());
      } else {
        return buildRejectedResponse();
      }
    });
  }

  private Function<RewardsTransStatus, Uni<? extends LoadRespDto>> reloadAndUpdateDb(LoadReqDto loadReqDto, RewardsTransStatus log, PromoSku promo, ILoadProviderSvc loadSvcProvider) {
    return logEntity -> {
      loadReqDto.setTransactionId(logEntity.getId().toString());
      return loadSvcProvider.reload(loadReqDto, promo)
          .onFailure(ApiSvcEx.class)
          .recoverWithItem(handleReloadException(logEntity))
          .chain(saveRequestToDb(log, logEntity));
    };
  }

  private Uni<Response> buildRejectedResponse() {
    var resp = new LoadRespDto();
    resp.setStatus(ApiStatus.REJ);
    resp.setError(LoadConst.NO_LOAD_PROVIDER_AVAILABLE);
    resp.setTimestamp(DateUtil.nowAsStr());
    return Uni.createFrom().item(Response
        .status(Response.Status.NOT_ACCEPTABLE)
        .entity(resp)
        .build());
  }

  private Function<LoadRespDto, Response> determineReloadResponse() {
    return loadRespDto ->
        Response.status(loadRespDto.getError() == null ?
                Response.Status.CREATED : Response.Status.NOT_ACCEPTABLE)
            .entity(loadRespDto)
            .build();
  }

  private Function<LoadRespDto, Uni<? extends LoadRespDto>> saveRequestToDb(RewardsTransStatus log, RewardsTransStatus logEntity) {
    return loadRespDto -> {
      dtoToEntityMapper.mapLoadRespDtoToEntity(
          loadRespDto, logEntity
      );
      log.setLoadSmsId(LoadUtils.encodeId(log.getLoadProvider(), log.getTransactionId()));
      log.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
      return repo.persistOrUpdate(logEntity)
          .map(res -> loadRespDto);
    };
  }

  private Function<Throwable, LoadRespDto> handleReloadException(RewardsTransStatus logEntity) {
    return apiEx -> {
      var errorResp = new LoadRespDto();
      errorResp.setError(apiEx.getMessage());
      errorResp.setTimestamp(DateUtil.nowAsStr());
      errorResp.setTransactionId(logEntity.getTransactionId());
      errorResp.setStatus(ApiStatus.REJ);
      return errorResp;
    };
  }

  @Override
  public GLRewardsReqDto mapToDto(RewardsTransStatus entity) {
    throw new NotImplementedException();
  }

  @Override
  public RewardsTransStatus mapToEntity(GLRewardsReqDto dto) {
    throw new NotImplementedException();
  }
}
