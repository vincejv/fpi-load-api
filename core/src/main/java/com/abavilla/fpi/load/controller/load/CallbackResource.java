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

import com.abavilla.fpi.fw.controller.AbsBaseResource;
import com.abavilla.fpi.fw.dto.impl.NullDto;
import com.abavilla.fpi.fw.dto.impl.RespDto;
import com.abavilla.fpi.fw.util.MapperUtil;
import com.abavilla.fpi.load.config.ApiKeyConfig;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsCallbackDto;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.service.load.RewardsCallbackSvc;
import com.dtone.dvs.dto.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

/**
 * Endpoints for receiving the status for the load transaction.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@Path("/fpi/load/callback")
public class CallbackResource
    extends AbsBaseResource<GLRewardsCallbackDto, RewardsTransStatus, RewardsCallbackSvc> {
  @Inject
  ApiKeyConfig apiKeyConfig;

  @Path("{apiKey}")
  @POST
  public Uni<RespDto<NullDto>> callback(@PathParam("apiKey") String apiKey,
                                        JsonNode body) {
    if (StringUtils.equals(apiKey, apiKeyConfig.getGenericApiKey())) {
      return service.storeCallback(MapperUtil.convert(body, GLRewardsCallbackDto.class));
    } else if (StringUtils.equals(apiKey, "intlprov")) {
      return service.storeCallback(MapperUtil.convert(body, Transaction.class));
    } else {
      throw new WebApplicationException(Response
          .status(HttpResponseStatus.UNAUTHORIZED.code())
          .build());
    }
  }
}
