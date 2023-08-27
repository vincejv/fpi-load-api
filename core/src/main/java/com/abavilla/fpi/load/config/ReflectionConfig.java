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

package com.abavilla.fpi.load.config;

import com.abavilla.fpi.fw.config.BaseReflectionConfig;
import com.dtone.dvs.dto.AmountRanged;
import com.dtone.dvs.dto.ApiError;
import com.dtone.dvs.dto.ApiRequest;
import com.dtone.dvs.dto.ApiResponse;
import com.dtone.dvs.dto.Balance;
import com.dtone.dvs.dto.BalanceFilter;
import com.dtone.dvs.dto.Benefit;
import com.dtone.dvs.dto.BenefitAmountFixed;
import com.dtone.dvs.dto.BenefitAmountRanged;
import com.dtone.dvs.dto.BenefitFixed;
import com.dtone.dvs.dto.BenefitRanged;
import com.dtone.dvs.dto.BenefitType;
import com.dtone.dvs.dto.BenefitTypes;
import com.dtone.dvs.dto.CalculationMode;
import com.dtone.dvs.dto.Country;
import com.dtone.dvs.dto.ErrorResponse;
import com.dtone.dvs.dto.LookupOperatorRequest;
import com.dtone.dvs.dto.Operator;
import com.dtone.dvs.dto.PageAsync;
import com.dtone.dvs.dto.PageInfo;
import com.dtone.dvs.dto.Party;
import com.dtone.dvs.dto.PartyIdentifier;
import com.dtone.dvs.dto.Pin;
import com.dtone.dvs.dto.PinInfo;
import com.dtone.dvs.dto.Price;
import com.dtone.dvs.dto.Prices;
import com.dtone.dvs.dto.Product;
import com.dtone.dvs.dto.ProductFilter;
import com.dtone.dvs.dto.ProductFixed;
import com.dtone.dvs.dto.ProductPrice;
import com.dtone.dvs.dto.ProductPriceFixed;
import com.dtone.dvs.dto.ProductPriceRanged;
import com.dtone.dvs.dto.ProductPricesFixed;
import com.dtone.dvs.dto.ProductPricesRanged;
import com.dtone.dvs.dto.ProductRanged;
import com.dtone.dvs.dto.ProductSource;
import com.dtone.dvs.dto.ProductType;
import com.dtone.dvs.dto.Promotion;
import com.dtone.dvs.dto.PromotionFilter;
import com.dtone.dvs.dto.Rates;
import com.dtone.dvs.dto.Region;
import com.dtone.dvs.dto.Service;
import com.dtone.dvs.dto.Source;
import com.dtone.dvs.dto.SourceFixed;
import com.dtone.dvs.dto.SourceRanged;
import com.dtone.dvs.dto.StatementIdentifier;
import com.dtone.dvs.dto.Status;
import com.dtone.dvs.dto.StatusClass;
import com.dtone.dvs.dto.Transaction;
import com.dtone.dvs.dto.TransactionFilter;
import com.dtone.dvs.dto.TransactionFixed;
import com.dtone.dvs.dto.TransactionRanged;
import com.dtone.dvs.dto.TransactionRequest;
import com.dtone.dvs.dto.UnitTypes;
import com.dtone.dvs.dto.Validity;
import com.dtone.dvs.dto.Values;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Classes to register for reflection for Quarkus native image.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@RegisterForReflection(targets = {
  AmountRanged.class,
  ApiError.class,
  ApiRequest.class,
  ApiResponse.class,
  Balance.class,
  BalanceFilter.class,
  Benefit.class,
  BenefitAmountFixed.class,
  BenefitAmountRanged.class,
  BenefitFixed.class,
  BenefitRanged.class,
  BenefitType.class,
  BenefitTypes.class,
  CalculationMode.class,
  Country.class,
  ErrorResponse.class,
  LookupOperatorRequest.class,
  Operator.class,
  PageAsync.class,
  PageInfo.class,
  Party.class,
  PartyIdentifier.class,
  Pin.class,
  PinInfo.class,
  Price.class,
  Prices.class,
  Product.class,
  ProductFilter.class,
  ProductFixed.class,
  ProductPrice.class,
  ProductPriceFixed.class,
  ProductPriceRanged.class,
  ProductPricesFixed.class,
  ProductPricesRanged.class,
  ProductRanged.class,
  ProductSource.class,
  ProductType.class,
  Promotion.class,
  PromotionFilter.class,
  Rates.class,
  Region.class,
  Service.class,
  Source.class,
  SourceFixed.class,
  SourceRanged.class,
  StatementIdentifier.class,
  Status.class,
  StatusClass.class,
  Transaction.class,
  TransactionFilter.class,
  TransactionFixed.class,
  TransactionRanged.class,
  TransactionRequest.class,
  UnitTypes.class,
  Validity.class,
  Values.class
})
public class ReflectionConfig extends BaseReflectionConfig {
}
