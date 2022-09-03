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

package com.abavilla.fpi.controller.impl.sms;

import com.abavilla.fpi.config.ApiKeyConfig;
import com.abavilla.fpi.controller.AbsResource;
import com.abavilla.fpi.dto.impl.NullDto;
import com.abavilla.fpi.entity.impl.sms.LeakAck;
import com.abavilla.fpi.service.impl.sms.MsgAckSvc;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Path("/fpi/sms/dlr")
public class FPICallbackResource extends AbsResource<NullDto, LeakAck, MsgAckSvc> {
  @Inject
  ApiKeyConfig apiKeyConfig;
  @Path("webhook/{apiKey}")
  @GET
  public Uni<Void> acknowledge(@QueryParam("status_code") String stsCde,
                               @QueryParam("transid") String msgId,
                               @QueryParam("timestamp") String timestamp,
                               @PathParam("apiKey") String apiKey) {
    if (StringUtils.equals(apiKey, apiKeyConfig.getDlrApiKey())) {
      return service.acknowledge(msgId, stsCde, timestamp);
    } else {
      throw new WebApplicationException(Response
          .status(HttpResponseStatus.UNAUTHORIZED.code())
          .build());
    }
  }
}