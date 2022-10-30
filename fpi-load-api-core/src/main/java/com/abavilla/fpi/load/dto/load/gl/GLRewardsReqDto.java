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

package com.abavilla.fpi.load.dto.load.gl;

import com.abavilla.fpi.fw.dto.AbsDto;
import com.abavilla.fpi.fw.dto.AbsFieldDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RegisterForReflection
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = GLRewardsReqDto.class)
public class GLRewardsReqDto extends AbsDto {

  @JsonProperty("outboundRewardRequest")
  private Body body;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  @RegisterForReflection
  public static class Body extends AbsFieldDto {
    @JsonProperty("app_id")
    private String appId;
    @JsonProperty("app_secret")
    private String appSecret;
    @JsonProperty("rewards_token")
    private String rewardsToken;
    private String address;
    @JsonProperty("promo")
    private String sku;
  }
}
