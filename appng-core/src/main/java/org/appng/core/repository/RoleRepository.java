/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appng.core.repository;

import java.util.List;

import org.appng.core.domain.RoleImpl;
import org.appng.persistence.repository.SearchRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends SearchRepository<RoleImpl, Integer> {

	List<RoleImpl> findByApplicationId(Integer applicationId);

	RoleImpl findByApplicationIdAndName(Integer applicationId, String name);

	RoleImpl findByApplicationNameAndName(String applicationName, String name);

	@Query("select r from SiteApplication s inner join s.application p inner join p.roles r where s.id.siteId = ?1 order by p.name, r.name")
	List<RoleImpl> findRolesForSite(Integer siteId);

	@Query("select r from SiteApplication s inner join s.application p inner join p.roles r where s.id.siteId = ?1 and s.id.applicationId = ?2 order by p.name, r.name")
	List<RoleImpl> findRolesForApplicationAndSite(Integer siteId, Integer applicationId);
}
