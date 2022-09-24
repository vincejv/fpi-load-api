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

package com.abavilla.fpi.load.entity.load;

import java.util.List;

import com.abavilla.fpi.fw.entity.mongo.AbsMongoItem;
import com.abavilla.fpi.load.entity.enums.SkuType;
import com.abavilla.fpi.load.entity.enums.Telco;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@EqualsAndHashCode(callSuper = true)
@RegisterForReflection
@NoArgsConstructor
@BsonDiscriminator
@MongoEntity(collection = "promo_sku")
public class PromoSku extends AbsMongoItem {
  private SkuType type;
  private String name;
  private Pricing denomination;
  private Pricing srp;
  private Telco telco;
  private List<ProviderOffer> offers;
  private List<String> keywords;
}
