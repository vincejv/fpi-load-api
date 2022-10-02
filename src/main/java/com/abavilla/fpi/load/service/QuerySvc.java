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

package com.abavilla.fpi.load.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.abavilla.fpi.fw.dto.AbsDto;
import com.abavilla.fpi.fw.dto.impl.RespDto;
import com.abavilla.fpi.fw.exceptions.FPISvcEx;
import com.abavilla.fpi.fw.service.AbsRepoSvc;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.QueryDto;
import com.abavilla.fpi.load.dto.QueryRespDto;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.entity.Query;
import com.abavilla.fpi.load.mapper.QueryMapper;
import com.abavilla.fpi.load.repo.QueryRepo;
import com.abavilla.fpi.load.service.load.RewardsSvc;
import com.abavilla.fpi.load.util.LoadConst;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.smallrye.common.constraint.Nullable;
import io.smallrye.mutiny.Uni;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

/**
 * Service layer for operating on load queries.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@ApplicationScoped
public class QuerySvc extends AbsRepoSvc<QueryDto, Query, QueryRepo> {

  /**
   * Dto to entity mapper for {@link Query}
   */
  @Inject
  QueryMapper queryMapper;

  /**
   * Service for reloading prepaid accounts
   */
  @Inject
  RewardsSvc rewardsSvc;

  /**
   * Utility library for detecting and formatting phone numbers
   */
  @Inject
  PhoneNumberUtil phoneNumberUtil;

  /**
   * Utility library for detecting the operator of phone numbers
   */
  @Inject
  PhoneNumberToCarrierMapper carrierMapper;

  /**
   * Process the load query to invoke load service.
   *
   * @param query {@link QueryDto} containing the query
   * @return {@link RespDto}
   */
  public Uni<RespDto<AbsDto>> processQuery(QueryDto query) {
    RespDto<AbsDto> resp = buildResponse();

    return repo.findByQuery(query.getQuery()).chain(found->{
      if (found.isPresent()) {
        return Uni.createFrom().failure(new FPISvcEx("Duplicate load request detected!",
            Response.Status.BAD_REQUEST.getStatusCode()));
      }
      return Uni.createFrom().voidItem();
    })
    .chain(v -> {
      Query log = mapToEntity(query);
      log.setExpiry(DateUtil.now().plusMinutes(30));
      log.setDateCreated(DateUtil.now());
      log.setDateUpdated(DateUtil.now());
      return repo.persist(log);
    })
    .chain(savedItem -> {
      var tokens = StringUtils.split(query.getQuery(), null, 3);
      if (tokens.length >= 2) {
        var sku = tokens[0];
        var msisdn = tokens[1];
        var network = tokens.length == 2 ? StringUtils.EMPTY : tokens[3];

        var loadReq = buildLoadRequest(msisdn, sku, network);
        return rewardsSvc.reloadNumber(loadReq).chain(svcResponse -> {
          resp.setResp((AbsDto) svcResponse.getEntity());
          return Uni.createFrom().item(resp);
        });
      } else {
        return Uni.createFrom().failure(new FPISvcEx("Invalid query",
            Response.Status.BAD_REQUEST.getStatusCode()));
      }
    });
  }

  /**
   * Creates the API Response.
   *
   * @return {@link QueryRespDto} Status for query request
   */
  private RespDto<AbsDto> buildResponse() {
    var resp = new RespDto<AbsDto>();
    var queryResp = new QueryRespDto();

    resp.setResp(queryResp);
    resp.setTimestamp(DateUtil.nowAsStr());
    return resp;
  }

  /**
   * Creates the load request based on given parameters, determines the network if missing
   *
   * @param mobile Mobile or account number
   * @param sku Promo to avail
   * @param network Telco or operator for mobile or account number
   * @return {@link LoadReqDto} Load request
   */
  @SneakyThrows
  private LoadReqDto buildLoadRequest(String mobile, String sku, @Nullable String network) {
    var loadReq = new LoadReqDto();
    var carrier = network;
    var number = phoneNumberUtil.parse(mobile, LoadConst.PH_REGION_CODE);

    loadReq.setSku(sku);

    if (phoneNumberUtil.isValidNumber(number)) {
      loadReq.setMobile(mobile);
      if (StringUtils.isBlank(network)) {
        carrier = carrierMapper.getNameForNumber(number, LoadConst.DEFAULT_LOCALE);
      }
    }
    loadReq.setAccountNo(mobile);
    loadReq.setTelco(carrier);

    return loadReq;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QueryDto mapToDto(Query entity) {
    return queryMapper.mapToDto(entity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Query mapToEntity(QueryDto dto) {
    return queryMapper.mapToEntity(dto);
  }
}