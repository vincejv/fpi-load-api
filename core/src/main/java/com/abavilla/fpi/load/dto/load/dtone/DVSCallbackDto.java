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

package com.abavilla.fpi.load.dto.load.dtone;

import java.util.List;

import com.abavilla.fpi.fw.dto.AbsDto;
import com.dtone.dvs.dto.Benefit;
import com.dtone.dvs.dto.Party;
import com.dtone.dvs.dto.PartyIdentifier;
import com.dtone.dvs.dto.Pin;
import com.dtone.dvs.dto.Prices;
import com.dtone.dvs.dto.Product;
import com.dtone.dvs.dto.Promotion;
import com.dtone.dvs.dto.Rates;
import com.dtone.dvs.dto.StatementIdentifier;
import com.dtone.dvs.dto.Status;
import com.dtone.dvs.dto.Values;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class DVSCallbackDto extends AbsDto {

  private Long dtOneId;

  private String externalId;

  private String creationDate;

  private String confirmationExpirationDate;

  private String confirmationDate;

  private Status status;

  private String operatorReference;

  private Pin pin;

  private Product product;

  private Prices prices;

  private Rates rates;

  private List<Promotion> promotions;

  private Values requestedValues;

  private Values adjustedValues;

  private Party sender;

  private Party beneficiary;

  private PartyIdentifier debitPartyIdentifier;

  private PartyIdentifier creditPartyIdentifier;

  private StatementIdentifier statementIdentifier;

  private String callbackUrl;

  private List<Benefit> benefits;

}
