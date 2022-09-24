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

package com.abavilla.fpi.load.repo.sms;

import javax.inject.Inject;
import javax.ws.rs.POST;

import com.abavilla.fpi.load.dto.auth.LoginDto;
import com.abavilla.fpi.load.dto.sms.MsgReqDto;
import com.abavilla.fpi.load.dto.sms.MsgReqStatusDto;
import com.abavilla.fpi.load.repo.auth.LoginRepo;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Resource access for authenticating with sending SMS through SMS service
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@RegisterRestClient(configKey = "sms-api")
@ClientHeaderParam(name = "Authorization", value = "{authenticate}")
public abstract class SmsRepo {

  /**
   * API Key for SMS service access
   */
  @ConfigProperty(name = "fpi.app-to-app.auth.username")
  String apiKey;

  /**
   * Secret Key for SMS service access
   */
  @ConfigProperty(name = "fpi.app-to-app.auth.password")
  String secretKey;

  /**
   * Login resource
   */
  @Inject
  LoginRepo loginRepo;

  /**
   * Send an SMS through SMS service
   * @param msgReqDto {@link MsgReqDto} object
   *
   * @return {@link MsgReqDto} future object containing the status
   */
  @POST
  public abstract Uni<MsgReqStatusDto> sendSms(MsgReqDto msgReqDto);

  /**
   * Get a token for authentication.
   *
   * @return Session token
   */
  public String authenticate() {
    var creds = new LoginDto();
    creds.setUsername(apiKey);
    creds.setPassword(secretKey);

    var session = loginRepo.authenticate(creds);
    return "Bearer " + session.await().indefinitely().getAccessToken();
  }
}
