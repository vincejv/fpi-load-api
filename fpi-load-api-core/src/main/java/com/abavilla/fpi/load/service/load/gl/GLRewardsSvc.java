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

package com.abavilla.fpi.load.service.load.gl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.abavilla.fpi.fw.exceptions.ApiSvcEx;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsReqDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsRespDto;
import com.abavilla.fpi.load.entity.load.PromoSku;
import com.abavilla.fpi.load.ext.dto.LoadRespDto;
import com.abavilla.fpi.load.mapper.load.LoadRespMapper;
import com.abavilla.fpi.load.repo.load.gl.GLLoadApiRepo;
import com.abavilla.fpi.load.service.load.AbsLoadProviderSvc;
import com.abavilla.fpi.load.util.LoadConst;
import com.abavilla.fpi.telco.ext.enums.ApiStatus;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.smallrye.mutiny.Uni;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class GLRewardsSvc extends AbsLoadProviderSvc {

  @ConfigProperty(name = "ph.com.gl.app-id")
  String appId;

  @ConfigProperty(name = "ph.com.gl.app-secret")
  String appSecret;

  @ConfigProperty(name = "ph.com.gl.rewards.token")
  String amaxToken;

  @RestClient
  GLLoadApiRepo loadApi;

  @Inject
  LoadRespMapper rewardsMapper;

  @Override
  public void init() {
    priority = 0;
    providerName = LoadConst.PROV_GL;
  }

  @Override
  public Uni<LoadRespDto> callSvc(LoadReqDto req, PromoSku promo) {
    var apiReqBody = new GLRewardsReqDto.Body();
    apiReqBody.setAppId(appId);
    apiReqBody.setAppSecret(appSecret);
    apiReqBody.setRewardsToken(amaxToken);

    apiReqBody.setAddress(req.getMobile());
    apiReqBody.setSku(getProductCode(promo));

    var apiReq = new GLRewardsReqDto();
    apiReq.setBody(apiReqBody);
    var loadResp = new LoadRespDto();
    loadResp.setTransactionId(req.getTransactionId());
    loadResp.setApiRequest(apiReq);
    loadResp.setStatus(ApiStatus.CREATED);

    return loadApi.sendLoad(apiReq)
        .onFailure(ApiSvcEx.class).recoverWithItem(ex -> {
          var apiEx = (ApiSvcEx) ex; // error encountered
          var resp = apiEx.getJsonResponse(GLRewardsRespDto.class);
          resp.chainEx(apiEx);
          return resp;
        })
        .map(resp -> {
          if (resp.getLastEx() == null) {
            loadResp.setStatus(ApiStatus.WAIT);
          } else {
            loadResp.setStatus(ApiStatus.REJ);
          }
          rewardsMapper.mapGLRespToDto(resp, loadResp);
          loadResp.setApiResponse(resp);
          return loadResp;
        });
  }

  @SneakyThrows
  @Override
  protected void parsePhoneNumber(LoadReqDto loadReqDto) {
    var number = phoneNumberUtil.parse(loadReqDto.getMobile(), LoadConst.PH_REGION_CODE);
    String sanitizedNumber = phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    sanitizedNumber = StringUtils.deleteWhitespace(sanitizedNumber); // remove all white spaces
    sanitizedNumber = StringUtils.substring(sanitizedNumber, 1); // remove first digit 0
    loadReqDto.setMobile(sanitizedNumber);
  }
}
