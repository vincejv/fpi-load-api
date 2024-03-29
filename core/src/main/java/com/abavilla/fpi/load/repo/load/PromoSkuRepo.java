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

package com.abavilla.fpi.load.repo.load;

import java.util.Optional;

import com.abavilla.fpi.fw.repo.AbsMongoRepo;
import com.abavilla.fpi.load.entity.load.PromoSku;
import com.abavilla.fpi.telco.ext.enums.Telco;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.math.NumberUtils;

@ApplicationScoped
public class PromoSkuRepo extends AbsMongoRepo<PromoSku> {

  public Uni<Optional<PromoSku>> findByTelcoAndDenominationOrKeyword(Telco telco, String keyword) {
    return find("""
            {
              $and: [
                { 'telco.value': ?1 },
                {
                  $or: [
                    {
                       $and: [
                         {'keywords': ?2},
                         {
                           $or: [
                             {'type.value': 'Bundle' },
                             {'type.value': 'Credits' }
                           ]
                         }
                       ]
                    },
                    {
                       $and: [
                         { 'type.value': 'Ranged' },
                         { 'denomination.min': { $lte: ?3 } },
                         { 'denomination.max': { $gte: ?3 } }
                       ]
                    }
                  ]
                }
              ]
            }
            """,
        Sort.by("offers.wholesaleDiscount", Sort.Direction.Descending)
          .and("type.ord", Sort.Direction.Ascending),
        telco.getValue(),
        keyword,
        NumberUtils.toInt(keyword)
    ).firstResultOptional();
  }

  public Uni<Optional<PromoSku>> findByKeyword(String keyword) {
    return find("""
            {
              $and: [
                {
                  $or: [
                    {
                       $and: [
                         {'keywords': ?1},
                         {
                           $or: [
                             {'type.value': 'Bundle' },
                             {'type.value': 'Credits' }
                           ]
                         }
                       ]
                    },
                    {
                       $and: [
                         { 'type.value': 'Ranged' },
                         { 'denomination.min': { $lte: ?2 } },
                         { 'denomination.max': { $gte: ?2 } }
                       ]
                    }
                  ]
                }
              ]
            }
            """,
      Sort.by("offers.wholesaleDiscount", Sort.Direction.Descending)
        .and("type.ord", Sort.Direction.Ascending),
        keyword,
        NumberUtils.toInt(keyword)
    ).firstResultOptional();
  }

}
