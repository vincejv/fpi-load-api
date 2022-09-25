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
public abstract class LoadUtils {

  /**
   * Compress and encodes a string to Bse64 given the provider and provider id.
   * @param prov Load Provider
   * @param provId Provider Id
   *
   * @return Encoded string
   */
  public static String encodeId(String prov, String provId) {
    if (NumberUtils.isDigits(provId)) {
      throw new IllegalArgumentException("Supports only integer provider ids");
    }
    if (StringUtils.isBlank(prov)) {
      throw new IllegalArgumentException("Must provide a provider");
    }

    return prov.charAt(0) + B32Util.encode(Long.parseLong(provId));
  }

  /**
   * Converts long value to byte array
   * @param l The long value
   *
   * @return Byte array
   */
  private static byte[] longToBytes(long l) {
    byte[] result = new byte[Long.BYTES];
    for (int i = Long.BYTES - 1; i >= 0; i--) {
      result[i] = (byte)(l & 0xFF);
      l >>= Byte.SIZE;
    }
    return result;
  }
}
