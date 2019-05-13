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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * A client for appNGizer.
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
	Properties getApplicationProperties(@PathParam("site") String site, @PathParam("app") String app);

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
		 * @param appNGizer the {@link AppNGizer} to use
		 * @param name      the name of the {@link Site}
		 * @param out       the target to write to
		 * @return the {@link Site}'s {@link Properties}
		 * @throws IOException if an error occurred while writing the output
		 */
		public static Properties readSiteProperties(AppNGizer appNGizer, String name, OutputStream out)
				throws IOException {
			Site site = appNGizer.getSite(name);
			Properties siteProperties = appNGizer.siteProperties(name);
			LOGGER.info("Read {} properties for site {}", siteProperties.getProperty().size(), name);
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			mapper.setDefaultPropertyInclusion(Include.NON_NULL);
			Map<String, SiteWrapper> data = new HashMap<>();
			SiteWrapper s = new SiteWrapper();
			Map<String, Property> props = siteProperties.getProperty().stream()
					.collect(Collectors.toMap(p -> p.getName(), p -> removeUnusedFields(p)));
			s.setProperties(new TreeMap<>(props));
			s.setSite(removeUnusedFields(site));
			data.put(name, s);
			mapper.writer().writeValue(out, data);
			return siteProperties;
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
		public static Properties writeSiteProperties(AppNGizer appNGizer, InputStream in) throws IOException {
			Map<String, SiteWrapper> site = new ObjectMapper(new YAMLFactory()).readValue(in,
					new TypeReference<HashMap<String, SiteWrapper>>() {
					});

			Entry<String, SiteWrapper> siteEntry = site.entrySet().iterator().next();
			SiteWrapper wrapper = siteEntry.getValue();
			wrapper.setName(siteEntry.getKey());
			for (Entry<String, Property> entry : wrapper.getProperties().entrySet()) {
				String name = entry.getKey();
				Property prop = entry.getValue();
				prop.setName(name);
				appNGizer.updateSiteProperty(wrapper.getName(), name, prop);
			}
			LOGGER.info("Wrote {} properties for site {}", wrapper.getProperties().size(), wrapper.getName());
			return appNGizer.siteProperties(wrapper.getName());
		}
	}

	class SiteWrapper {
		@Getter
		@Setter
		Map<String, Property> properties;
		@Delegate
		@Setter
		org.appng.appngizer.model.xml.Site site = new org.appng.appngizer.model.xml.Site();
	}

}
