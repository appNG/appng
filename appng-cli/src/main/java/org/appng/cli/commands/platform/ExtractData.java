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
package org.appng.cli.commands.platform;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.model.Properties;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.service.PropertySupport;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

/**
 * Extracts the data from {@code $APPNG_HOME} to {@code $appngData}.<br/>
 * This includes moving
 * <ul>
 * <li>{@code WEB-INF/web.xml} to {@code $appngData/WEB-INF/web.xml}</li>
 * <li>{@code WEB-INF/conf} to {@code $appngData/conf}</li>
 * <li>{@code WEB-INF/bin} to {@code $appngData/bin}</li>
 * <li>{@code WEB-INF/logs} to {@code $appngData/logs}</li>
 * <li>{@code /applications} to {@code $appngData/applications}</li>
 * <li>{@code /repository} to {@code $appngData/repository}</li>
 * </ul>
 * .
 * 
 * <p>
 * Additionally, a <a href="http://tomcat.apache.org/tomcat-8.5-doc/config/resources.html">&lt;Resources&gt;</a> element
 * is being added to {@code $APPNG_HOME/META-INF/context.xml}, referencing the {@code appngData} directory.
 * </p>
 * 
 * <pre>
 * Usage: extract-data [options]
 *   Options:
 *   * -appngData
 *       The folder to extract the data to.
 *     -copy
 *       Copy data instead of moving.
 *       Default: false 
 *     -revert
 *       Revert previously extracted data.
 *       Default: false
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@Parameters(commandDescription = "Extracts the data from $APPNG_HOME to the given folder.")
public class ExtractData implements ExecutableCliCommand {

	private static final String CONF = "conf";
	private static final String LOG = "log";
	private static final String WEB_XML = "web.xml";
	private static final String WEB_INF = "WEB-INF";
	private static final String PRIVILEGED_CONTEXT = "<Context privileged=\"true\">";
	private static final String NEWLINE = "\r\n";
	private static final String $APPNG_HOME = "${appng.home}";
	private static final String $APPNG_DATA = "${appng.data}";
	private static final String $WEBAPP_ROOT = "${webapp.root}/" + WEB_INF;
	@Parameter(names = "-appngData", required = true, description = "The folder to extract the data to.", converter = FileConverter.class)
	private File appngData;

	@Parameter(names = "-copy", description = "Copy data instead of moving.")
	private boolean copy = false;

	@Parameter(names = "-revert", description = "Revert previously extracted data.")
	private boolean revert = false;

	public ExtractData() {

	}

	public void execute(CliEnvironment cle) throws BusinessException {
		if (appngData.isFile()) {
			CliEnvironment.out.println(String.format("%s is a not a directory!", appngData));
			return;
		}
		appngData.mkdirs();
		Properties platformConfig = cle.getPlatformConfig();
		String appngHome = platformConfig.getString(Platform.Property.PLATFORM_ROOT_PATH);
		String applicationDir = platformConfig.getString(Platform.Property.APPLICATION_DIR);
		String repositoryPath = platformConfig.getString(Platform.Property.REPOSITORY_PATH);
		extract(appngHome, cle, applicationDir, repositoryPath);
	}

	protected void extract(String appngHome, CliEnvironment cle, String applicationDir, String repositoryPath)
			throws BusinessException {
		try {
			move(Paths.get(appngHome, WEB_INF, WEB_XML), Paths.get(appngData.toString(), WEB_INF, WEB_XML));
			move(Paths.get(appngHome, WEB_INF, CONF), Paths.get(appngData.toString(), CONF));
			move(Paths.get(appngHome, WEB_INF, LOG), Paths.get(appngData.toString(), LOG));
			move(Paths.get(appngHome, applicationDir), Paths.get(appngData.toString(), applicationDir));
			move(Paths.get(appngHome, repositoryPath), Paths.get(appngData.toString(), repositoryPath));
			writeContextXml(appngHome);
			setCacheConfig(cle);
			File log4jProperties = Paths.get(appngData.toString(), CONF, "log4j.properties").toFile();
			if (revert) {
				replaceInFile(log4jProperties, $APPNG_DATA, $WEBAPP_ROOT);
			} else {
				replaceInFile(log4jProperties, $WEBAPP_ROOT, $APPNG_DATA);
				File binFolder = new File(appngData, "bin");
				binFolder.mkdirs();
				copyBatchFileToBin(binFolder, appngData.toString(), appngHome, "appng");
				copyBatchFileToBin(binFolder, appngData.toString(), appngHome, "appng.bat");
			}
		} catch (IOException | URISyntaxException e) {
			throw new BusinessException(e);
		}
	}

	protected void copyBatchFileToBin(File binFolder, String appngData, String appngHome, String name)
			throws IOException {
		File targetFile = new File(binFolder, name);
		try (InputStream is = getClass().getResourceAsStream(name); FileWriter fw = new FileWriter(targetFile);) {
			IOUtils.copy(is, fw, StandardCharsets.UTF_8);
		}
		targetFile.setExecutable(true, true);
		replaceInFile(targetFile, $APPNG_DATA, appngData.toString());
		replaceInFile(targetFile, $APPNG_HOME, appngHome);
	}

	protected void writeContextXml(String appngHome) throws IOException, URISyntaxException {
		Path contextXml = Paths.get(appngHome, "META-INF", "context.xml");
		Charset charset = StandardCharsets.UTF_8;
		String contextXmlContent = new String(Files.readAllBytes(contextXml), charset);
		URL resource = getClass().getClassLoader().getResource("context-resources.xml");
		try (FileSystem fs = FileSystems.newFileSystem(resource.toURI(), new HashMap<>())) {
			String resources = new String(Files.readAllBytes(Paths.get(resource.toURI())), charset);
			String resourceContext = PRIVILEGED_CONTEXT + NEWLINE + resources;
			if (revert) {
				contextXmlContent = contextXmlContent.replace(resourceContext, PRIVILEGED_CONTEXT);
			} else {
				contextXmlContent = contextXmlContent.replace(PRIVILEGED_CONTEXT, resourceContext);
			}
			Files.write(contextXml, contextXmlContent.getBytes(charset));
			CliEnvironment.out.println(String.format("Updated %s", contextXml.toString()));
		}
	}

	protected void setCacheConfig(CliEnvironment cle) {
		PropertyImpl cacheConfig = cle.getCoreService()
				.getProperty(PropertySupport.PREFIX_PLATFORM + Platform.Property.CACHE_CONFIG);
		String defaultValue = "conf/hazelcast.xml";
		if (revert) {
			defaultValue = WEB_INF + "/" + defaultValue;
		}
		cacheConfig.setString(defaultValue);
		CliEnvironment.out
				.println(String.format("Setting default value for %s: %s", cacheConfig.getName(), defaultValue));
		cle.getCoreService().saveProperty(cacheConfig);
	}

	private File move(Path source, Path target) throws IOException {
		File sourceFile = revert ? target.toFile() : source.toFile();
		File targetFile = revert ? source.toFile() : target.toFile();
		if (sourceFile.exists()) {
			return moveResource(sourceFile, targetFile);
		} else {
			CliEnvironment.out.println(String.format("%s does not exist.", sourceFile));
			return null;
		}
	}

	protected static void replaceInFile(File file, String search, String replacement) throws IOException {
		Path path = file.toPath();
		Charset charset = StandardCharsets.UTF_8;
		String content = new String(Files.readAllBytes(path), charset);
		content = content.replaceAll(Pattern.quote(search), Matcher.quoteReplacement(replacement));
		Files.write(path, content.getBytes(charset));
		CliEnvironment.out.println("Replaced " + search + " with " + replacement + " in " + path);
	}

	private File moveResource(File sourceFile, File targetFile) throws IOException {
		if (copy) {
			if (sourceFile.isFile()) {
				FileUtils.copyFile(sourceFile, targetFile, true);
			} else {
				FileUtils.copyDirectory(sourceFile, targetFile, true);
			}
		} else {
			if (sourceFile.isFile()) {
				FileUtils.moveFile(sourceFile, targetFile);
			} else {
				FileUtils.moveDirectory(sourceFile, targetFile);
			}
		}
		CliEnvironment.out.println(String.format("%s\t%s -> %s", copy ? "copied" : "moved", sourceFile, targetFile));
		return targetFile;
	}
}
