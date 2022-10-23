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

package com.abavilla.fpi.load.mapper.load.gl;

import java.time.DateTimeException;
import java.time.LocalDateTime;

import com.abavilla.fpi.fw.mapper.IMapper;
import com.abavilla.fpi.fw.util.DateUtil;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsCallbackDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsReqDto;
import com.abavilla.fpi.load.dto.load.gl.GLRewardsRespDto;
import com.abavilla.fpi.load.entity.gl.GLRewardsCallback;
import com.abavilla.fpi.load.entity.gl.GLRewardsReq;
import com.abavilla.fpi.load.entity.gl.GLRewardsResp;
import com.abavilla.fpi.load.util.LoadConst;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface GLMapper extends IMapper {

  String GL_TIMESTAMP_FORMAT = "EEE, MMM dd yyyy HH:mm:ss 'GMT'Z (z)";

  @Mapping(source = "body.", target = ".")
  GLRewardsReq mapGLRewardsReqToEntity(GLRewardsReqDto dto);

  @Mapping(source = "body.", target = ".")
  @Mapping(source = "error", target = "error")
  GLRewardsResp mapGLRewardsRespToEntity(GLRewardsRespDto dto);

  @Mapping(target = "dateCreated", ignore = true)
  @Mapping(target = "dateUpdated", ignore = true)
  @Mapping(target = "loadProvider", constant = LoadConst.PROV_GL)
  @Mapping(source = "body.", target = ".")
  GLRewardsCallback mapGLCallbackDtoToEntity(GLRewardsCallbackDto dto);

  default String glLdtToStr(LocalDateTime ldtTimestamp) {
    if (ldtTimestamp != null) {
      return DateUtil.convertLdtToStr(ldtTimestamp, DateUtil.UTC_ZULU_TIMEZONE);
    } else {
      return null;
    }
  }

  default LocalDateTime glStrToLdt(String strTimestamp) {
    try {
      return DateUtil.parseStrDateWTzToLdt(strTimestamp, GL_TIMESTAMP_FORMAT, DateUtil.UTC_ZULU_TIMEZONE);
    } catch (DateTimeException | NullPointerException ex) {
      return null;
    }
  }
}
