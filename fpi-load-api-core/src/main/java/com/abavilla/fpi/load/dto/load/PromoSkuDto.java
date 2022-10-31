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

package com.abavilla.fpi.load.dto.load;

import java.util.List;

import com.abavilla.fpi.fw.dto.AbsDto;
import com.abavilla.fpi.load.entity.load.ProviderOffer;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data transfer object containing the information for promotional packs.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RegisterForReflection
@NoArgsConstructor
public class PromoSkuDto extends AbsDto {

  /**
   * Promo type, whether bundle, credits or others
   */
  private String type;

  /**
   * Name of the promo
   */
  private String name;

  /**
   * The face value of the pack
   */
  private PricingDto denomination;

  /**
   * Suggested retail price
   */
  private PricingDto srp;

  /**
   * Operator which the promo is available
   */
  private String telco;

  /**
   * Upstream providers offering the promo
   */
  private List<ProviderOffer> offers;

  /**
   * Keywords linked to the offering
   */
  private List<String> keywords;
}
