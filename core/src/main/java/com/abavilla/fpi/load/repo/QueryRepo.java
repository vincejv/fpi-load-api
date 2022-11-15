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

package com.abavilla.fpi.load.repo;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.abavilla.fpi.fw.repo.AbsMongoRepo;
import com.abavilla.fpi.load.entity.Query;
import io.smallrye.mutiny.Uni;

/**
 * Repository later for doing CRUD Database operations for {@link Query}
 *
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 */
@ApplicationScoped
public class QueryRepo extends AbsMongoRepo<Query> {

  /**
   * Finds {@link Query} by query string.
   *
   * @return {@link Query} object containing the load query
   */
  public Uni<Optional<Query>> findByQuery(String query, String fpiUser) {
    return find("{ 'query' : ?1, 'fpiUser' : ?2 }", query, fpiUser).firstResultOptional();
  }
}
