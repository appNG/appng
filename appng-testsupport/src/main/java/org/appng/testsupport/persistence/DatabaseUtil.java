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
package org.appng.testsupport.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.excel.XlsDataSet;
import org.dbunit.dataset.xml.FlatDtdDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.FlatXmlWriter;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class helping importing/exporting data and retrieving connection information.
 * 
 * @author Matthias Müller
 *
 */
public class DatabaseUtil {

	private final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
	private final static ClassLoader CLASS_LOADER = DatabaseUtil.class.getClassLoader();
	private final FlatXmlDataSetBuilder XML_BUILDER = new FlatXmlDataSetBuilder();
	private IDatabaseConnection connection;
	private Connection jdbcConnection;
	private final String SCHEMA = null;

	private static volatile boolean imported = false;

	private synchronized void openConnection() throws Exception {
		try {
			jdbcConnection = getJDBConnection();
			logger.info("JDBC-Connection created: " + jdbcConnection);
			connection = getConnection();
			logger.debug("connection created: " + connection.getClass().getName());
		} catch (Exception e) {
			logger.error("an error occured", e);
			throw e;
		}
	}

	private Connection getJDBConnection() throws ClassNotFoundException, SQLException {
		String connectionUrl = connectionInfo.getJdbcUrl();
		String driver = connectionInfo.getDriverClass();
		String userName = connectionInfo.getUser();
		String password = connectionInfo.getPassword();

		logger.info("using datasource " + connectionUrl + " " + userName + "/****");
		Class.forName(driver);
		Connection connection = DriverManager.getConnection(connectionUrl, userName, password);
		return connection;
	}

	private ConnectionInfo connectionInfo;

	public DatabaseUtil(ConnectionInfo info) {
		this.connectionInfo = info;
	}

	public void importData(String name) throws Exception {
		importData(name, false);
	}

	public void importData(String name, boolean force) throws Exception {
		importData(name, force, true);
	}

	public void importData(Class<? extends TestDataProvider> testDataProviderClass) throws Exception {
		logger.info("found TestDataProvider " + testDataProviderClass);
		TestDataProvider testDataProvider = testDataProviderClass.newInstance();
		clearDBJPA(true, testDataProvider);
	}

	public void importData(TestDataProvider testDataProvider) throws Exception {
		clearDBJPA(true, testDataProvider);
	}

	public void importData(String name, boolean force, boolean clearDb) throws Exception {
		if (!imported || force) {
			XML_BUILDER.setColumnSensing(true);
			long start = System.currentTimeMillis();
			long end = -1;
			try {
				clearDBJPA(clearDb, null);
				openConnection();
				importFile("dbunit/" + name + ".xml");
				shutDown();
				imported = true;
			} catch (Exception e) {
				throw e;
			} finally {
				end = System.currentTimeMillis();
			}
			logger.info("finished DBExport in " + (end - start) + "ms");
		}
	}

	public void exportData(String dataName) {
		exportData(dataName, null);
	}

	public void exportData(String dataName, boolean clearDb, Class<? extends TestDataProvider> testDataProviderClass) {
		XML_BUILDER.setColumnSensing(true);
		long start = System.currentTimeMillis();
		try {
			openConnection();
			logger.info("found TestDataProvider " + testDataProviderClass);
			TestDataProvider testDataProvider = testDataProviderClass.newInstance();
			clearDBJPA(clearDb, testDataProvider);
			export(dataName);
			shutDown();
		} catch (Exception e) {
			logger.error("an error occured", e);
		} finally {
			long end = System.currentTimeMillis();
			logger.info("finished DBExport in " + (end - start) + "ms");
		}
	}

	public void exportData(String dataName, Class<? extends TestDataProvider> testDataProvider) {
		exportData(dataName, true, testDataProvider);
	}

	public void shutDown() throws SQLException {
		try {
			connection.close();
			jdbcConnection.close();
			logger.debug("shutting down connection " + connection.getClass().getSimpleName());
		} catch (SQLException e) {
			logger.error("error whole closing the connection", e);
			throw e;
		}
	}

	public void importData(Map<String, String> properties, Class<? extends TestDataProvider> testDataProvider)
			throws Exception {
		Map<String, String> copy = new HashMap<String, String>(properties);
		copy.put("hibernate.hbm2ddl.auto", "create");
		EntityManagerFactory emf = null;
		EntityManager em = null;
		EntityTransaction tx = null;
		try {
			logger.info("clearing database...");
			Map<String, String> props = new HashMap<String, String>();
			props.put("hibernate.hbm2ddl.auto", "create");
			emf = Persistence.createEntityManagerFactory(connectionInfo.getPersistenceUnit(), copy);
			em = emf.createEntityManager();
			tx = em.getTransaction();
			tx.begin();
			if (testDataProvider != null) {
				logger.info("writing testdata");
				testDataProvider.newInstance().writeTestData(em);
				logger.info("done importing testdata");
			}
			logger.info("...done clearing");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		} finally {
			if (tx != null) {
				if (tx.getRollbackOnly()) {
					tx.rollback();
				} else {
					tx.commit();
				}
			}
			if (em != null) {
				em.close();
			}
			if (emf != null) {
				emf.close();
			}
		}
	}

	public void clearDBJPA(boolean createSchema, TestDataProvider testDataProvider) throws Exception {
		clearDBJPA(createSchema, false, testDataProvider);
	}

	public void clearDBJPA(boolean createSchema, boolean showSql, TestDataProvider testDataProvider) throws Exception {

		EntityManagerFactory emf = null;
		EntityManager em = null;
		try {
			logger.info("clearing database...");
			Map<String, String> props = new HashMap<String, String>();
			if (createSchema) {
				props.put("hibernate.hbm2ddl.auto", "create");
				if (showSql) {
					props.put("hibernate.show_sql", "true");
				}
				logger.info("setting hibernate.hbm2ddl.auto to 'create'");
			}
			emf = Persistence.createEntityManagerFactory(connectionInfo.getPersistenceUnit(), props);
			em = emf.createEntityManager();
			em.getTransaction().begin();

			logger.info("EntityManager created");
			logger.info("EntityTransaction created");
			logger.info("EntityTransaction started");

			if (testDataProvider != null) {
				logger.info("writing testdata");
				testDataProvider.writeTestData(em);
				logger.info("done importing testdata");
			}
			logger.info("...done clearing");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
		em.getTransaction().commit();
		em.close();
		emf.close();
	}

	public void clearDB() {
		try {
			logger.info("clearing database...");
			for (int i = connectionInfo.getTableNames().size(); i > 0; i--) {
				String table = connectionInfo.getTableNames().get(i - 1);
				Statement statement = jdbcConnection.createStatement();
				String stmt = "delete from " + ((null != SCHEMA) ? SCHEMA + "." : "") + table;
				int rows = statement.executeUpdate(stmt);
				logger.debug(".....clearing " + table + " (" + rows + " rows deleted)");
				statement.close();
			}

			logger.info("...done clearing");
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		} finally {
		}
	}

	private void importFile(String path) throws Exception {
		long start = System.currentTimeMillis();
		try {
			execute(path, DatabaseOperation.INSERT);
			long end = System.currentTimeMillis();
			logger.info("imported " + path + " in " + (end - start) + "ms");
		} catch (Exception e) {
			logger.error("error while importing file", e);
			throw e;
		}
	}

	public void delete(String path) {
		try {
			execute(path, DatabaseOperation.DELETE);
		} catch (Exception e) {
			logger.error("error while importing file", e);
		}
	}

	private void execute(String path, DatabaseOperation operation) throws Exception {
		ClassLoader classLoader = CLASS_LOADER;
		IDataSet dataSet = null;
		if (path.endsWith(".xls")) {
			InputStream file = classLoader.getResourceAsStream(path);
			dataSet = new XlsDataSet(file);
		} else if (path.endsWith(".xml")) {
			URL file = classLoader.getResource(path);
			dataSet = XML_BUILDER.build(file);
		} else {
			throw new DataSetException("unknow data type");
		}
		operation.execute(connection, dataSet);
	}

	public void export(String name) throws Exception {
		if (null == connection) {
			openConnection();
		}
		QueryDataSet dataSet = new QueryDataSet(connection);
		for (String table : connectionInfo.getTableNames()) {
			logger.info("adding table '" + table + "' to testdata");
			dataSet.addTable(table);
		}
		exportDTD(dataSet, name);
		exportXml(dataSet, name);
	}

	private void exportDTD(QueryDataSet dataSet, String name) throws Exception {
		File outFolder = new File("src/test/resources/dbunit/");
		if (!outFolder.exists()) {
			outFolder.mkdirs();
		}
		File out = new File(outFolder, name + ".dtd");
		FlatDtdDataSet.write(dataSet, new FileOutputStream(out));
		logger.info("created " + out.getAbsolutePath());
	}

	private void exportXml(IDataSet dataSet, String name) throws Exception {
		File out = new File("src/test/resources/dbunit/" + name + ".xml");
		FileOutputStream outStream = new FileOutputStream(out);
		FlatXmlWriter datasetWriter = new FlatXmlWriter(outStream);
		datasetWriter.setDocType(name + ".dtd");
		datasetWriter.write(dataSet);
		logger.info("created " + out.getAbsolutePath());
	}

	private void exportXls(IDataSet dataSet, String file) throws Exception {
		XlsDataSet.write(dataSet, new FileOutputStream("src/test/resources/" + file));
	}

	public void xlsToXml(String file) throws Exception {
		IDataSet dataSet = new XlsDataSet(CLASS_LOADER.getResourceAsStream(file));
		exportXml(dataSet, file.replace("xls", "xml"));
	}

	public void xmlToXls(String file) throws Exception {
		URL resource = CLASS_LOADER.getResource(file);
		IDataSet dataSet = XML_BUILDER.build(resource);
		exportXls(dataSet, file.replace("xml", "xls"));
	}

	private IDatabaseConnection getConnection() throws Exception, DatabaseUnitException {
		Constructor<? extends IDatabaseConnection> constructor = connectionInfo.getConnection().getConstructor(
				Connection.class, String.class);
		IDatabaseConnection connection = constructor.newInstance(jdbcConnection, null);
		return connection;
	}

	public ConnectionInfo getConnectionInfo() {
		return connectionInfo;
	}

	/**
	 * Uses system-property 'hsqlPort' and returns a map containing
	 * {@value org.appng.testsupport.persistence.ConnectionHelper#HIBERNATE_CONNECTION_URL} = <jdbcUrl-with-hsqlPort>
	 * 
	 * @param class1
	 * @return a {@link Map} containing the
	 *         {@value org.appng.testsupport.persistence.ConnectionHelper#HIBERNATE_CONNECTION_URL}-property
	 * @throws Exception
	 */
	public static Map<String, String> importTestData(Class<? extends TestDataProvider> class1) throws Exception {
		ConnectionInfo connectionInfo = ConnectionHelper.getHSqlConnectionInfo();
		return importTestData(class1, connectionInfo);
	}

	public static Map<String, String> importTestData(Class<? extends TestDataProvider> class1,
			ConnectionInfo connectionInfo) throws Exception {
		String jdbcUrl = connectionInfo.getJdbcUrl();
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(ConnectionHelper.HIBERNATE_CONNECTION_URL, jdbcUrl);
		DatabaseUtil databaseUtil = new DatabaseUtil(connectionInfo);
		databaseUtil.importData(properties, class1);
		return properties;
	}

}
