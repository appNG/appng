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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.appngizer.model.xml.Application;
import org.appng.appngizer.model.xml.Applications;
import org.appng.appngizer.model.xml.Database;
import org.appng.appngizer.model.xml.Grants;
import org.appng.appngizer.model.xml.Group;
import org.appng.appngizer.model.xml.Groups;
import org.appng.appngizer.model.xml.Home;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * A client for appNGizer.<br/>
 * Usage:
 * 
 * <pre>
 * AppNGizer appNGizer = new AppNGizer("http://localhost:8080", "TheSecret ");
 * appNGizer.login();
 * </pre>
 * 
 * Check out the <a href=
 * "https://appng.org/appng/docs/current/appngizer/html/appngizer-user-manual.html">appNGizer
 * User Manual</a> for a detailed description of the possible operations.
 * 
 * @author Matthias Müller
 *
 */

@Slf4j
public class AppNGizer implements AppNGizerClient {

	private RestTemplate restTemplate;
	private Map<String, String> cookies = new HashMap<>();
	private String endpoint;
	private String sharedSecret;

	public AppNGizer(String endpoint, String sharedSecret) {
		this.endpoint = endpoint;
		this.sharedSecret = sharedSecret;
		restTemplate = new RestTemplate();
	}

	private <REQ, RES> RES exchange(String path, REQ body, HttpMethod method, Class<RES> returnType) {
		return exchange(path, body, method, getHeaders(false), returnType);
	}

	private <REQ, RES> RES exchange(String path, REQ body, HttpMethod method, HttpHeaders headers,
			Class<RES> returnType) {
		try {
			RequestEntity<REQ> req = new RequestEntity<>(body, headers, method,
					new URI(endpoint + "/appNGizer" + path));
			ResponseEntity<RES> res = restTemplate.exchange(req, returnType);
			setCookies(res);
			return res.getBody();
		} catch (URISyntaxException e) {
			throw new RestClientException("error while calling appNGizer", e);
		}
	}

	protected HttpHeaders getHeaders(boolean acceptAnyType) {
		HttpHeaders headers = new HttpHeaders();
		if (!cookies.isEmpty()) {
			cookies.keySet().forEach(k -> {
				String cookie = cookies.get(k);
				headers.add(HttpHeaders.COOKIE, k + "=" + cookie);
				LOGGER.debug("sent cookie: {}={}", k, cookies.get(k));
			});
		}
		headers.set(HttpHeaders.USER_AGENT, "appNGizer Client");
		return headers;
	}

	protected void setCookies(ResponseEntity<?> entity) {
		List<String> setCookies = entity.getHeaders().get(HttpHeaders.SET_COOKIE);
		if (null != setCookies) {
			for (String c : setCookies) {
				int valueStart = c.indexOf('=');
				String name = c.substring(0, valueStart);
				int end = c.indexOf(';');
				String value = c.substring(valueStart + 1, end < 0 ? c.length() : end);
				cookies.put(name, value);
				LOGGER.debug("received cookie: {}={}", name, value);
			}
		}
	}

	public Home welcome() {
		return get("/", Home.class);
	}

	public Home login() {
		return post("/", sharedSecret, Home.class);
	}

	private <RES> RES get(String path, Class<RES> responseClazz) {
		return exchange(path, null, HttpMethod.GET, responseClazz);
	}

	private void delete(String path) {
		exchange(path, null, HttpMethod.DELETE, Void.class);
	}

	private <REQ, RES> RES post(String path, REQ body, Class<RES> responseClazz) {
		return exchange(path, body, HttpMethod.POST, responseClazz);
	}

	private <REQ, RES> RES put(String path, REQ body, Class<RES> responseClazz) {
		return exchange(path, body, HttpMethod.PUT, responseClazz);
	}

	public Subjects subjects() {
		return get("/subject", Subjects.class);
	}

	public Subject subject(String name) {
		return get("/subject/" + name, Subject.class);
	}

	public Subject createSubject(Subject subject) {
		return post("/subject/", subject, Subject.class);
	}

	public Subject updateSubject(String name, Subject subject) {
		return put("/subject/" + name, subject, Subject.class);
	}

	public void deleteSubject(String name) {
		delete("/subject/" + name);
	}

	public Groups groups() {
		return get("/group", Groups.class);
	}

	public Group group(String name) {
		return get("/group/" + name, Group.class);
	}

	public Group createGroup(Group group) {
		return post("/group/", group, Group.class);
	}

	public Group updateGroup(String name, Group group) {
		return put("/group/" + name, group, Group.class);
	}

	public void deleteGroup(String name) {
		delete("/group/" + name);
	}

	public Applications applications() {
		return get("/application", Applications.class);
	}

	public Application application(String app) {
		return get("/application/" + app, Application.class);
	}

	public Application updateApplication(String app, Application application) {
		return put("/application/" + app, application, Application.class);
	}

	public void deleteApplication(String app) {
		delete("/application/" + app);
	}

	public Properties applicationProperties(String app) {
		return get("/application/" + app + "/property", Properties.class);
	}

	public Property createApplicationProperty(String app, Property property) {
		return post("/application/" + app + "/property", property, Property.class);
	}

	public Property updateApplicationProperty(String app, Property property) {
		return put("/application/" + app + "/property/" + property.getName(), property, Property.class);
	}

	public void deleteApplicationProperty(String app, String name) {
		delete("/application/" + app + "/property/" + name);
	}

	public Roles roles(String app) {
		return get("/application/" + app + "/role", Roles.class);
	}

	public Role role(String app, String name) {
		return get("/application/" + app + "/role/" + name, Role.class);
	}

	public Role createRole(String app, Role role) {
		return post("/application/" + app + "/role", role, Role.class);
	}

	public Role updateRole(String app, String name, Role role) {
		return put("/application/" + app + "/role/" + name, role, Role.class);
	}

	public void deleteRole(String app, String name) {
		delete("/application/" + app + "/role/" + name);
	}

	public Permissions permissions(String app) {
		return get("/application/" + app + "/permission", Permissions.class);
	}

	public Permission permission(String app, String name) {
		return get("/application/" + app + "/permission/" + name, Permission.class);
	}

	public Permission createPermission(String app, Permission permission) {
		return post("/application/" + app + "/permission", permission, Permission.class);
	}

	public Permission updatePermission(String app, String name, Permission permission) {
		return put("/application/" + app + "/permission/" + name, permission, Permission.class);
	}

	public void deletePermission(String app, String name) {
		delete("/application/" + app + "/permission/" + name);
	}

	public Sites sites() {
		return get("/site", Sites.class);
	}

	public Site site(String name) {
		return get("/site/" + name, Site.class);
	}

	public Site createSite(Site site) {
		return post("/site", site, Site.class);
	}

	public Site updateSite(String name, Site site) {
		return put("/site/" + name, site, Site.class);
	}

	public void deleteSite(String name) {
		delete("/site/" + name);
	}

	public void reloadSite(String name) {
		put("/site/" + name + "/reload", null, Void.class);
	}

	public Properties siteProperties(String site) {
		return get("/site/" + site + "/property", Properties.class);
	}

	public Property siteProperty(String site, String name) {
		return get("/site/" + site + "/property/" + name, Property.class);
	}

	public Property createSiteProperty(String site, Property property) {
		return post("/site/" + site + "/property", property, Property.class);
	}

	public Property updateSiteProperty(String site, String name, Property property) {
		return put("/site/" + site + "/property/" + name, property, Property.class);
	}

	public void deleteSiteProperty(String site, String name) {
		delete("/site/" + site + "/property/" + name);
	}

	public Applications applications(String site) {
		return get("/site/" + site + "/application", Applications.class);
	}

	public Application application(String site, String app) {
		return get("/site/" + site + "/application/" + app, Application.class);
	}

	public void activateApplication(String site, String app) {
		post("/site/" + site + "/application/" + app, null, Void.class);
	}

	public void deactivateApplication(String site, String app) {
		delete("/site/" + site + "/application/" + app);
	}

	public Grants siteGrants(String site, String app) {
		return get("/site/" + site + "/application/" + app + "/grants", Grants.class);
	}

	public Grants updateSiteGrants(String site, String app, Grants grants) {
		return put("/site/" + site + "/application/" + app + "/grants", grants, Grants.class);
	}

	public Properties applicationProperties(String site, String app) {
		return get("/site/" + site + "/application/" + app + "/property", Properties.class);
	}

	public Property createApplicationProperty(String site, String app, Property property) {
		return post("/site/" + site + "/application/" + app + "/property", property, Property.class);
	}

	public Property updateApplicationProperty(String site, String app, String name, Property property) {
		return put("/site/" + site + "/application/" + app + "/property/" + name, property, Property.class);
	}

	public void deleteApplicationProperty(String site, String app, String name) {
		delete("/site/" + site + "/application/" + app + "/property/" + name);
	}

	public Repositories repositories() {
		return get("/repository", Repositories.class);
	}

	public Repository repository(String name) {
		return get("/repository/" + name, Repository.class);
	}

	public Repository createRepository(Repository repository) {
		return post("/repository", repository, Repository.class);
	}

	public Repository updateRepository(String name, Repository repository) {
		return put("/repository/" + name, repository, Repository.class);
	}

	public void deleteRepository(String name) {
		delete("/repository/" + name);
	}

	public Package installPackage(String name, Package packageToInstall) {
		return put("/repository/" + name + "/install/" + name, packageToInstall, Package.class);
	}

	public Package uploadPackage(String name, File archive) throws IOException {
		MultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
		multipartRequest.add("file", new FileSystemResource(archive));
		HttpHeaders headers = getHeaders(false);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		return exchange("/repository/" + name + "/upload", multipartRequest, HttpMethod.POST, headers, Package.class);
	}

	public Properties platformProperties() {
		return get("/platform/property", Properties.class);
	}

	public Property platformProperty(String name) {
		return get("/platform/property/" + name, Property.class);
	}

	public Property createPlatformProperty(Property property) {
		return post("/platform/property", property, Property.class);
	}

	public Property updatePlatformProperty(String name, Property property) {
		return put("/platform/property/" + name, property, Property.class);
	}

	public void deletePlatformProperty(String name) {
		delete("/platform/property/" + name);
	}

	public Properties environment() {
		return get("/platform/environment", Properties.class);
	}

	public Properties system() {
		return get("/platform/system", Properties.class);
	}

	public Database database() {
		return get("/platform/database", Database.class);
	}

	public Database initializeDatabase() {
		return post("/platform/database/initialize", null, Database.class);
	}

}
