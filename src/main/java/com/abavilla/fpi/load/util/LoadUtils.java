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

package com.abavilla.fpi.load.util;

import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods for the Load API Service.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
public abstract class LoadUtils {

  /**
   * Encodes a string to Bse64 given the provider and provider id.
   * @param prov Load Provider
   * @param provId Provider Id
   *
   * @return Encoded string
   */
  public static String encodeId(String prov, String provId) {
    String rawString = StringUtils.substring(prov, 0, 1) + provId;
    return Base64.getEncoder()
        .withoutPadding()
        .encodeToString(rawString.getBytes());
  }
}
