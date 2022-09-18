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
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.mapper.load.LoadReqEntityMapper;
import com.abavilla.fpi.load.mapper.load.RewardsTransStatusMapper;
import com.abavilla.fpi.load.util.LoadConst;
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
      ILoadProviderSvc loadSvc = null;
      if (promo.isPresent()) {
        loadSvc = loadEngine.getProvider(promo.get());
      }
      var loadReq = loadReqMapper.mapToEntity(loadReqDto);
      log.setLoadRequest(loadReq);

      final var loadSvcProvider = loadSvc;
      if (loadSvcProvider != null) {
        log.setLoadProvider(loadSvc.getProviderName());
        log.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
        var logJob = repo.persist(log);
        return logJob
            .chain(logEntity -> {
              loadReqDto.setTransactionId(logEntity.getId().toString());
              return loadSvcProvider.reload(loadReqDto, promo.get())
                  .chain(loadRespDto -> {
                    dtoToEntityMapper.mapLoadRespDtoToEntity(
                        loadRespDto, logEntity
                    );
                    log.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
                    return repo.persistOrUpdate(logEntity)
                        .map(res -> loadRespDto);
                  })
                  .onFailure(ApiSvcEx.class).recoverWithItem(apiEx -> {
                    var errorResp = new LoadRespDto();
                    errorResp.setError(apiEx.getMessage());
                    errorResp.setTimestamp(DateUtil.nowAsStr());
                    errorResp.setTransactionId(logEntity.getTransactionId());
                    errorResp.setStatus(ApiStatus.REJ);
                    return errorResp;
                  });
            })
            .map(loadRespDto -> Response.status(loadRespDto.getError() == null ?
                    Response.Status.CREATED : Response.Status.NOT_ACCEPTABLE)
                .entity(loadRespDto)
                .build());
      } else {
        var resp = new LoadRespDto();
        resp.setStatus(ApiStatus.REJ);
        resp.setError(LoadConst.NO_LOAD_PROVIDER_AVAILABLE);
        resp.setTimestamp(DateUtil.nowAsStr());
        return Uni.createFrom().item(Response
            .status(Response.Status.NOT_ACCEPTABLE)
            .entity(resp)
            .build());
      }
    });

//    return
//        loadApi.sendLoad(apiReq)
//            .onFailure(ApiSvcEx.class).recoverWithItem(ex -> {
//              var apiEx = (ApiSvcEx) ex;
//              var resp = apiEx.getJsonResponse(RewardsRespDto.class);
//              resp.chainEx(apiEx);
//              return resp;
//            })
//            .chain(resp -> {
//              rewardsMapper.mapRespDtoToEntity(resp, log);
//              log.setDateCreated(LocalDateTime.now(ZoneOffset.UTC));
//              log.setDateUpdated(LocalDateTime.now(ZoneOffset.UTC));
//              return repo.persist(log).chain(()->Uni.createFrom().item(log));
//            })
//            .map(rewardsTransStatus -> {
//              var dtoResp = loadRespMapper.mapToDto(rewardsTransStatus.getResponse());
//              if (rewardsTransStatus.lastEx() == null) {
//                return Response.ok().entity(dtoResp).build();
//              } else {
//                if (rewardsTransStatus.lastEx() instanceof ApiSvcEx) {
//                  return Response.status(((ApiSvcEx)rewardsTransStatus.lastEx())
//                      .getHttpResponseStatus().code()).entity(dtoResp).build();
//                } else {
//                  return Response.serverError().entity(dtoResp).build();
//                }
//              }
//            });
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
