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
package org.appng.appngizer.model;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.appngizer.model.xml.SchemaVersion;
import org.appng.appngizer.model.xml.Versions;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.security.BCryptPasswordHandler;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.security.crypto.bcrypt.BCrypt;

public class Database extends org.appng.appngizer.model.xml.Database implements UriAware {

	private static DatatypeFactory dtf;

	static {
		try {
			dtf = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new IllegalStateException("error creating DatatypeFactory", e);
		}
	}

	public static Database fromDomain(DatabaseConnection dbc, MigrationInfoService status, String salt) {
		return fromDomain(dbc, status, salt, false);
	}

	public static Database fromDomain(DatabaseConnection dbc, MigrationInfoService status, String salt,
			boolean withManagedState) {
		Database db = new org.appng.appngizer.model.Database();
		StringBuilder dbInfo = new StringBuilder();
		boolean isOK = dbc.testConnection(dbInfo);
		if (isOK) {
			db.setVersions(new Versions());
			MigrationInfo[] all = status.all();
			for (int i = all.length; i > 0; i--) {
				MigrationInfo migrationInfo = all[i - 1];
				SchemaVersion schemaVersion = new SchemaVersion();
				schemaVersion.setDescription(migrationInfo.getDescription());
				schemaVersion.setChecksum(migrationInfo.getChecksum());
				schemaVersion.setVersion(migrationInfo.getVersion().getVersion());
				schemaVersion.setState(migrationInfo.getState().getDisplayName());
				Date installedOn = migrationInfo.getInstalledOn();
				if (null != installedOn) {
					GregorianCalendar cal = new GregorianCalendar();
					cal.setTime(installedOn);
					schemaVersion.setInstalled(dtf.newXMLGregorianCalendar(cal));
				}
				db.getVersions().getVersion().add(schemaVersion);
			}
			db.setDbVersion(dbInfo.toString());
		} else {
			db.setDbVersion("-unknown-");
		}
		db.setOk(isOK);
		db.setId(dbc.getId());
		db.setUser(dbc.getUserName());
		String realSalt = BCryptPasswordHandler.getPrefix() + "13$" + DigestUtils.sha256Hex(salt);
		db.setPassword(BCrypt.hashpw(dbc.getPasswordPlain(), realSalt));
		db.setDriver(dbc.getDriverClass());
		db.setUrl(dbc.getJdbcUrl());
		db.setType(dbc.getType().name());
		if (withManagedState) {
			db.setManaged(dbc.isManaged());
		}
		if (null != dbc.getDatabaseSize()) {
			db.setSize(dbc.getDatabaseSize());
		}
		return db;
	}

	public static void applyChanges(org.appng.appngizer.model.xml.Database database, DatabaseConnection dbc) {
		dbc.setUserName(database.getUser());
		dbc.setJdbcUrl(database.getUrl());
		dbc.setDriverClass(database.getDriver());
		if (StringUtils.isNotBlank(database.getPassword())) {
			dbc.setPasswordPlain(database.getPassword());
		}
	}
}
