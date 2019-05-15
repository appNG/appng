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
package org.appng.appngizer.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
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
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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

	void deleteApplicationProperty(String app,String name);

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
	 * 
	 */
	@Slf4j
	class YamlConfig {

		/**
		 * Reads a {@link Site}'s {@link Properties} with the given
		 * {@link AppNGizerClient} and writes these to the given {@link OutputStream}
		 * using YAML format.
		 * 
		 * @param appNGizer      the {@link AppNGizerClient} to use
		 * @param name           the name of the {@link Site}
		 * @param out            the target to write to
		 * @param nonDefaultOnly write only those properties where the value differs
		 *                       from the default value
		 * @return the {@link Site}'s {@link Properties}
		 * @throws IOException if an error occurred while writing the output
		 */
		public static Properties readSiteProperties(AppNGizerClient appNGizer, String name, OutputStream out,
				boolean nonDefaultOnly) throws IOException {
			Properties siteProperties = appNGizer.siteProperties(name);
			LOGGER.info("Read {} properties for site {}", siteProperties.getProperty().size(), name);
			return writeYamlProperties(name, out, siteProperties, nonDefaultOnly);
		}

		/**
		 * Reads an {@link Application}'s {@link Properties} with the given
		 * {@link AppNGizerClient} and writes these to the given {@link OutputStream}
		 * using YAML format.
		 * 
		 * @param appNGizer      the {@link AppNGizerClient} to use
		 * @param site           the {@link Site} where the {@link Application} is
		 *                       installed on
		 * @param app            the {@link Application}'s name
		 * @param out            the target to write to
		 * @param nonDefaultOnly write only those properties where the value differs
		 *                       from the default value
		 * @return the {@link Application}'s {@link Properties}
		 * @throws IOException
		 */
		public static Properties readSiteApplicationProperties(AppNGizerClient appNGizer, String site, String app,
				OutputStream out, boolean nonDefaultOnly) throws IOException {
			Properties applicationProperties = appNGizer.applicationProperties(site, app);
			LOGGER.info("Read {} properties for site {} with application {}",
					applicationProperties.getProperty().size(), site, app);
			return writeYamlProperties(app, out, applicationProperties, nonDefaultOnly);
		}

		/**
		 * Reads the platform's {@link Properties} with the given
		 * {@link AppNGizerClient} and writes these to the given {@link OutputStream}
		 * using YAML format.
		 * 
		 * @param appNGizer      the {@link AppNGizerClient} to use
		 * @param out            the target to write to
		 * @param nonDefaultOnly write only those properties where the value differs
		 *                       from the default value
		 * @return the platform's {@link Properties}
		 * @throws IOException if an error occurred while writing the output
		 */
		public static Properties readPlatformProperties(AppNGizerClient appNGizer, OutputStream out,
				boolean nonDefaultOnly) throws IOException {
			Properties platformProperties = appNGizer.platformProperties();
			LOGGER.info("Read {} platform properties", platformProperties.getProperty().size());
			return writeYamlProperties("appNG", out, platformProperties, nonDefaultOnly);
		}

		private static Properties writeYamlProperties(String name, OutputStream out, Properties properties,
				boolean nonDefaultOnly) throws IOException, JsonGenerationException, JsonMappingException {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			mapper.setDefaultPropertyInclusion(Include.NON_NULL);
			Map<String, PropertyWrapper> data = new HashMap<>();
			PropertyWrapper wrapper = new PropertyWrapper();
			Map<String, Property> props = properties.getProperty().stream()
					.filter(p -> nonDefaultOnly ? (!isDefaultValue(p)) : true)
					.collect(Collectors.toMap(p -> p.getName(), p -> removeUnusedFields(p)));
			wrapper.setProperties(new TreeMap<>(props));
			data.put(name, wrapper);
			mapper.writer().writeValue(out, data);
			return properties;
		}

		private static <T extends Nameable> T removeUnusedFields(T nameable) {
			nameable.setName(null);
			nameable.setSelf(null);
			nameable.setLinks(null);
			return nameable;
		}

		/**
		 * Writes a {@link Site}'s {@link Properties} defined by the given
		 * {@link InputStream} with the given {@link AppNGizerClient}
		 * 
		 * @param appNGizer the {@link AppNGizerClient} to use
		 * @param in        the {@link InputStream} to read from
		 * @return the {@link Site}'s {@link Properties}
		 * @throws IOException if an error occurred while reading the input
		 */
		public static Properties writeSiteProperties(AppNGizerClient appNGizer, String site, InputStream in)
				throws IOException {
			PropertyWrapper wrapper = readProperties(in);
			for (Property prop : new TreeMap<>(wrapper.properties).values()) {
				appNGizer.updateSiteProperty(site, prop.getName(), prop);
			}
			LOGGER.info("Wrote {} properties for site {}", wrapper.properties.size(), site);
			return appNGizer.siteProperties(site);
		}

		/**
		 * Writes the platform's {@link Properties} defined by the given
		 * {@link InputStream} with the given {@link AppNGizerClient}
		 * 
		 * @param appNGizer the {@link AppNGizerClient} to use
		 * @param in        the {@link InputStream} to read from
		 * @return the platform's {@link Properties}
		 * @throws IOException if an error occurred while reading the input
		 */
		public static Properties writePlatformProperties(AppNGizerClient appNGizer, InputStream in) throws IOException {
			PropertyWrapper wrapper = readProperties(in);
			for (Property prop : new TreeMap<>(wrapper.properties).values()) {
				appNGizer.updatePlatformProperty(prop.getName(), prop);
			}
			LOGGER.info("Wrote {} platform properties", wrapper.properties.size());
			return appNGizer.platformProperties();
		}

		/**
		 * Writes an {@link Application}'s {@link Properties} defined by the given
		 * {@link InputStream} with the given {@link AppNGizerClient}
		 * 
		 * @param appNGizer the {@link AppNGizerClient} to use
		 * @param site      the {@link Site} where the {@link Application} is installed
		 *                  on
		 * @param app       the {@link Application}'s name
		 * @param in        the {@link InputStream} to read from
		 * @return the {@link Application}'s {@link Properties}
		 * @throws IOException if an error occurred while reading the input
		 */
		public static Properties writeSiteApplicationProperties(AppNGizerClient appNGizer, String site, String app,
				InputStream in) throws IOException {
			PropertyWrapper wrapper = readProperties(in);
			for (Property prop : new TreeMap<>(wrapper.properties).values()) {
				appNGizer.updateApplicationProperty(site, app, prop.getName(), prop);
			}
			LOGGER.info("Wrote {} properties for application {} on site {}", wrapper.properties.size(), app, site);
			return appNGizer.applicationProperties(site, app);
		}

		private static PropertyWrapper readProperties(InputStream in)
				throws IOException, JsonParseException, JsonMappingException {
			Map<String, PropertyWrapper> wrappers = new ObjectMapper(new YAMLFactory()).readValue(in,
					new TypeReference<HashMap<String, PropertyWrapper>>() {
					});

			Entry<String, PropertyWrapper> wrapperEntry = wrappers.entrySet().iterator().next();
			PropertyWrapper wrapper = wrapperEntry.getValue();
			wrapper.setName(wrapperEntry.getKey());
			for (Entry<String, Property> entry : wrapper.getProperties().entrySet()) {
				String name = entry.getKey();
				Property prop = entry.getValue();
				prop.setName(name);
				if (isDefaultValue(prop)) {
					prop.setValue(null);
				}
			}
			return wrapper;
		}

		private static boolean isDefaultValue(Property prop) {
			return !Boolean.TRUE.equals(prop.isClob()) && Objects.equals(prop.getDefaultValue(), prop.getValue());
		}

	}

	@Data
	class PropertyWrapper {
		private String name;
		private Map<String, Property> properties;
	}

}
