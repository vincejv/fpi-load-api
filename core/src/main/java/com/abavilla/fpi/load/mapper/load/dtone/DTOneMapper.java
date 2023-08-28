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

package com.abavilla.fpi.load.mapper.load.dtone;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.abavilla.fpi.fw.mapper.IMapper;
import com.abavilla.fpi.load.dto.load.dtone.DVSCallbackDto;
import com.abavilla.fpi.load.entity.dtone.DVSCallback;
import com.abavilla.fpi.load.entity.dtone.DVSReq;
import com.abavilla.fpi.load.entity.dtone.DVSResp;
import com.abavilla.fpi.load.util.LoadConst;
import com.dtone.dvs.dto.Transaction;
import com.dtone.dvs.dto.TransactionFixed;
import com.dtone.dvs.dto.TransactionRanged;
import com.dtone.dvs.dto.TransactionRequest;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DTOneMapper extends IMapper {

  DVSReq copyTransactionReqToDVSReq(TransactionRequest dto);

  @Mapping(target = "dtOneId", source = "id")
  DVSResp copyTransactionRespToDVSResp(Transaction dto);

  @AfterMapping
  default void postCopyTransactionRespToDVSResp(Transaction dvsCallbackTransaction, @MappingTarget DVSResp dvsResp) {
    if (dvsCallbackTransaction instanceof TransactionFixed fixed && ObjectUtils.isNotEmpty(fixed.getBenefits())) {
      dvsResp.setBenefits(new ArrayList<>(fixed.getBenefits().size()));
      dvsResp.getBenefits().addAll(fixed.getBenefits());
    } else if (dvsCallbackTransaction instanceof TransactionRanged ranged && ObjectUtils.isNotEmpty(ranged.getBenefits())) {
      dvsResp.setBenefits(new ArrayList<>(ranged.getBenefits().size()));
      dvsResp.getBenefits().addAll(ranged.getBenefits());
    }
  }

  @Mapping(target = "loadProvider", constant = LoadConst.PROV_DTONE)
  DVSCallback mapDTOneRespToEntity(DVSCallbackDto dto);

  @Mapping(target = "dtOneId", source = "id")
  DVSCallbackDto mapDTOneTransactionToCallbackDto(Transaction dto);

  @AfterMapping
  default void postMapDTOneTransactionToCallbackDto(Transaction dvsCallbackTransaction, @MappingTarget DVSCallbackDto dvsCallbackDto) {
    if (dvsCallbackTransaction instanceof TransactionFixed fixed && ObjectUtils.isNotEmpty(fixed.getBenefits())) {
      dvsCallbackDto.setBenefits(new ArrayList<>(fixed.getBenefits().size()));
      dvsCallbackDto.getBenefits().addAll(fixed.getBenefits());
    } else if (dvsCallbackTransaction instanceof TransactionRanged ranged && ObjectUtils.isNotEmpty(ranged.getBenefits())) {
      dvsCallbackDto.setBenefits(new ArrayList<>(ranged.getBenefits().size()));
      dvsCallbackDto.getBenefits().addAll(ranged.getBenefits());
    }
  }

  default String dtLdtToStr(LocalDateTime ldtTimestamp) {
    if (ldtTimestamp != null) {
      var formatter = DateTimeFormatter.ISO_DATE_TIME;
      return ZonedDateTime.of(ldtTimestamp, ZoneOffset.UTC).format(formatter);
    } else {
      return null;
    }
  }

  default LocalDateTime dtStrToLdt(String strTimestamp) {
    var formatter = DateTimeFormatter.ISO_DATE_TIME;
    try {
      var zdt = ZonedDateTime.parse(strTimestamp, formatter);
      return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    } catch (DateTimeException | NullPointerException ex) {
      return null;
    }
  }
}