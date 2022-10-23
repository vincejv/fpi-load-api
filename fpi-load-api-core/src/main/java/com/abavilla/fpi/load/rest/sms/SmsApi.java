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

package com.abavilla.fpi.load.rest.sms;

import javax.ws.rs.POST;

import com.abavilla.fpi.fw.rest.IApi;
import com.abavilla.fpi.load.dto.sms.MsgReqDto;
import com.abavilla.fpi.load.dto.sms.MsgReqStatusDto;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Resource access for authenticating with sending SMS through SMS service
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@RegisterRestClient(configKey = "sms-api")
public interface SmsApi extends IApi {

  /**
   * Send an SMS through SMS service
   * @param msgReqDto {@link MsgReqDto} object
   *
   * @return {@link MsgReqDto} future object containing the status
   */
  @POST
  Uni<MsgReqStatusDto> sendSms(MsgReqDto msgReqDto);
}
