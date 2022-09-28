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

package com.abavilla.fpi.load.mapper.load;

import com.abavilla.fpi.fw.mapper.IMongoItemMapper;
import com.abavilla.fpi.load.dto.load.PromoSkuDto;
import com.abavilla.fpi.load.entity.enums.SkuType;
import com.abavilla.fpi.load.entity.enums.Telco;
import com.abavilla.fpi.load.entity.load.PromoSku;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapping definition between conversion of {@link PromoSku} and {@link PromoSkuDto}
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PromoSkuMapper extends IMongoItemMapper<PromoSkuDto, PromoSku> {

  /**
   * Mapping for {@link Telco} to {@link String} and vice versa
   *
   * @param telcoStr The telco string containing the value
   * @return Equivalent {@link Telco} enum value
   */
  default Telco strToTelco(String telcoStr) {
    return Telco.fromValue(telcoStr);
  }

  /**
   * Mapping for {@link Telco} to {@link String} and vice versa
   *
   * @param skuStr The sku string containing the value
   * @return Equivalent {@link Telco} enum value
   */
  default SkuType strToSku(String skuStr) {
    return SkuType.fromValue(skuStr);
  }

}
