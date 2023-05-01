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

import com.abavilla.fpi.fw.exceptions.FPISvcEx;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.entity.load.PromoSku;
import com.abavilla.fpi.load.entity.load.ProviderOffer;
import com.abavilla.fpi.load.ext.dto.LoadRespDto;
import com.abavilla.fpi.load.util.LoadConst;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

public abstract class AbsLoadProviderSvc implements ILoadProviderSvc {
  protected long priority;
  protected String providerName;

  @Inject
  protected PhoneNumberUtil phoneNumberUtil;

  @PostConstruct
  final void constructObject() {
    init();
  }

  @Override
  public long getPriority() {
    return priority;
  }

  @Override
  public String getProviderName() {
    return providerName;
  }

  protected final String getProductCode(PromoSku promo) {
    return promo.getOffers().stream().filter(sku ->
        StringUtils.equals(sku.getProviderName(), providerName))
        .map(ProviderOffer::getProductCode).findAny().orElseThrow();
  }

  public Uni<LoadRespDto> reload(LoadReqDto req, PromoSku promo) {

    if (StringUtils.isNotBlank(req.getMobile())) {
      if (isValidPhoneNo(req)) {
        parsePhoneNumber(req);
      } else {
        var ex = new FPISvcEx("Invalid phone number",
            Response.Status.BAD_REQUEST.getStatusCode(), req);
        return Uni.createFrom().failure(ex);
      }
    }

    return callSvc(req, promo);
  }

  protected abstract Uni<LoadRespDto> callSvc(LoadReqDto req, PromoSku promo);

  @SneakyThrows
  protected boolean isValidPhoneNo(LoadReqDto loadReqDto) {
    var phoneNo = phoneNumberUtil.parse(loadReqDto.getMobile(), LoadConst.PH_REGION_CODE);
    return phoneNumberUtil.isValidNumber(phoneNo);
  }

  protected void parsePhoneNumber(LoadReqDto loadReqDto) {
  }
}
