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

package com.abavilla.fpi.load.mapper;

import com.abavilla.fpi.fw.mapper.IDtoToEntityMapper;
import com.abavilla.fpi.load.entity.Query;
import com.abavilla.fpi.load.ext.dto.QueryDto;
import com.abavilla.fpi.telco.ext.enums.BotSource;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Entity to DTO mapper for converting and mapping between {@link Query} and {@link QueryDto}
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface QueryMapper extends IDtoToEntityMapper<QueryDto, Query> {

  /**
   * {@inheritDoc}
   */
  @Override
  @Mapping(target = "query", source = "body")
  @Mapping(target = "botSource", source = "source")
  QueryDto mapToDto(Query entity);

  /**
   * {@inheritDoc}
   */
  @Override
  @Mapping(target = "body", source = "query")
  @Mapping(target = "source", source = "botSource")
  Query mapToEntity(QueryDto dto);

  /**
   * {@inheritDoc}
   */
  @Override
  @Mapping(target = "body", source = "query")
  @Mapping(target = "source", source = "botSource")
  @BeanMapping(
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
  )
  void patchEntity(@MappingTarget Query entity, QueryDto dto);

  default BotSource strToBotSource(String value) {
    return StringUtils.isNotBlank(value) ? BotSource.fromValue(value) : null;
  }

  default String botSourceToStr(BotSource botSource) {
    return botSource == null ? null : botSource.toString();
  }

}
