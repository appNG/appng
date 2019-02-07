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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.catalina.startup.Tomcat;

public class ApplicationStartup {

	private static final String APPLICATION_WAR = "appng-application-%s.war";
	private static final String APPNGIZER_WAR = "appng-appngizer-%s.war";
	private static final String AUTO_INSTALL_LIST = "auto-install.list";
	private static final String APPNG_PROPERTIES = "appNG.properties";
	private static final String LOG4J_PROPERTIES = "log4j.properties";
	private static final String APPNG_VERSION = "appng.version";
	private static final String INSTALL = "-i";
	private static final String UNZIP = "-u";
	private static final String PORT = "-p";

	public static void main(String[] args) throws Exception {
		List<String> arguments = Arrays.asList(args);

		File appng = new File("appng");
		File appNGizer = new File("appNGizer");
		File webInf = new File(appng, "WEB-INF");
		boolean unzip = arguments.contains(UNZIP);
		if (unzip) {
			Properties props = new Properties();
			props.load(ApplicationStartup.class.getClassLoader().getResourceAsStream(APPNG_PROPERTIES));
			String appngVersion = props.getProperty(APPNG_VERSION);

			unzipWarFile(appng, String.format(APPLICATION_WAR, appngVersion));
			File bin = new File(webInf, "bin");
			copyTo("script/appng", new File(bin, "appng")).setExecutable(true);
			copyTo("script/appng.bat", new File(bin, "appng.bat"));

			unzipWarFile(appNGizer, String.format(APPNGIZER_WAR, appngVersion));
			String appNGHome = appng.getAbsolutePath();
			System.out.println("Setting ${appNGHome}: " + appNGHome);
			replaceInFile("appNGizer/META-INF/context.xml", "${appNGHome}", appNGHome);
			replaceInFile("appNGizer/WEB-INF/web.xml", "${appNGHome}", appNGHome);
		}
		boolean install = arguments.contains(INSTALL);
		if (install) {
			File conf = new File(webInf, "conf");
			copy(APPNG_PROPERTIES, conf);
			copy(LOG4J_PROPERTIES, conf);
			doInstall(arguments, conf);
		}

		List<URL> urls = new ArrayList<>();
		addLib(urls, new File(webInf, "lib"));
		URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
		Thread.currentThread().setContextClassLoader(urlClassLoader);

		System.out.println("Starting Tomcat");
		Tomcat tomcat = new Tomcat();
		System.setProperty("appng.localRepoPath", new File("repository").toURI().toString());
		int port = 8080;
		if (arguments.indexOf(PORT) > -1) {
			port = Integer.valueOf(arguments.get(arguments.indexOf(PORT) + 1));
		}
		tomcat.setPort(port);
		tomcat.setBaseDir("");
		tomcat.addWebapp("", appng.getAbsolutePath());
		tomcat.addWebapp("/appNGizer", appNGizer.getAbsolutePath());
		tomcat.start();
		tomcat.getServer().await();
	}

	protected static void replaceInFile(String file, String search, String replacement) throws IOException {
		Path path = Paths.get(file);
		Charset charset = StandardCharsets.UTF_8;
		String content = new String(Files.readAllBytes(path), charset);
		if (System.getProperty("os.name").startsWith("Windows")) {
			replacement = "/" + replacement;
		}
		content = content.replaceAll(Pattern.quote(search), Matcher.quoteReplacement(replacement));
		Files.write(path, content.getBytes(charset));
	}

	protected static void unzipWarFile(File targetFolder, String warFile)
			throws FileNotFoundException, ZipException, IOException {
		File warFileAbsolute = new File(warFile).getAbsoluteFile();
		if (!warFileAbsolute.exists()) {
			throw new FileNotFoundException(warFileAbsolute.getAbsolutePath() + " does not exist!");
		}
		System.out.print("Unzipping WAR-Archive " + warFileAbsolute.getName() + " ...... ");
		unzipWar(targetFolder, warFileAbsolute);
		System.out.println("done!");
	}

	protected static void doInstall(List<String> arguments, File conf) throws IOException, FileNotFoundException {
		String installFile = null;
		int idx = arguments.indexOf(INSTALL);
		if (arguments.size() > (idx + 1) && !arguments.get(idx + 1).startsWith("-")) {
			installFile = arguments.get(idx + 1);
			File file = new File(installFile);
			if (file.exists()) {
				write(new FileInputStream(installFile), new FileOutputStream(new File(conf, AUTO_INSTALL_LIST)));
			} else {
				System.err.println("no such file: " + installFile);
				installFile = null;
			}
		} else {
			installFile = AUTO_INSTALL_LIST;
			copy(AUTO_INSTALL_LIST, conf);
		}
		if (null != installFile) {
			System.out.println("Installing from " + installFile);
		}
	}

	protected static void unzipWar(File appng, File warFile) throws ZipException, IOException, FileNotFoundException {
		ZipFile zipFile = new ZipFile(warFile);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			if (zipEntry.isDirectory()) {
				new File(appng, zipEntry.getName()).mkdirs();
			} else {
				File target = new File(appng, zipEntry.getName());
				if (!target.exists()) {
					File folder = target.getParentFile();
					if (null != folder && !target.getParentFile().exists()) {
						target.getParentFile().mkdirs();
					}
					write(zipFile.getInputStream(zipEntry), new FileOutputStream(target));
				}
			}
		}
		zipFile.close();
	}

	private static File copy(String name, File targetFolder) throws IOException {
		File out = new File(targetFolder, name);
		System.out.println(String.format("Copying %s to %s", name, targetFolder));
		return copyTo(name, out);
	}

	private static File copyTo(String name, File targetFile) throws IOException, FileNotFoundException {
		write(ApplicationStartup.class.getClassLoader().getResourceAsStream(name), new FileOutputStream(targetFile));
		return targetFile;
	}

	private static void addLib(List<URL> urls, File lib) {
		for (String jar : lib.list()) {
			try {
				urls.add(new File(lib, jar).toURI().toURL());
			} catch (MalformedURLException e) {
			}
		}
	}

	private static void write(InputStream in, OutputStream out) throws IOException {
		int nRead;
		byte[] data = new byte[1024];
		while ((nRead = in.read(data, 0, data.length)) != -1) {
			out.write(data, 0, nRead);
		}
		in.close();
		out.close();
	}
}
