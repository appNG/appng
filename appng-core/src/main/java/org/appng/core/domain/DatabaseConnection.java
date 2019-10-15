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
package org.appng.core.domain;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.sql.DataSource;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ValidationMessages;
import org.appng.api.model.Site;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.ClassUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Represents a connection to a database which is being used either by the platform itself ("root-connection") or by a
 * {@link SiteApplication}.
 * 
 * @author Matthias Müller
 * 
 * @see SiteApplication#getDatabaseConnection()
 * 
 */
@Slf4j
@Entity
@Table(name = "database_connection")
@EntityListeners(PlatformEventListener.class)
public class DatabaseConnection implements Auditable<Integer> {

	private static final String DATABASE_NAME = "databaseName=";
	public static final String DB_PLACEHOLDER = "<name>";
	private static String MYSQL_DATASOURCE = "com.mysql.cj.jdbc.MysqlDataSource";
	private static final String MYSQL_LEGACY_DATASOURCE = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource";
	private static String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
	private static final String MYSQL_LEGACY_DRIVER = "com.mysql.jdbc.Driver";

	static {
		if (ClassUtils.isPresent(MYSQL_LEGACY_DATASOURCE, null)) {
			MYSQL_DATASOURCE = MYSQL_LEGACY_DATASOURCE;
			MYSQL_DRIVER = MYSQL_LEGACY_DRIVER;
		}
	}

	/** enum-type for the supported databases */
	public enum DatabaseType {

		/** MySQL */
		MYSQL(MYSQL_DRIVER, MYSQL_DATASOURCE, "jdbc:mysql://localhost:3306/" + DB_PLACEHOLDER, "select 1"),

		/** Microsoft SQL Server */
		MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver", "com.microsoft.sqlserver.jdbc.SQLServerDataSource",
				"jdbc:sqlserver://localhost:1433;databaseName=" + DB_PLACEHOLDER, "select 1"),

		/** HSQL DB */
		HSQL("org.hsqldb.jdbc.JDBCDriver", "org.hsqldb.jdbc.JDBCDataSource",
				"jdbc:hsqldb:hsql://localhost:9001/" + DB_PLACEHOLDER, "select 1 from INFORMATION_SCHEMA.SYSTEM_USERS");

		private final String defaultDriver;
		private final String templateUrl;
		private String validationQuery;
		private String dataSourceClassName;

		private DatabaseType(String defaultDriver, String dataSourceClassName, String templateUrl,
				String validationQuery) {
			this.defaultDriver = defaultDriver;
			this.dataSourceClassName = dataSourceClassName;
			this.templateUrl = templateUrl;
			this.validationQuery = validationQuery;
		}

		/**
		 * fully qualified class-name of the {@code java.sql.Driver} for this type
		 */
		public String getDefaultDriver() {
			return defaultDriver;
		}

		/** an example JDBC-URL */
		public String getTemplateUrl() {
			return templateUrl;
		}

		/** the default validation query */
		public String getDefaultValidationQuery() {
			return validationQuery;
		}

		/**
		 * fully qualified class-name of the {@code javax.sql.DataSource} for this type
		 */
		public String getDataSourceClassName() {
			return dataSourceClassName;
		}

		String getDatabaseName(String jdbcUrl) {
			int paramStart = jdbcUrl.indexOf('?') < 0 ? jdbcUrl.length() : jdbcUrl.indexOf('?');
			switch (this) {

			case MSSQL:
				return jdbcUrl.substring(jdbcUrl.lastIndexOf(DATABASE_NAME) + DATABASE_NAME.length(), paramStart);

			default:
				int beginIndex = jdbcUrl.indexOf('/', jdbcUrl.indexOf("//") + 2) + 1;
				return jdbcUrl.substring(beginIndex, paramStart);
			}
		}

	}

	private Integer id;
	private DatabaseType type;
	private String name;
	private String jdbcUrl;
	private String userName;
	private byte[] password;
	private String driverClass;
	private Date version;
	private String description;
	private Site site;
	private boolean managed;
	private boolean active;

	private Integer minConnections = 1;
	private Integer maxConnections = 20;
	private String validationQuery;
	private Integer validationPeriod;
	private Double databaseSize;

	private MigrationInfoService migrationInfoService;

	public DatabaseConnection() {

	}

	public DatabaseConnection(DatabaseType type, String databaseName, String userName, byte[] password) {
		this.type = type;
		this.jdbcUrl = type.getTemplateUrl().replace(DB_PLACEHOLDER, databaseName);
		this.driverClass = type.getDefaultDriver();
		this.userName = userName;
		this.password = ArrayUtils.clone(password);
		this.validationQuery = type.getDefaultValidationQuery();
	}

	public DatabaseConnection(DatabaseType type, String jdbcUrl, String driverClass, String userName, byte[] password,
			String validationQuery) {
		this.type = type;
		this.jdbcUrl = jdbcUrl;
		this.driverClass = driverClass;
		this.userName = userName;
		this.password = ArrayUtils.clone(password);
		this.validationQuery = validationQuery;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Enumerated(EnumType.STRING)
	public DatabaseType getType() {
		return type;
	}

	public void setType(DatabaseType type) {
		this.type = type;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "jdbc_url")
	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Column(name = "username")
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Lob
	public byte[] getPassword() {
		return password;
	}

	public void setPassword(byte[] password) {
		this.password = ArrayUtils.clone(password);
	}

	@Column(name = "driver_class")
	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	public String getDriverClass() {
		return driverClass;
	}

	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	@Version
	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@ManyToOne(targetEntity = SiteImpl.class)
	@JoinColumn(name = "site_id", referencedColumnName = "id")
	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Column(name = "min_connections")
	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Min(value = 1, message = ValidationMessages.VALIDATION_MIN)
	public Integer getMinConnections() {
		return minConnections;
	}

	public void setMinConnections(Integer minConnections) {
		this.minConnections = minConnections;
	}

	@Column(name = "max_connections")
	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	public Integer getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(Integer maxConnections) {
		this.maxConnections = maxConnections;
	}

	public boolean isManaged() {
		return managed;
	}

	public void setManaged(boolean managed) {
		this.managed = managed;
	}

	@Column(name = "validation_query")
	public String getValidationQuery() {
		return validationQuery;
	}

	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	@Transient
	public String getPasswordPlain() {
		return null == password ? "" : new String(password);
	}

	public void setPasswordPlain(String passwordPlain) {
		if (null != passwordPlain) {
			setPassword(passwordPlain.getBytes());
		}
	}

	@Transient
	public Integer getValidationPeriod() {
		return validationPeriod;
	}

	public void setValidationPeriod(Integer validationPeriod) {
		this.validationPeriod = validationPeriod;
	}

	public void registerDriver(boolean throwException) {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Driver> driverClazz = (Class<? extends Driver>) Class.forName(driverClass);
			DriverManager.registerDriver(driverClazz.newInstance());
			LOGGER.info("Registered JDBC driver {}", driverClass);
		} catch (Exception e) {
			if (throwException) {
				throw new RuntimeException("Error while registering driver " + driverClass, e);
			} else {
				LOGGER.warn("Driver {} could not be loaded.", driverClass);
			}
		}
	}

	public boolean testConnection(StringBuilder dbInfo) {
		return testConnection(dbInfo, false);
	}

	public boolean testConnection(StringBuilder dbInfo, boolean determineSize) {
		ConnectionCallback<Void> infoCallback = new ConnectionCallback<Void>() {
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				if (null != dbInfo) {
					DatabaseMetaData metaData = con.getMetaData();
					dbInfo.append(metaData.getDatabaseProductName() + StringUtils.SPACE
							+ metaData.getDatabaseProductVersion());
				}
				return null;
			}
		};
		return testConnection(determineSize, infoCallback);
	}

	public boolean testConnection(boolean determineSize, ConnectionCallback<?>... callbacks) {
		try {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
			for (ConnectionCallback<?> connectionCallback : callbacks) {
				jdbcTemplate.execute(connectionCallback);
			}

			if (determineSize) {
				String resource = String.format("db/init/%s/size.sql", getType().name().toLowerCase());
				InputStream sqlFile = getClass().getClassLoader().getResourceAsStream(resource);
				if (null != sqlFile) {
					String sizeStatement = StringUtils.replace(IOUtils.toString(sqlFile, StandardCharsets.UTF_8),
							"<database>", getDatabaseName());
					jdbcTemplate.query(sizeStatement, new RowMapper<Void>() {
						public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
							setDatabaseSize(rs.getDouble(1));
							return null;
						}
					});
				}
			}
			return true;
		} catch (Exception e) {
			LOGGER.warn("error while connecting to {} ({}: {})", jdbcUrl, e.getClass().getName(), e.getMessage());
		}
		return false;
	}

	@Transient
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(jdbcUrl, userName, new String(password));
	}

	public void closeConnection(Connection connection) {
		if (null != connection) {
			try {
				connection.close();
			} catch (SQLException e) {
				LOGGER.warn("error while closing connection", e);
			}
		}
	}

	public String getDatabaseConnectionString(String databaseName) {
		switch (type) {
		case MYSQL:
			return getJdbcUrl().substring(0, getJdbcUrl().lastIndexOf('/') + 1) + databaseName;

		case MSSQL:
			return getJdbcUrl().substring(0, getJdbcUrl().indexOf(DATABASE_NAME) + DATABASE_NAME.length())
					+ databaseName;
		case HSQL:
			return getJdbcUrl().substring(0, getJdbcUrl().lastIndexOf('/') + 1) + databaseName;

		default:
			return null;
		}
	}

	@Transient
	public String getDatabaseName() {
		return type.getDatabaseName(jdbcUrl);
	}

	@Transient
	public boolean isRootConnection() {
		return getSite() == null;
	}

	@Transient
	public MigrationInfoService getMigrationInfoService() {
		return migrationInfoService;
	}

	public void setMigrationInfoService(MigrationInfoService migrationInfoService) {
		this.migrationInfoService = migrationInfoService;
	}

	@Transient
	public Double getDatabaseSize() {
		return databaseSize;
	}

	public void setDatabaseSize(Double databaseSize) {
		this.databaseSize = databaseSize;
	}

	@Override
	public String toString() {
		return (type == null ? "Unknown" : type.toString()) + " " + getJdbcUrl();
	}

	@Transient
	public DataSource getDataSource() {
		return new DriverManagerDataSource(jdbcUrl, userName, getPasswordPlain());
	}

}
