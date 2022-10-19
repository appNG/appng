/*
 * Copyright 2011-2021 the original author or authors.
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
import java.util.Set;

import org.appng.core.domain.SiteImpl;
import org.appng.persistence.repository.SearchRepository;
import org.springframework.data.jpa.repository.Query;

public interface SiteRepository extends SearchRepository<SiteImpl, Integer> {

	SiteImpl findByName(String name);

	SiteImpl findByHost(String host);

	SiteImpl findByDomain(String domain);

	@Query("select s from SiteImpl s")
	List<SiteImpl> findSites();

	@Query("select s.id from SiteImpl s")
	List<Integer> getSiteIds();

	@Query("select s from SiteImpl s join s.siteApplications p where p.application.id= ?1")
	List<SiteImpl> findSitesForApplication(Integer applicationId);

	@Query("select s from SiteImpl s join s.siteApplications p where p.application.id= ?1 and s.active=?2")
	List<SiteImpl> findSitesForApplication(Integer applicationId, boolean active);

	@Query("select distinct s from SiteImpl s left join s.hostAliases a where s.host = ?1 or a = ?1")
	List<SiteImpl> findSitesForHostName(String hostName);

	@Query("select distinct s from SiteImpl s left join s.hostAliases a where s.host in (?1) or a in (?1)")
	List<SiteImpl> findSitesForHostNames(Set<String> hostNames);

	List<SiteImpl> findByNameIn(Iterable<String> siteNames);

}
