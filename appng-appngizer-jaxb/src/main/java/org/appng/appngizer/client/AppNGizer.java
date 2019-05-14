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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;

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
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

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
 * A client for appNGizer.<br/>
 * Usage:
 * 
 * <pre>
 * AppNGizer appNGizer = AppNGizer.Builder.getInstance("http://localhost:8080");
 * appNGizer.login("TheSecret");
 * </pre>
 * 
 * Check out the <a href=
 * "https://appng.org/appng/docs/current/appngizer/html/appngizer-user-manual.html">appNGizer
 * User Manual</a> for a detailed description of the possible operations.
 * 
 * @author Matthias Müller
 *
 */
@Path("/appNGizer/")
@Consumes("application/xml")
@Produces("application/xml")
public interface AppNGizer {

	class Builder {
		public static AppNGizer getInstance(String host) {
			ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
					.httpEngine(new ApacheHttpClient43Engine()).build();
			return client.target(host).proxy(AppNGizer.class);
		}
	}

	@POST
	Home login(String sharedsecret);

	// subjects
	@GET
	@Path("subject")
	Subjects subjects();

	@GET
	@Path("subject/{name}")
	Subject getSubject(@PathParam("name") String name);

	@POST
	@Path("subject")
	Subject createSubject(Subject subject);

	@PUT
	@Path("subject/{name}")
	Subject updateSubject(@PathParam("name") String name, Subject subject);

	@DELETE
	@Path("subject/{name}")
	void deleteSubject(@PathParam("name") String name);

	// groups
	@GET
	@Path("group")
	Groups groups();

	@GET
	@Path("group/{name}")
	Group getGroup(@PathParam("name") String name);

	@POST
	@Path("group")
	Group createGroup(Group group);

	@PUT
	@Path("group/{name}")
	Group updateGroup(@PathParam("name") String name, Group group);

	@DELETE
	@Path("group/{name}")
	void deleteGroup(@PathParam("name") String name);

	// applications
	@GET
	@Path("application")
	Applications applications();

	@GET
	@Path("application/{app}")
	Application getApplication(@PathParam("app") String app);

	@PUT
	@Path("application/{app}")
	Application updateApplication(@PathParam("app") String app, Application application);

	@DELETE
	@Path("application/{app}")
	void deleteApplication(@PathParam("app") String app);

	// application properties
	@GET
	@Path("application/{app}/property")
	Properties getApplicationProperties(@PathParam("app") String app);

	@POST
	@Path("application/{app}/property")
	Property createApplicationProperty(@PathParam("app") String app, Property property);

	@PUT
	@Path("application/{app}/property/{name}")
	Property updateApplicationProperty(@PathParam("app") String app, Property property);

	@POST
	@Path("application/{app}/property/{name}")
	void deleteApplicationProperty(@PathParam("app") String app, @PathParam("name") String name);

	// roles
	@GET
	@Path("application/{app}/role")
	Roles roles(@PathParam("app") String app);

	@GET
	@Path("application/{app}/role/{name}")
	Role getRole(@PathParam("app") String app, @PathParam("name") String name);

	@POST
	@Path("application/{app}/role")
	Role createRole(@PathParam("app") String app, Role role);

	@PUT
	@Path("application/{app}/role/{name}")
	Role updateRole(@PathParam("app") String app, @PathParam("name") String name, Role role);

	@DELETE
	@Path("application/{app}/role/{name}")
	void deleteRole(@PathParam("app") String app, @PathParam("name") String name);

	// permissions
	@GET
	@Path("application/{app}/permission")
	Permissions permissions(@PathParam("app") String app);

	@GET
	@Path("application/{app}/permission/{name}")
	Permission getPermission(@PathParam("app") String app, @PathParam("name") String name);

	@POST
	@Path("application/{app}/permission")
	Permission createPermission(@PathParam("app") String app, Permission permission);

	@PUT
	@Path("application/{app}/permission/{name}")
	Permission updatePermission(@PathParam("app") String app, @PathParam("name") String name, Permission permission);

	@DELETE
	@Path("application/{app}/permission/{name}")
	void deletePermission(@PathParam("app") String app, @PathParam("name") String name);

	// sites
	@GET
	@Path("site")
	Sites sites();

	@GET
	@Path("site/{name}")
	Site getSite(@PathParam("name") String name);

	@POST
	@Path("site")
	Site createSite(Site site);

	@PUT
	@Path("site/{name}")
	Site updateSite(@PathParam("name") String name, Site site);

	@DELETE
	@Path("site/{name}")
	void deleteSite(@PathParam("name") String name);

	@PUT
	@Path("site/{name}/reload")
	void reloadSite(@PathParam("name") String name);

	// site properties
	@GET
	@Path("site/{site}/property")
	Properties siteProperties(@PathParam("site") String site);

	@POST
	@Path("site/{site}/property")
	Property createSiteProperty(@PathParam("site") String site, Property property);

	@PUT
	@Path("site/{site}/property/{name}")
	Property updateSiteProperty(@PathParam("site") String site, @PathParam("name") String name, Property property);

	@POST
	@Path("site/{site}/property/{name}")
	void deleteSiteProperty(@PathParam("site") String site, @PathParam("name") String name);

	// site applications
	@GET
	@Path("site/{site}/application")
	Applications applications(@PathParam("site") String site);

	@GET
	@Path("site/{site}/application/{app}")
	Application getApplications(@PathParam("site") String site, @PathParam("app") String app);

	@POST
	@Path("site/{site}/application/{app}")
	void activateApplication(@PathParam("site") String site, @PathParam("app") String app);

	@DELETE
	@Path("site/{site}/application/{app}")
	void deactivateApplication(@PathParam("site") String site, @PathParam("app") String app);

	@GET
	@Path("site/{site}/application/{app}/grants")
	Grants getSiteGrants(@PathParam("site") String site, @PathParam("app") String app);

	@PUT
	@Path("site/{site}/application/{app}/grants")
	Grants updateSiteGrants(@PathParam("site") String site, @PathParam("app") String app, Grants grants);

	// site application properties
	@GET
	@Path("site/{site}/application/{app}/property")
	Properties applicationProperties(@PathParam("site") String site, @PathParam("app") String app);

	@POST
	@Path("site/{site}/application/{app}/property")
	Property createApplicationProperty(@PathParam("site") String site, @PathParam("app") String app, Property property);

	@PUT
	@Path("site/{site}/application/{app}/property/{name}")
	Property updateApplicationProperty(@PathParam("site") String site, @PathParam("app") String app, Property property);

	@POST
	@Path("site/{site}/application/{app}/property/{name}")
	void deleteApplicationProperty(@PathParam("site") String site, @PathParam("app") String app,
			@PathParam("name") String name);

	// repositories
	@GET
	@Path("repository")
	Repositories repositories();

	@GET
	@Path("repository/{name}")
	Site getRepository(@PathParam("name") String name);

	@POST
	@Path("repository")
	Site createRepository(Repository repository);

	@PUT
	@Path("repository/{name}")
	Site updateRepository(@PathParam("name") String name, Repository repository);

	@DELETE
	@Path("repository/{name}")
	void deleteRepository(@PathParam("name") String name);

	@PUT
	@Path("repository/{name}/install")
	Package installPackage(@PathParam("name") String name, Package packageToInstall);

	@POST
	@Path("repository/{name}/upload")
	@Consumes("multipart/form-data")
	Package uploadPackage(@PathParam("name") String name, @FormParam("file") File file);

	// platform properties
	@GET
	@Path("platform/property")
	Properties platformProperties();

	@POST
	@Path("platform/property")
	Property createPlatformProperty(Property property);

	@PUT
	@Path("platform/property/{name}")
	Property updatePlatformProperty(@PathParam("name") String name, Property property);

	@POST
	@Path("platform/property/{name}")
	void deletePlatformProperty(@PathParam("name") String name);

	@GET
	@Path("platform/environment")
	Properties environment();

	@GET
	@Path("platform/system")
	Properties system();

	@GET
	@Path("platform/database")
	Database database();

	@POST
	@Path("platform/database")
	Database initializeDatabase();

	@Slf4j
	class Config {

		/**
		 * Reads a {@link Site}'s {@link Properties} with the given {@link AppNGizer}
		 * and writes these to the given {@link OutputStream} using YAML format.
		 * 
		 * @param appNGizer      the {@link AppNGizer} to use
		 * @param name           the name of the {@link Site}
		 * @param out            the target to write to
		 * @param nonDefaultOnly write only those properties where the value differs
		 *                       from the default value
		 * @return the {@link Site}'s {@link Properties}
		 * @throws IOException if an error occurred while writing the output
		 */
		public static Properties readSiteProperties(AppNGizer appNGizer, String name, OutputStream out,
				boolean nonDefaultOnly) throws IOException {
			Properties siteProperties = appNGizer.siteProperties(name);
			LOGGER.info("Read {} properties for site {}", siteProperties.getProperty().size(), name);
			return writeYamlProperties(name, out, siteProperties, nonDefaultOnly);
		}

		/**
		 * Reads an {@link Application}'s {@link Properties} with the given
		 * {@link AppNGizer} and writes these to the given {@link OutputStream} using
		 * YAML format.
		 * 
		 * @param appNGizer      the {@link AppNGizer} to use
		 * @param site           the {@link Site} where the {@link Application} is
		 *                       installed on
		 * @param app            the {@link Application}'s name
		 * @param out            the target to write to
		 * @param nonDefaultOnly write only those properties where the value differs
		 *                       from the default value
		 * @return the {@link Application}'s {@link Properties}
		 * @throws IOException
		 */
		public static Properties readSiteApplicationProperties(AppNGizer appNGizer, String site, String app,
				OutputStream out, boolean nonDefaultOnly) throws IOException {
			Properties applicationProperties = appNGizer.applicationProperties(site, app);
			LOGGER.info("Read {} properties for site {} with application {}",
					applicationProperties.getProperty().size(), site, app);
			return writeYamlProperties(app, out, applicationProperties, nonDefaultOnly);
		}

		/**
		 * Reads the platform's {@link Properties} with the given {@link AppNGizer} and
		 * writes these to the given {@link OutputStream} using YAML format.
		 * 
		 * @param appNGizer      the {@link AppNGizer} to use
		 * @param out            the target to write to
		 * @param nonDefaultOnly write only those properties where the value differs
		 *                       from the default value
		 * @return the platform's {@link Properties}
		 * @throws IOException if an error occurred while writing the output
		 */
		public static Properties readPlatformProperties(AppNGizer appNGizer, OutputStream out, boolean nonDefaultOnly)
				throws IOException {
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
		 * {@link InputStream} with the given {@link AppNGizer}
		 * 
		 * @param appNGizer the {@link AppNGizer} to use
		 * @param in        the {@link InputStream} to read from
		 * @return the {@link Site}'s {@link Properties}
		 * @throws IOException if an error occurred while reading the input
		 */
		public static Properties writeSiteProperties(AppNGizer appNGizer, String site, InputStream in)
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
		 * {@link InputStream} with the given {@link AppNGizer}
		 * 
		 * @param appNGizer the {@link AppNGizer} to use
		 * @param in        the {@link InputStream} to read from
		 * @return the platform's {@link Properties}
		 * @throws IOException if an error occurred while reading the input
		 */
		public static Properties writePlatformProperties(AppNGizer appNGizer, InputStream in) throws IOException {
			PropertyWrapper wrapper = readProperties(in);
			for (Property prop : new TreeMap<>(wrapper.properties).values()) {
				appNGizer.updatePlatformProperty(prop.getName(), prop);
			}
			LOGGER.info("Wrote {} platform properties", wrapper.properties.size());
			return appNGizer.platformProperties();
		}

		/**
		 * Writes an {@link Application}'s {@link Properties} defined by the given
		 * {@link InputStream} with the given {@link AppNGizer}
		 * 
		 * @param appNGizer the {@link AppNGizer} to use
		 * @param site      the {@link Site} where the {@link Application} is installed
		 *                  on
		 * @param app       the {@link Application}'s name
		 * @param in        the {@link InputStream} to read from
		 * @return the {@link Application}'s {@link Properties}
		 * @throws IOException if an error occurred while reading the input
		 */
		public static Properties writeSiteApplicationProperties(AppNGizer appNGizer, String site, String app,
				InputStream in) throws IOException {
			PropertyWrapper wrapper = readProperties(in);
			for (Property prop : new TreeMap<>(wrapper.properties).values()) {
				appNGizer.updateApplicationProperty(site, app, prop);
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
