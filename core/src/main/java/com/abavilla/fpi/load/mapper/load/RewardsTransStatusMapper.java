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

import java.util.List;

import com.abavilla.fpi.fw.entity.mongo.AbsMongoField;
import com.abavilla.fpi.fw.mapper.IMapper;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.load.LoadReqDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsReqDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsRespDto;
import com.abavilla.fpi.load.entity.load.CallBack;
import com.abavilla.fpi.load.entity.load.RewardsTransStatus;
import com.abavilla.fpi.load.ext.dto.LoadRespDto;
import com.abavilla.fpi.load.mapper.load.dtone.DTOneMapper;
import com.abavilla.fpi.load.mapper.load.gl.GLMapper;
import com.abavilla.fpi.telco.ext.enums.ApiStatus;
import com.abavilla.fpi.telco.ext.enums.BotSource;
import com.dtone.dvs.dto.Transaction;
import com.dtone.dvs.dto.TransactionRequest;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = LoadReqEntityMapper.class)
public abstract class RewardsTransStatusMapper implements IMapper {

  @Inject
  DTOneMapper dtOneMapper;

  @Inject
  GLMapper glMapper;

  @Mapping(target = "id", ignore = true) // do not copy id from dto
  @Mapping(target = "dateCreated", ignore = true)
  @Mapping(target = "dateUpdated", ignore = true)
  @Mapping(target = "apiCallback", source = "status")
  @Mapping(target = "transactionId", source = "extTransactionId")
  public abstract void mapLoadRespDtoToEntity(LoadRespDto loadRespDto,
                                              @MappingTarget RewardsTransStatus dest);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "loadRequest", source = ".", qualifiedByName = "mapToEntity")
  @Mapping(target = "source", source = "botSource")
  public abstract void mapLoadReqToEntity(LoadReqDto loadReqDto,
                                          @MappingTarget RewardsTransStatus dest);

  /**
   * Adds initial callback
   * @return initial callback list
   */
  List<CallBack> addInitialCallbackStatus(ApiStatus apiStatus) {
    CallBack callBack = new CallBack();
    callBack.setStatus(apiStatus);
    callBack.setDateReceived(DateUtil.now());
    return List.of(callBack);
  }

  AbsMongoField anyObjectToAbsField(Object dto) {
    AbsMongoField field = null;
    if (dto instanceof GLRewardsReqDto glReqDto) {
      field = glMapper.mapGLRewardsReqToEntity(glReqDto);
    } else if (dto instanceof GLRewardsRespDto glRespDto) {
      field = glMapper.mapGLRewardsRespToEntity(glRespDto);
    } else if (dto instanceof TransactionRequest transReq) {
      field = dtOneMapper.copyTransactionReqToDVSReq(transReq);
    } else if (dto instanceof Transaction transResp) {
      field = dtOneMapper.copyTransactionRespToDVSResp(transResp);
    }
    return field;
  }

  BotSource strToBotSource(String value) {
    return StringUtils.isNotBlank(value) ? BotSource.fromValue(value) : null;
  }

  String botSourceToStr(BotSource botSource) {
    return botSource == null ? null : botSource.toString();
  }

}
