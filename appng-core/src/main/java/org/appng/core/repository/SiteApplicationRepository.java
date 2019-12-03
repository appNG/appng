/*
 * Copyright 2011-2019 the original author or authors.
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

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteApplicationPK;
import org.appng.core.domain.SiteImpl;
import org.appng.persistence.repository.SearchRepository;
import org.springframework.data.jpa.repository.Query;

public interface SiteApplicationRepository extends SearchRepository<SiteApplication, SiteApplicationPK> {

	SiteApplication findByDatabaseConnectionId(Integer connectionId);

	SiteApplication findByApplicationNameAndGrantedSitesName(String grantedApplication, String grantedSite);

	List<SiteApplication> findByGrantedSites(Site site);

	@Query("select sa.application from SiteApplication sa where sa.databaseConnection=?1")
	Application findApplicationForConnection(DatabaseConnection dbc);

	@Query("select sa.databaseConnection from SiteApplication sa where sa.site=?1 and sa.application=?2")
	DatabaseConnection getDatabaseForSiteAndApplication(SiteImpl site, ApplicationImpl application);

	SiteApplication findBySiteNameAndApplicationName(String site, String application);

}
