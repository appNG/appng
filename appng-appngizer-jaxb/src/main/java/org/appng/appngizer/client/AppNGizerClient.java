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
package org.appng.appngizer.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.appng.appngizer.model.xml.Application;
import org.appng.appngizer.model.xml.Applications;
import org.appng.appngizer.model.xml.Database;
import org.appng.appngizer.model.xml.Grants;
import org.appng.appngizer.model.xml.Group;
import org.appng.appngizer.model.xml.Groups;
import org.appng.appngizer.model.xml.Home;
import org.appng.appngizer.model.xml.Nameable;
import org.appng.appngizer.model.xml.Package;
import org.appng.appngizer.model.xml.Permission;
import org.appng.appngizer.model.xml.Permissions;
import org.appng.appngizer.model.xml.Properties;
import org.appng.appngizer.model.xml.Property;
import org.appng.appngizer.model.xml.Repositories;
import org.appng.appngizer.model.xml.Repository;
import org.appng.appngizer.model.xml.Role;
import org.appng.appngizer.model.xml.Roles;
import org.appng.appngizer.model.xml.Site;
import org.appng.appngizer.model.xml.Sites;
import org.appng.appngizer.model.xml.Subject;
import org.appng.appngizer.model.xml.Subjects;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Interface for interacting with appNGizer
 */
public interface AppNGizerClient {

	Home welcome();

	Home login();

	// subjects
	Subjects subjects();

	Subject subject(String name);

	Subject createSubject(Subject subject);

	Subject updateSubject(String name, Subject subject);

	void deleteSubject(String name);

	// groups
	Groups groups();

	Group group(String name);

	Group createGroup(Group group);

	Group updateGroup(String name, Group group);

	void deleteGroup(String name);

	// applications
	Applications applications();

	Application application(String app);

	Application updateApplication(String app, Application application);

	void deleteApplication(String app);

	// application properties
	Properties applicationProperties(String app);

	Property createApplicationProperty(String app, Property property);

	Property updateApplicationProperty(String app, Property property);

	void deleteApplicationProperty(String app, String name);

	// roles
	Roles roles(String app);

	Role role(String app, String name);

	Role createRole(String app, Role role);

	Role updateRole(String app, String name, Role role);

	void deleteRole(String app, String name);

	// permissions
	Permissions permissions(String app);

	Permission permission(String app, String name);

	Permission createPermission(String app, Permission permission);

	Permission updatePermission(String app, String name, Permission permission);

	void deletePermission(String app, String name);

	// sites
	Sites sites();

	Site site(String name);

	Site createSite(Site site);

	Site updateSite(String name, Site site);

	void deleteSite(String name);

	void reloadSite(String name);

	// site properties
	Properties siteProperties(String site);

	Property siteProperty(String site, String name);

	Property createSiteProperty(String site, Property property);

	Property updateSiteProperty(String site, String name, Property property);

	void deleteSiteProperty(String site, String name);

	// site applications
	Applications applications(String site);

	Application application(String site, String app);

	void activateApplication(String site, String app);

	void deactivateApplication(String site, String app);

	Grants siteGrants(String site, String app);

	Grants updateSiteGrants(String site, String app, Grants grants);

	// site application properties
	Properties applicationProperties(String site, String app);

	Property createApplicationProperty(String site, String app, Property property);

	Property updateApplicationProperty(String site, String app, String name, Property property);

	void deleteApplicationProperty(String site, String app, String name);

	// repositories
	Repositories repositories();

	Repository repository(String name);

	Repository createRepository(Repository repository);

	Repository updateRepository(String name, Repository repository);

	void deleteRepository(String name);

	Package installPackage(String name, Package packageToInstall);

	Package uploadPackage(String name, File archive) throws IOException;

	// platform properties
	Properties platformProperties();

	Property platformProperty(String name);

	Property createPlatformProperty(Property property);

	Property updatePlatformProperty(String name, Property property);

	void deletePlatformProperty(String name);

	// others
	Properties environment();

	Properties system();

	Database database();

	Database initializeDatabase();

	/**
	 * Utility class to read and write {@link Properties} from/to YAML or JSON files.
	 */
	@Slf4j
	class Config {

		public enum Format {
			YAML, JSON
		}

		/**
		 * Reads a {@link Site}'s {@link Properties} with the given {@link AppNGizerClient} and writes these to the
		 * given {@link OutputStream} using YAML format.
		 * 
		 * @param appNGizer
		 *                       the {@link AppNGizerClient} to use
		 * @param name
		 *                       the name of the {@link Site}
		 * @param format
		 *                       the format to use
		 * @param out
		 *                       the target to write to
		 * @param nonDefaultOnly
		 *                       write only those properties where the value differs from the default value
		 * 
		 * @return the {@link Site}'s {@link Properties}
		 * 
		 * @throws IOException
		 *                     if an error occurred while writing the output
		 */
		public static Properties readSiteProperties(AppNGizerClient appNGizer, String name, OutputStream out,
				Format format, boolean nonDefaultOnly) throws IOException {
			Properties siteProperties = appNGizer.siteProperties(name);
			LOGGER.info("Read {} properties for site {}", siteProperties.getProperty().size(), name);
			SiteConfig wrapper = new SiteConfig();
			wrapper.setName(name);
			wrapper.setProperties(getWrapper(siteProperties, nonDefaultOnly).properties);

			List<Application> applications = appNGizer.applications(name).getApplication();
			Map<String, PropertyWrapper> applicationMap = applications.stream()
					.collect(Collectors.toMap(Application::getName, a -> {
						Properties applicationProperties = appNGizer.applicationProperties(name, a.getName());
						return getWrapper(applicationProperties, false);
					}));
			wrapper.setApplications(applicationMap);
			write(name, out, format, wrapper);
			return appNGizer.siteProperties(name);
		}

		/**
		 * Reads an {@link Application}'s {@link Properties} with the given {@link AppNGizerClient} and writes these to
		 * the given {@link OutputStream} using YAML format.
		 * 
		 * @param appNGizer
		 *                       the {@link AppNGizerClient} to use
		 * @param site
		 *                       the {@link Site} where the {@link Application} is installed on
		 * @param app
		 *                       the {@link Application}'s name
		 * @param out
		 *                       the target to write to
		 * @param format
		 *                       the {@link Format} to use
		 * @param nonDefaultOnly
		 *                       write only those properties where the value differs from the default value
		 * 
		 * @return the {@link Application}'s {@link Properties}
		 * 
		 * @throws IOException
		 */
		public static Properties readSiteApplicationProperties(AppNGizerClient appNGizer, String site, String app,
				OutputStream out, Format format, boolean nonDefaultOnly) throws IOException {
			Properties applicationProperties = appNGizer.applicationProperties(site, app);
			LOGGER.info("Read {} properties for site {} with application {}",
					applicationProperties.getProperty().size(), site, app);
			PropertyWrapper wrapper = getWrapper(applicationProperties, nonDefaultOnly);
			write(app, out, format, wrapper);
			return applicationProperties;
		}

		/**
		 * Reads the platform's {@link Properties} with the given {@link AppNGizerClient} and writes these to the given
		 * {@link OutputStream} using YAML format.
		 * 
		 * @param appNGizer
		 *                       the {@link AppNGizerClient} to use
		 * @param out
		 *                       the target to write to
		 * @param format
		 *                       the {@link Format} to use
		 * @param nonDefaultOnly
		 *                       write only those properties where the value differs from the default value
		 * 
		 * @return the platform's {@link Properties}
		 * 
		 * @throws IOException
		 *                     if an error occurred while writing the output
		 */
		public static Properties readPlatformProperties(AppNGizerClient appNGizer, OutputStream out, Format format,
				boolean nonDefaultOnly) throws IOException {
			Properties platformProperties = appNGizer.platformProperties();
			LOGGER.info("Read {} platform properties", platformProperties.getProperty().size());

			PropertyWrapper wrapper = getWrapper(platformProperties, nonDefaultOnly);
			write("appNG", out, format, wrapper);
			return platformProperties;
		}

		private static PropertyWrapper getWrapper(Properties properties, boolean nonDefaultOnly) {
			PropertyWrapper wrapper = new PropertyWrapper();
			Map<String, Property> props = properties.getProperty().stream()
					.filter(p -> nonDefaultOnly ? (!isDefaultOrMultiline(p)) : true)
					.collect(Collectors.toMap(p -> p.getName(), p -> removeUnusedFields(p)));
			wrapper.setProperties(new TreeMap<>(props));
			return wrapper;
		}

		/**
		 * Writes the given {@link PropertyWrapper} to the given {@link OutputStream}
		 * 
		 * @param out
		 *                the stream to write to
		 * @param name
		 *                the name to use
		 * @param format
		 * @param wrapper
		 *                the wrapper to read from
		 * 
		 * @throws IOException
		 *                     if an error occurs while writing
		 */
		public static void write(String name, OutputStream out, Format format, PropertyWrapper wrapper)
				throws IOException {
			Map<String, PropertyWrapper> data = new HashMap<>();
			data.put(name, wrapper);
			ObjectMapper mapper = getObjectMapper(format);
			mapper.setDefaultPropertyInclusion(Include.NON_NULL);
			mapper.writerWithDefaultPrettyPrinter().writeValue(out, data);
		}

		private static <T extends Nameable> T removeUnusedFields(T nameable) {
			nameable.setName(null);
			nameable.setSelf(null);
			nameable.setLinks(null);
			return nameable;
		}

		/**
		 * Writes a {@link Site}'s {@link Properties} defined by the given {@link InputStream} with the given
		 * {@link AppNGizerClient}
		 * 
		 * @param appNGizer
		 *                  the {@link AppNGizerClient} to use
		 * @param in
		 *                  the {@link InputStream} to read from
		 * @param format
		 *                  the {@link Format} to use
		 * 
		 * @return the {@link Site}'s {@link Properties}
		 * 
		 * @throws IOException
		 *                     if an error occurred while reading the input
		 */
		public static Properties writeSiteProperties(AppNGizerClient appNGizer, String site, InputStream in,
				Format format) throws IOException {

			Map<String, SiteConfig> wrappers = readSite(in, format);
			SiteConfig wrapper = wrappers.values().iterator().next();
			preparePropertiesForWrite(wrappers);

			for (Entry<String, Property> propEntry : wrapper.getProperties().entrySet()) {
				appNGizer.updateSiteProperty(site, propEntry.getKey(), propEntry.getValue());
			}

			preparePropertiesForWrite(wrapper.getApplications());
			for (String app : wrapper.getApplications().keySet()) {
				PropertyWrapper appProps = wrapper.getApplications().get(app);
				for (Entry<String, Property> propEntry : appProps.getProperties().entrySet()) {
					appNGizer.updateApplicationProperty(site, app, propEntry.getKey(), propEntry.getValue());
				}
			}
			LOGGER.info("Wrote {} properties for site {}", wrapper.getProperties().size(), site);
			return appNGizer.siteProperties(site);
		}

		/**
		 * Reads a stream and parses it to a map of {@link SiteConfig}s
		 * 
		 * @param in
		 *               the stream to read from
		 * @param format
		 *               the {@link Format} to use
		 * 
		 * @return the map
		 * 
		 * @throws IOException
		 *                     if an error occurred while reading the input
		 */
		public static Map<String, SiteConfig> readSite(InputStream in, Format format) throws IOException {
			return getObjectMapper(format).readValue(in, new TypeReference<HashMap<String, SiteConfig>>() {
			});
		}

		private static ObjectMapper getObjectMapper(Format format) {
			JsonFactory factory = Format.JSON.equals(format) ? new JsonFactory()
					: new YAMLFactory().enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
							.disable(YAMLGenerator.Feature.SPLIT_LINES)
							.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
			return new ObjectMapper(factory).setSerializationInclusion(Include.NON_ABSENT);
		}

		/**
		 * Reads a stream and parses it to a map of {@link PropertyWrapper}s
		 * 
		 * @param in
		 *               the stream to read from
		 * @param format
		 *               the {@link Format} to use
		 * 
		 * @return the map
		 * 
		 * @throws IOException
		 *                     if an error occurred while reading the input
		 */
		public static Map<String, PropertyWrapper> read(InputStream in, Format format) throws IOException {
			return getObjectMapper(format).readValue(in, new TypeReference<HashMap<String, PropertyWrapper>>() {
			});
		}

		/**
		 * Writes the platform's {@link Properties} defined by the given {@link InputStream} with the given
		 * {@link AppNGizerClient}
		 * 
		 * @param appNGizer
		 *                  the {@link AppNGizerClient} to use
		 * @param in
		 *                  the {@link InputStream} to read from
		 * @param format
		 *                  the {@link Format} to use
		 * 
		 * @return the platform's {@link Properties}
		 * 
		 * @throws IOException
		 *                     if an error occurred while reading the input
		 */
		public static Properties writePlatformProperties(AppNGizerClient appNGizer, InputStream in, Format format)
				throws IOException {
			PropertyWrapper wrapper = preparePropertiesForWrite(read(in, format));
			for (Property prop : new TreeMap<>(wrapper.properties).values()) {
				appNGizer.updatePlatformProperty(prop.getName(), prop);
			}
			LOGGER.info("Wrote {} platform properties", wrapper.properties.size());
			return appNGizer.platformProperties();
		}

		/**
		 * Writes an {@link Application}'s {@link Properties} defined by the given {@link InputStream} with the given
		 * {@link AppNGizerClient}
		 * 
		 * @param appNGizer
		 *                  the {@link AppNGizerClient} to use
		 * @param site
		 *                  the {@link Site} where the {@link Application} is installed on
		 * @param app
		 *                  the {@link Application}'s name
		 * @param in
		 *                  the {@link InputStream} to read from
		 * @param format
		 *                  the {@link Format} to use
		 * 
		 * @return the {@link Application}'s {@link Properties}
		 * 
		 * @throws IOException
		 *                     if an error occurred while reading the input
		 */
		public static Properties writeSiteApplicationProperties(AppNGizerClient appNGizer, String site, String app,
				InputStream in, Format format) throws IOException {
			PropertyWrapper wrapper = preparePropertiesForWrite(read(in, format));
			for (Property prop : new TreeMap<>(wrapper.properties).values()) {
				appNGizer.updateApplicationProperty(site, app, prop.getName(), prop);
			}
			LOGGER.info("Wrote {} properties for application {} on site {}", wrapper.properties.size(), app, site);
			return appNGizer.applicationProperties(site, app);
		}

		private static PropertyWrapper preparePropertiesForWrite(Map<String, ? extends PropertyWrapper> wrappers) {
			for (String key : wrappers.keySet()) {
				PropertyWrapper wrapper = wrappers.get(key);
				wrapper.setName(key);
				for (Entry<String, Property> entry : wrapper.getProperties().entrySet()) {
					String name = entry.getKey();
					Property prop = entry.getValue();
					prop.setName(name);
					if (isDefaultOrMultiline(prop)) {
						prop.setValue(null);
					}
				}
			}
			return wrappers.values().iterator().next();
		}

		private static boolean isDefaultOrMultiline(Property prop) {
			return !Boolean.TRUE.equals(prop.isClob()) && Objects.equals(prop.getDefaultValue(), prop.getValue());
		}

	}

	@Data
	class PropertyWrapper {
		private String name;
		private Map<String, Property> properties;
	}

	@Data
	class SiteConfig extends PropertyWrapper {
		private Map<String, PropertyWrapper> applications;
	}

}
