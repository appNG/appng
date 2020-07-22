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
package org.appng.standalone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

public class ApplicationStartup {

	private static final String APPLICATION_WAR = "appng-application-%s.war";
	private static final String AUTO_INSTALL_LIST = "auto-install.list";
	private static final String APPNG_PROPERTIES = "appNG.properties";
	private static final String LOG4J_PROPERTIES = "log4j.properties";
	private static final String URLREWRITE = "urlrewrite.xml";
	private static final String APPNG_VERSION = "appng.version";
	private static final String PORT = "-p";
	private static final String INSTALL = "-i";

	public static void main(String[] args) throws Exception {
		List<String> arguments = Arrays.asList(args);

		File webapps = new File("webapps");
		File appng = new File(webapps, "appng");
		File webInf = new File(appng, "WEB-INF");
		boolean install = arguments.contains(INSTALL);

		if (install) {
			System.out.println("-i is set, deleting " + webapps.getAbsolutePath());
			delete(webapps);
		}

		Properties props = new Properties();
		props.load(ApplicationStartup.class.getClassLoader().getResourceAsStream(APPNG_PROPERTIES));
		String appngVersion = props.getProperty(APPNG_VERSION);

		if (appng.exists()) {
			System.out.println("appNG installed at " + appng.getAbsolutePath());
		} else {
			unzipWarFile(appng, String.format(APPLICATION_WAR, appngVersion));
			File bin = new File(webInf, "bin");
			copyTo("script/appng", new File(bin, "appng")).setExecutable(true);
			copyTo("script/appng.bat", new File(bin, "appng.bat"));
		}

		File conf = new File(webInf, "conf");
		String[] installFiles = conf.list((dir, name) -> name.startsWith(AUTO_INSTALL_LIST));
		if (installFiles.length == 0) {
			System.setProperty("appng.localRepoPath", new File("repository").toURI().toString());
			copy(AUTO_INSTALL_LIST, conf);
			copy(APPNG_PROPERTIES, conf);
			copy(URLREWRITE, new File(appng, "repository/manager/meta/conf/"));
			copy(LOG4J_PROPERTIES, conf);
		} else {
			System.out.println("Installation protocoll: " + new File(conf, installFiles[0]));
		}

		System.out.println("Starting Tomcat");
		System.setProperty("catalina.base", new File("").getAbsolutePath());
		int port = 8080;
		if (arguments.indexOf(PORT) > -1) {
			port = Integer.valueOf(arguments.get(arguments.indexOf(PORT) + 1));
		}
		Tomcat tomcat = new Tomcat();
		Connector connector = new Connector();
		connector.setPort(port);
		tomcat.setConnector(connector);
		tomcat.addWebapp("", appng.getAbsolutePath());
		tomcat.start();
		tomcat.getServer().await();
	}

	protected static void replaceInFile(File file, String search, String replacement) throws IOException {
		Path path = file.toPath();
		Charset charset = StandardCharsets.UTF_8;
		String content = new String(Files.readAllBytes(path), charset);
		content = content.replaceAll(Pattern.quote(search), Matcher.quoteReplacement(replacement));
		System.out.println("Replaced " + search + " with " + replacement + " in " + path);
		Files.write(path, content.getBytes(charset));
	}

	protected static boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	protected static void unzipWarFile(File targetFolder, String warFile)
			throws FileNotFoundException, ZipException, IOException {
		File warFileAbsolute = new File("archive", warFile).getAbsoluteFile();
		if (!warFileAbsolute.exists()) {
			throw new FileNotFoundException(warFileAbsolute.getAbsolutePath() + " does not exist!");
		}
		System.out.print("Unzipping WAR-Archive " + warFileAbsolute.getName() + " to " + targetFolder.getAbsolutePath()
				+ " ...... ");
		unzipWar(targetFolder, warFileAbsolute);
		System.out.println("done!");
	}

	protected static void unzipWar(File targetFolder, File warFile)
			throws ZipException, IOException, FileNotFoundException {
		ZipFile zipFile = new ZipFile(warFile);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			if (zipEntry.isDirectory()) {
				new File(targetFolder, zipEntry.getName()).mkdirs();
			} else {
				File target = new File(targetFolder, zipEntry.getName());
				if (!target.exists()) {
					File folder = target.getParentFile();
					if (null != folder && !target.getParentFile().exists()) {
						target.getParentFile().mkdirs();
					}
					try (InputStream in = zipFile.getInputStream(zipEntry);
							OutputStream out = new FileOutputStream(target)) {
						write(in, out);
					}
				}
			}
		}
		zipFile.close();
	}

	private static File copy(String name, File targetFolder) throws IOException {
		targetFolder.mkdirs();
		System.out.println(String.format("Copying %s to %s", name, targetFolder));
		return copyTo(name, new File(targetFolder, name));
	}

	private static File copyTo(String name, File targetFile) throws IOException, FileNotFoundException {
		try (InputStream in = ApplicationStartup.class.getClassLoader().getResourceAsStream(name);
				OutputStream out = new FileOutputStream(targetFile)) {
			write(in, out);
			return targetFile;
		}
	}

	private static void write(InputStream in, OutputStream out) throws IOException {
		int nRead;
		byte[] data = new byte[1024];
		while ((nRead = in.read(data, 0, data.length)) != -1) {
			out.write(data, 0, nRead);
		}
	}

	static boolean delete(File dir) {
		File[] files = dir.listFiles();
		if (null != files) {
			for (File file : files) {
				delete(file);
			}
		}
		return dir.delete();
	}
}
