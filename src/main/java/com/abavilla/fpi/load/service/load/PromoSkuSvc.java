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

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.abavilla.fpi.fw.service.AbsSvc;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.entity.enums.Telco;
import com.abavilla.fpi.load.entity.load.PromoSku;
import com.abavilla.fpi.load.repo.load.PromoSkuRepo;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.NotImplementedException;

@ApplicationScoped
public class PromoSkuSvc extends AbsSvc<LoadReqDto, PromoSku> {

  @Inject
  PromoSkuRepo advRepo;

  public Uni<Optional<PromoSku>> findSku(LoadReqDto loadReq) {
    return advRepo.findByTelcoAndKeyword(
        Telco.fromValue(loadReq.getTelco()), loadReq.getSku());
  }

  @Override
  public LoadReqDto mapToDto(PromoSku entity) {
    throw new NotImplementedException();
  }

  @Override
  public PromoSku mapToEntity(LoadReqDto dto) {
    throw new NotImplementedException();
  }
}
