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
package org.appng.appngizer.controller;

import java.io.File;
import java.util.List;
import java.util.Properties;

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
import org.appng.core.service.MigrationService;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class DatabaseController extends ControllerBase {

	@GetMapping(value = "/platform/database")
	public ResponseEntity<Database> info() throws Exception {
		Properties props = configurer.getProps();
		DatabaseType type = DatabaseType.valueOf(props.getProperty(MigrationService.DATABASE_TYPE).toUpperCase());
		DatabaseConnection platformConnection = databaseService.getRootConnectionOfType(type);
		databaseService.statusComplete(platformConnection);
		return info(platformConnection);
	}

	@PutMapping(value = "/platform/database")
	public ResponseEntity<Database> updateRootConnection(@RequestBody org.appng.appngizer.model.xml.Database database)
			throws Exception {
		DatabaseConnection platformConnection = databaseService
				.getRootConnectionOfType(DatabaseType.valueOf(database.getType()));
		if (null == platformConnection) {
			return notFound();
		}
		platformConnection.setMigrationInfoService(databaseService.statusComplete(platformConnection));
		if (database.isManaged() != null && !database.isManaged().booleanValue() == platformConnection.isManaged()) {
			platformConnection.setManaged(database.isManaged().booleanValue());
			databaseService.save(platformConnection);
		}
		return info(platformConnection);
	}

	@PostMapping(value = "/platform/database/initialize")
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

	@GetMapping(value = "/site/{name}/database")
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

	@GetMapping(value = "/site/{site}/application/{app}/database")
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

	@PutMapping(value = "/site/{site}/application/{app}/database")
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

	@GetMapping(value = "/site/{name}/database/{id}")
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

	@PutMapping(value = "/site/{name}/database/{id}")
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
		return LOGGER;
	}

}
