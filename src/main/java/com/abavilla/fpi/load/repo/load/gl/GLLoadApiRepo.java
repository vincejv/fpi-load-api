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

package com.abavilla.fpi.load.repo.load.gl;

import javax.ws.rs.POST;

import com.abavilla.fpi.load.dto.load.gl.GLRewardsReqDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsRespDto;
import com.abavilla.fpi.load.exceptions.handler.ApiRepoExHandler;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "gl-rewards-api")
@RegisterProvider(value = ApiRepoExHandler.class)
public interface GLLoadApiRepo {
  @POST
  Uni<GLRewardsRespDto> sendLoad(GLRewardsReqDto req);
}