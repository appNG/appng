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
package org.appng.cli.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.appng.core.domain.DatabaseConnection;
import org.appng.core.service.DatabaseService;

import com.beust.jcommander.Parameter;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;

/**
 * The main {@code appng} command.<br/>
 * 
 * <pre>
 * Usage: appng [options] [command] [command options]
 *   Options:
 *     -h, -help
 *        Prints the usage of the appNG Command Line Interface
 *        Default: false
 *     -initdatabase, -i
 *        Initializes the database. Use this in production. Must be executed also
 *        if connection parameters like user name or password of the database root
 *        connection have been changed.
 *        Default: false
 *     -managed, -m
 *        Make the root-connection managed by appNG
 *        Default: false
 *     -plainsql
 *        Do not format the SQL.
 *        Default: false
 *     -schemaexport
 *        Exports the schema to the database. Use only during development!
 *        Default: false
 *     -showsql
 *        Prints formatted SQL.
 *        Default: false
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 */
public class CommandMain {

	@Parameter
	private List<String> parameters = new ArrayList<String>();

	public List<String> getParameters() {
		return parameters;
	}

	@Parameter(names = { "-h", "-help" }, help = true, description = "Prints the usage of the appNG Command Line Interface")
	private boolean usage = false;

	@Parameter(names = "-schemaexport", description = "Exports the schema to the database. Use only during development!")
	private boolean schemaExport = false;

	@Parameter(names = "-showsql", description = "Prints formatted SQL.")
	private boolean showSql = false;

	@Parameter(names = "-plainsql", description = "Do not format the SQL.")
	private boolean plainsql = false;

	@Parameter(names = { "-initdatabase", "-i" }, description = "Initializes the database. Use this in production. Must be executed also if connection parameters like user name or password of the database root connection have been changed.")
	private boolean initDatabase = false;

	@Parameter(names = { "-managed", "-m" }, description = "Make the root-connection managed by appNG")
	private boolean connectionManaged = false;

	public boolean isUsage() {
		return usage;
	}

	public Map<String, String> getHibernateParams() {
		Map<String, String> hibernateParams = new HashMap<String, String>();
		if (schemaExport) {
			hibernateParams.put("hibernate.hbm2ddl.auto", "create");
		}
		hibernateParams.put("hibernate.show_sql", String.valueOf(showSql));
		hibernateParams.put("hibernate.format_sql", String.valueOf(!plainsql));
		return hibernateParams;
	}

	public boolean isSchemaExport() {
		return schemaExport;
	}

	public boolean isInitDatabase() {
		return initDatabase;
	}

	public boolean isConnectionManaged() {
		return connectionManaged;
	}

	/**
	 * Initializes the appNG root connection
	 * 
	 * @param databaseService
	 *            a {@link DatabaseService}
	 * @param config
	 *            the properties read from {@value org.appng.core.controller.PlatformStartup#CONFIG_LOCATION}
	 * @return the {@link MigrationInfo} for the current version of the database (see
	 *         {@link MigrationInfoService#current()})
	 */
	public MigrationInfo doInitDatabase(DatabaseService databaseService, Properties config) {
		DatabaseConnection platformConnection = databaseService.initDatabase(config, isConnectionManaged(), true);
		return databaseService.status(platformConnection);
	}

}
