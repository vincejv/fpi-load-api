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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Utility methods for the Load API Service.
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
public abstract class LoadUtil {

  /**
   * Compress and encodes a string to Bse64 given the provider and provider id.
   * @param prov Load Provider
   * @param provId Provider Id
   *
   * @return Encoded string
   */
  public static String encodeId(String prov, String provId) {
    if (NumberUtils.isDigits(provId)) {
      throw new IllegalArgumentException("Supports only integer provider ids, given: " + provId);
    }
    if (StringUtils.isBlank(prov)) {
      throw new IllegalArgumentException("Must provide a provider, given empty string");
    }

    return prov.charAt(0) + B32Util.encode(Long.parseLong(provId));
  }
}
