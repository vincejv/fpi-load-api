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

package com.abavilla.fpi.service.impl.load.dtone;

import com.abavilla.fpi.dto.impl.load.LoadReqDto;
import com.abavilla.fpi.dto.impl.load.LoadRespDto;
import com.abavilla.fpi.entity.impl.load.PromoSku;
import com.abavilla.fpi.service.impl.load.AbsLoadProviderSvc;
import com.dtone.dvs.DvsApiClient;
import com.dtone.dvs.dto.Error;
import com.dtone.dvs.dto.PartyIdentifier;
import com.dtone.dvs.dto.Source;
import com.dtone.dvs.dto.TransactionRequest;
import io.smallrye.mutiny.Uni;
import lombok.SneakyThrows;
import org.apache.commons.lang3.math.NumberUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.stream.Collectors;

@ApplicationScoped
public class DTOneLoadSvc extends AbsLoadProviderSvc {
  @Inject
  DvsApiClient dvsClient;

  @Override
  public void init() {
    priority = 1;
    providerName = "DTOne";
  }

  @SneakyThrows
  @Override
  public Uni<LoadRespDto> reload(LoadReqDto req, PromoSku promo) {
    var dvsReq = buildRngRequest(req, promo);
    var dvsRespJob = Uni.createFrom().item(dvsClient
        .createTransaction(dvsReq, false));

    var ret = new LoadRespDto();
    ret.setTransactionId(req.getTransactionId());
    ret.setApiRequest(dvsReq);

    return dvsRespJob.map(dvsResp -> {

      if (dvsResp.isSuccess()) {
        ret.setStatus(dvsResp.getResult().getStatus().getMessage());
      } else {
        ret.setError(dvsResp.getErrors()
            .stream().map(Error::getMessage)
            .collect(Collectors.joining(", ")));
      }
      ret.setApiResponse(dvsResp.getResult());
      return ret;
    });
  }

  private TransactionRequest buildRngRequest(LoadReqDto loadRequest,
                                             PromoSku promo) {
    var destUnit = new Source(); // required for ranged products
    destUnit.setAmount(NumberUtils.toDouble(loadRequest.getSku()));
    destUnit.setUnitType("CURRENCY");
    destUnit.setUnit("PHP"); // Philippines peso

    var dtoOneReq = new TransactionRequest();
    //dtoOneReq.setCalculationMode(CalculationMode.DESTINATION_AMOUNT);
    //dtoOneReq.setDestination(destUnit);
    dtoOneReq.setAutoConfirm(true);

    var destMob = new PartyIdentifier();
    destMob.setMobileNumber(loadRequest.getMobile());
    destMob.setAccountNumber(loadRequest.getMobile());
    dtoOneReq.setCreditPartyIdentifier(destMob);
    dtoOneReq.setProductId(NumberUtils.toLong(  // product id selection
        getProductCode(promo)));

    dtoOneReq.setCallbackUrl("https://api.abavilla.com/fpi/load/callback/intlprov");
    dtoOneReq.setExternalId(loadRequest.getTransactionId());
    return dtoOneReq;
  }
}