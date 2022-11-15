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

import com.abavilla.fpi.fw.service.AbsRepoSvc;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.dto.load.PromoSkuDto;
import com.abavilla.fpi.load.entity.load.PromoSku;
import com.abavilla.fpi.load.mapper.load.PromoSkuMapper;
import com.abavilla.fpi.load.repo.load.PromoSkuRepo;
import com.abavilla.fpi.telco.ext.enums.Telco;
import io.smallrye.mutiny.Uni;

/**
 * Service layer for operating on {@link PromoSku} items.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@ApplicationScoped
public class PromoSkuSvc extends AbsRepoSvc<PromoSkuDto, PromoSku, PromoSkuRepo> {

  @Inject
  PromoSkuMapper mapper;

  public Uni<Optional<PromoSku>> findSku(LoadReqDto loadReq) {
    return repo.findByTelcoAndDenominationOrKeyword(
        Telco.fromValue(loadReq.getTelco()), loadReq.getSku());
  }

  /**
   * Finds the {@link PromoSku} given the load request, it returns the first result found by keyword.
   *
   * @param loadReq {@link LoadReqDto} object containing the SKU information
   * @return {@link PromoSku} promo found
   */
  public Uni<Optional<PromoSku>> findSkuByDefaultOperator(LoadReqDto loadReq) {
    return repo.findByKeyword(loadReq.getSku());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PromoSkuDto mapToDto(PromoSku entity) {
    return mapper.mapToDto(entity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PromoSku mapToEntity(PromoSkuDto dto) {
    return mapper.mapToEntity(dto);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void patchEntityFromDto(PromoSku entity, PromoSkuDto dto) {
    mapper.patchEntity(entity, dto);
  }
}
