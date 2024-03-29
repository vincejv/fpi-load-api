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

package com.abavilla.fpi.load.service;

import com.abavilla.fpi.fw.service.AbsSvc;
import com.abavilla.fpi.fw.util.MapperUtil;
import com.abavilla.fpi.load.dto.ErrorLogDto;
import com.abavilla.fpi.load.entity.ErrorLog;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ErrorLogSvc extends AbsSvc<ErrorLogDto, ErrorLog> {
  public Uni<ErrorLogDto> post(ErrorLogDto dto) {
    return repo.persist(mapToEntity(dto)).map(this::mapToDto);
  }
  @Override
  public ErrorLogDto mapToDto(ErrorLog entity) {
    return MapperUtil.mapper().convertValue(entity, ErrorLogDto.class);
  }

  @Override
  public ErrorLog mapToEntity(ErrorLogDto dto) {
    return MapperUtil.mapper().convertValue(dto, ErrorLog.class);
  }
}
