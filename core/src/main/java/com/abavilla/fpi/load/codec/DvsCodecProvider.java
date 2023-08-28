/*************************************************************************
 * FPI Application - Abavilla                                            *
 * Copyright (C) 2023  Vince Jerald Villamora                            *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.*
 *************************************************************************/

package com.abavilla.fpi.load.codec;

import java.util.List;

import com.dtone.dvs.dto.Benefit;
import com.dtone.dvs.dto.BenefitFixed;
import com.dtone.dvs.dto.BenefitRanged;
import com.dtone.dvs.dto.Product;
import com.dtone.dvs.dto.ProductFixed;
import com.dtone.dvs.dto.ProductPrice;
import com.dtone.dvs.dto.ProductPriceFixed;
import com.dtone.dvs.dto.ProductPriceRanged;
import com.dtone.dvs.dto.ProductRanged;
import com.dtone.dvs.dto.ProductSource;
import com.dtone.dvs.dto.Source;
import com.dtone.dvs.dto.SourceFixed;
import com.dtone.dvs.dto.SourceRanged;
import com.dtone.dvs.dto.Transaction;
import com.dtone.dvs.dto.TransactionFixed;
import com.dtone.dvs.dto.TransactionRanged;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodecProvider;

/**
 * MongoDB Codec registry, contains the codec for saving DVS API Objects to mongodb.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
public class DvsCodecProvider implements CodecProvider {

  @SuppressWarnings("rawtypes")
  private static final List<Class> discriminatorClasses;

  static {
    discriminatorClasses = List.of(
      Transaction.class, TransactionRanged.class, TransactionFixed.class,
      Product.class, ProductRanged.class, ProductFixed.class,
      ProductPrice.class, ProductPriceRanged.class, ProductPriceFixed.class,
      ProductSource.class, Source.class, SourceRanged.class, SourceFixed.class,
      Benefit.class, BenefitFixed.class, BenefitRanged.class
    );
  }

  @Override
  public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
    if (discriminatorClasses.contains(clazz)) {
      return buildDiscriminatorCodec(clazz, registry);
    }
    return null; // Don't throw here, this tells
  }

  private static <T> Codec<T> buildDiscriminatorCodec(Class<T> clazz, CodecRegistry registry) {
    ClassModel<T> discriminatorModel = ClassModel.builder(clazz)
      .enableDiscriminator(true).build();
    return PojoCodecProvider.builder()
      .register(discriminatorModel)
      .build().get(clazz, registry);
  }

}
