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

package com.abavilla.fpi.load.service.load.dtone;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.abavilla.fpi.fw.util.FWConst;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.dto.load.LoadRespDto;
import com.abavilla.fpi.load.entity.enums.ApiStatus;
import com.abavilla.fpi.load.entity.load.PromoSku;
import com.abavilla.fpi.load.mapper.load.LoadRespMapper;
import com.abavilla.fpi.load.service.load.AbsLoadProviderSvc;
import com.abavilla.fpi.load.util.LoadConst;
import com.dtone.dvs.DvsApiClientAsync;
import com.dtone.dvs.dto.Error;
import com.dtone.dvs.dto.PartyIdentifier;
import com.dtone.dvs.dto.Source;
import com.dtone.dvs.dto.TransactionRequest;
import com.dtone.dvs.dto.UnitTypes;
import io.smallrye.mutiny.Uni;
import lombok.SneakyThrows;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;

@ApplicationScoped
public class DTOneLoadSvc extends AbsLoadProviderSvc {
  @Inject
  DvsApiClientAsync dvsClient;

  @Inject
  LoadRespMapper loadRespMapper;

  @ConfigProperty(name = "com.dtone.callback-url")
  String callbackUrl;

  @Inject
  ManagedExecutor executor;

  @Override
  public void init() {
    priority = 1;
    providerName = LoadConst.PROV_DTONE;
  }

  @SneakyThrows
  @Override
  public Uni<LoadRespDto> reload(LoadReqDto req, PromoSku promo) {
    var dvsReq = buildRngRequest(req, promo);
    var dvsRespJob = Uni.createFrom()
        .completionStage(() -> dvsClient.createTransaction(dvsReq))
        .onFailure().recoverWithNull();

    var loadResp = new LoadRespDto();
    loadResp.setTransactionId(req.getTransactionId());
    loadResp.setApiRequest(dvsReq);
    loadResp.setStatus(ApiStatus.CREATED);

    return dvsRespJob.map(dvsResp -> {
      if (dvsResp != null) {
        if (dvsResp.isSuccess()) {
          loadResp.setStatus(ApiStatus.WAIT);
          loadRespMapper.mapDTRespToDto(dvsResp.getResult(), loadResp);
        } else {
          loadResp.setError(dvsResp.getErrors()
              .stream().map(Error::getMessage)
              .collect(Collectors.joining(FWConst.COMMA_SEP)));
        }
        loadResp.setApiResponse(dvsResp.getResult());
      }
      return loadResp;
    });
  }

  private TransactionRequest buildRngRequest(LoadReqDto loadRequest,
                                             PromoSku promo) {
    var destUnit = new Source(); // required for ranged products
    destUnit.setAmount(NumberUtils.toDouble(loadRequest.getSku()));
    destUnit.setUnitType(UnitTypes.CURRENCY.name());
    destUnit.setUnit(LoadConst.PH_CURRENCY); // Philippines peso

    var dtoOneReq = new TransactionRequest();
    //dtoOneReq.setCalculationMode(CalculationMode.DESTINATION_AMOUNT);
    //dtoOneReq.setDestination(destUnit);
    dtoOneReq.setAutoConfirm(true);

    var destMob = new PartyIdentifier();
    //destMob.setMobileNumber(loadRequest.getMobile());
    destMob.setAccountNumber(loadRequest.getMobile());
    dtoOneReq.setCreditPartyIdentifier(destMob);
    dtoOneReq.setProductId(NumberUtils.toLong(  // product id selection
        getProductCode(promo)));

    dtoOneReq.setCallbackUrl(callbackUrl);
    dtoOneReq.setExternalId(loadRequest.getTransactionId());
    return dtoOneReq;
  }
}
