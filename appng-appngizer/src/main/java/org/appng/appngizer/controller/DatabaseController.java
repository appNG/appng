/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.appngizer.controller;

import java.io.File;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import org.appng.api.model.Application;
import org.appng.api.model.ResourceType;
import org.appng.appngizer.model.Database;
import org.appng.appngizer.model.Databases;
import org.appng.appngizer.model.Link;
import org.appng.appngizer.model.xml.Links;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.CacheProvider;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class DatabaseController extends ControllerBase {

	@RequestMapping(value = "/platform/database", method = RequestMethod.GET)
	public ResponseEntity<Database> info() throws Exception {
		DatabaseConnection platformConnection = databaseService.getPlatformConnection(configurer.getProps());
		return info(platformConnection);
	}

	@RequestMapping(value = "/platform/database", method = RequestMethod.PUT)
	public ResponseEntity<Database> updateRootConnection(@RequestBody org.appng.appngizer.model.xml.Database database)
			throws Exception {
		DatabaseConnection platformConnection = databaseService
				.getRootConnectionOfType(DatabaseType.valueOf(database.getType()));
		if (null == platformConnection) {
			return notFound();
		}
		platformConnection.setMigrationInfoService(databaseService.statusComplete(platformConnection));
		if (database.isManaged() != null && !database.isManaged().booleanValue() == platformConnection.isManaged()) {
			platformConnection.setManaged(true);
			databaseService.save(platformConnection);
		}
		return info(platformConnection);
	}

	@RequestMapping(value = "/platform/database/initialize", method = RequestMethod.POST)
	public ResponseEntity<Database> initialize(
			@RequestParam(name = "managed", required = false, defaultValue = "false") boolean isManaged)
			throws Exception {
		DatabaseConnection platformConnection = databaseService.initDatabase(configurer.getProps(), isManaged, true);
		return info(platformConnection);
	}

	protected ResponseEntity<Database> info(DatabaseConnection platformConnection)
			throws DatatypeConfigurationException {
		MigrationInfoService statusComplete = platformConnection.getMigrationInfoService();
		Database db = Database.fromDomain(platformConnection, statusComplete, getSharedSecret(), true);

		db.setSelf("/platform/database");
		db.setLinks(new Links());
		db.getLinks().getLink().add(new Link("initialize", db.getSelf() + "/initialize"));
		db.applyUriComponents(getUriBuilder());
		return ok(db);
	}

	@RequestMapping(value = "/site/{name}/database", method = RequestMethod.GET)
	public ResponseEntity<Databases> getDatabaseConnections(@PathVariable("name") String name) {
		SiteImpl site = getSiteByName(name);
		if (null == site) {
			return notFound();
		}
		Databases databases = new Databases(name);
		List<DatabaseConnection> databaseConnections = coreService.getDatabaseConnectionsForSite(site.getId());
		for (DatabaseConnection dbc : databaseConnections) {
			Database fromDomain = Database.fromDomain(dbc, databaseService.statusComplete(dbc), getSharedSecret());
			addApplicationLink(name, dbc, fromDomain);
			fromDomain.setSelf("/site/" + name + "/database/" + dbc.getId());
			fromDomain.applyUriComponents(getUriBuilder());
			databases.getDatabase().add(fromDomain);
		}

		return ok(databases);
	}

	protected void addApplicationLink(String site, DatabaseConnection dbc, Database fromDomain) {
		Application application = coreService.getApplicationForConnection(dbc);
		if (null != application) {
			fromDomain.addLink(new Link("application", "/site/" + site + "/application/" + application.getName()));
		}
	}

	@RequestMapping(value = "/site/{site}/application/{app}/database", method = RequestMethod.GET)
	public ResponseEntity<Database> getDatabaseConnectionForApplication(@PathVariable("site") String site,
			@PathVariable("app") String app) {
		SiteApplication siteApplication = coreService.getSiteApplication(site, app);
		if (null == siteApplication) {
			return notFound();
		}
		DatabaseConnection dbc = siteApplication.getDatabaseConnection();
		if (null == dbc) {
			return notFound();
		}

		CacheProvider cacheProvider = new CacheProvider(getCoreService().getPlatformProperties());
		File platformCache = cacheProvider.getPlatformCache(siteApplication.getSite(),
				siteApplication.getApplication());
		File sqlFolder = new File(platformCache, ResourceType.SQL.getFolder());

		MigrationInfoService dbStatus = databaseService.statusComplete(dbc, sqlFolder);
		Database fromDomain = Database.fromDomain(dbc, dbStatus, getSharedSecret());
		addApplicationLink(site, dbc, fromDomain);
		fromDomain.setSelf("/site/" + site + "/application/" + app + "/database/");
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	@RequestMapping(value = "/site/{site}/application/{app}/database", method = RequestMethod.PUT)
	public ResponseEntity<Database> updateDatabaseConnectionforApplication(@PathVariable("site") String site,
			@PathVariable("app") String app, @RequestBody org.appng.appngizer.model.xml.Database database) {
		SiteApplication siteApplication = coreService.getSiteApplication(site, app);
		if (null == siteApplication) {
			return notFound();
		}
		DatabaseConnection dbc = siteApplication.getDatabaseConnection();
		if (null == dbc) {
			return notFound();
		}
		Database.applyChanges(database, dbc);
		databaseService.save(dbc);
		ResponseEntity<Database> updated = getDatabaseConnectionForApplication(site, app);
		return ok(updated.getBody());
	}

	@RequestMapping(value = "/site/{name}/database/{id}", method = RequestMethod.GET)
	public ResponseEntity<Database> getDatabaseConnection(@PathVariable("name") String name,
			@PathVariable("id") Integer id) {
		SiteImpl site = getSiteByName(name);
		if (null == site) {
			return notFound();
		}
		DatabaseConnection dbc = coreService.getDatabaseConnection(id, false);
		if (null == dbc) {
			return notFound();
		}
		Database fromDomain = Database.fromDomain(dbc, databaseService.statusComplete(dbc), getSharedSecret());
		addApplicationLink(name, dbc, fromDomain);
		fromDomain.setSelf("/site/" + name + "/database/" + dbc.getId());
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	@RequestMapping(value = "/site/{name}/database/{id}", method = RequestMethod.PUT)
	public ResponseEntity<Database> updateDatabaseConnection(@PathVariable("name") String name,
			@PathVariable("id") Integer id, @RequestBody org.appng.appngizer.model.xml.Database database) {
		SiteImpl site = getSiteByName(name);
		if (null == site) {
			return notFound();
		}
		DatabaseConnection dbc = coreService.getDatabaseConnection(id, false);
		if (null == dbc) {
			return notFound();
		}
		Database.applyChanges(database, dbc);
		databaseService.save(dbc);
		ResponseEntity<Database> updated = getDatabaseConnection(name, id);
		return ok(updated.getBody());
	}

	Logger logger() {
		return log;
	}

}
