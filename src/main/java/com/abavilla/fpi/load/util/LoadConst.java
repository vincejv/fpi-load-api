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

public abstract class LoadConst {

  public static final String PH_CURRENCY = "PHP";

  public static final String PH_REGION_CODE = "PH";

  public static final String PROV_GL = "GlobeLabs";

  public static final String PROV_DTONE = "DTOne";

  public static final String NO_LOAD_PROVIDER_AVAILABLE = "No Load provider available";

  /**
   * Successful status code for globelabs provider
   */
  public static final String GL_SUCCESS_STS = "SUCCESS";

  /**
   * Successful status code for dtone provider
   */
  public static final long DT_SUCCESS_STS = 7000L;

  /**
   * Failed status code for globelabs provider
   */
  public static final String GL_FAILED_STS = "FAILED";

  /**
   * Failed status code when reloading a postpaid number with prepaid credits
   */
  public static final int DT_INVPREPAID_STS = 90000;

  /**
   * Failed status code when number is not in operator
   */
  public static final int DT_OPMISMATCH_STS = 90200;
}
