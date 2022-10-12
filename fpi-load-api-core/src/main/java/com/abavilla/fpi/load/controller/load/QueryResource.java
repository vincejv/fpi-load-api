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

package com.abavilla.fpi.load.controller.load;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.abavilla.fpi.fw.controller.AbsBaseResource;
import com.abavilla.fpi.fw.dto.AbsDto;
import com.abavilla.fpi.fw.dto.impl.RespDto;
import com.abavilla.fpi.fw.exceptions.FPISvcEx;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.entity.Query;
import com.abavilla.fpi.load.ext.dto.QueryDto;
import com.abavilla.fpi.load.service.QuerySvc;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Resource is used to submit query strings for load requests.
 * Endpoints for doing operations with {@link Query} items.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@Path("/fpi/load/query")
public class QueryResource extends AbsBaseResource<QueryDto, Query, QuerySvc> {

  /**
   * Endpoint to process the query and send to load api
   *
   * @param queryDto {@link QueryDto} containing the load query
   * @return {@link RespDto} response
   */
  @POST
  public Uni<RespDto<AbsDto>> loadQuery(QueryDto queryDto) {
    return service.processQuery(queryDto).map(resp -> {
      RespDto<AbsDto> queryResp = new RespDto<>();
      queryResp.setResp(resp);
      queryResp.setStatus(String.valueOf(resp.getStatus()));
      queryResp.setTimestamp(DateUtil.nowAsStr());
      return queryResp;
    });
  }

  /**
   * {@inheritDoc}
   */
  @ServerExceptionMapper
  @Override
  public RestResponse<RespDto<Object>> mapException(FPISvcEx x) {
    return super.mapException(x);
  }
}
