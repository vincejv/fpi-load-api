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
import com.abavilla.fpi.fw.dto.impl.RespDto;
import com.abavilla.fpi.fw.exceptions.FPISvcEx;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.dto.load.LoadRespDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsReqDto;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.service.load.RewardsSvc;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Endpoints for doing reload transactions.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@Path("/fpi/load/reload")
public class LoadResource
    extends AbsBaseResource<GLRewardsReqDto, RewardsTransStatus, RewardsSvc> {

  @POST
  public Uni<RespDto<LoadRespDto>> loadUp(LoadReqDto loadReq) {
    return service.reloadNumber(loadReq).map(loadRespDto -> {
      var resp = new RespDto<LoadRespDto>();
      resp.setResp(loadRespDto);
      resp.setTimestamp(DateUtil.nowAsStr());
      resp.setStatus(loadRespDto.getStatus().toString());
      return resp;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @ServerExceptionMapper
  protected RestResponse<RespDto<Object>> mapException(FPISvcEx x) {
    return super.mapException(x);
  }
}
