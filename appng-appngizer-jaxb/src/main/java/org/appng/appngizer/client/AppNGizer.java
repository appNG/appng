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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.appng.appngizer.model.xml.Application;
import org.appng.appngizer.model.xml.Applications;
import org.appng.appngizer.model.xml.Database;
import org.appng.appngizer.model.xml.Grants;
import org.appng.appngizer.model.xml.Group;
import org.appng.appngizer.model.xml.Groups;
import org.appng.appngizer.model.xml.Home;
import org.appng.appngizer.model.xml.Package;
import org.appng.appngizer.model.xml.Packages;
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
import org.springframework.web.util.UriUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * A client for appNGizer.<br/>
 * Usage:
 * 
 * <pre>
 * AppNGizer appNGizer = new AppNGizer("http://localhost:8080", "TheSecret");
 * appNGizer.login();
 * </pre>
 * 
 * Check out the <a href= "https://appng.org/appng/docs/current/appngizer/html/appngizer-user-manual.html">appNGizer
 * User Manual</a> for a detailed description of the possible operations.
 * 
 * @author Matthias Müller
 */

@Slf4j
public class AppNGizer implements AppNGizerClient {

	private RestTemplate restTemplate;
	private Map<String, String> cookies = new HashMap<>();
	private String endpoint;
	private String sharedSecret;
	private static String userAgent = "appNGizer Client";

	static {
		URL url = AppNGizer.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
		if (null != url) {
			try (InputStream in = url.openStream()) {
				String version = new Manifest(in).getMainAttributes().getValue("Implementation-Version");
				userAgent += " (" + version + ")";
			} catch (IOException e) {
				LOGGER.warn("Error reading MANIFEST.MF", e);
			}
		}
	}

	public AppNGizer(String endpoint, String sharedSecret) {
		this(endpoint, sharedSecret, new RestTemplate());
	}

	public AppNGizer(String endpoint, String sharedSecret, RestTemplate restTemplate) {
		this.endpoint = endpoint;
		this.sharedSecret = sharedSecret;
		this.restTemplate = restTemplate;
		this.restTemplate.getMessageConverters().add(new VoidResponseHttpMessageConverter());
	}

	private <REQ, RES> RES exchange(String path, REQ body, HttpMethod method, Class<RES> returnType) {
		return exchange(path, body, method, getHeaders(), returnType);
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

	protected HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		if (!cookies.isEmpty()) {
			cookies.keySet().forEach(k -> {
				String cookie = cookies.get(k);
				headers.add(HttpHeaders.COOKIE, k + "=" + cookie);
				LOGGER.debug("sent cookie: {}={}", k, cookies.get(k));
			});
		}
		headers.set(HttpHeaders.USER_AGENT, userAgent);
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
		return get("/subject/" + encode(name), Subject.class);
	}

	public Subject createSubject(Subject subject) {
		return post("/subject/", subject, Subject.class);
	}

	public Subject updateSubject(String name, Subject subject) {
		return put("/subject/" + encode(name), subject, Subject.class);
	}

	public void deleteSubject(String name) {
		delete("/subject/" + encode(name));
	}

	public Groups groups() {
		return get("/group", Groups.class);
	}

	public Group group(String name) {
		return get("/group/" + encode(name), Group.class);
	}

	public Group createGroup(Group group) {
		return post("/group/", group, Group.class);
	}

	public Group updateGroup(String name, Group group) {
		return put("/group/" + encode(name), group, Group.class);
	}

	public void deleteGroup(String name) {
		delete("/group/" + encode(name));
	}

	public Applications applications() {
		return get("/application", Applications.class);
	}

	public Application application(String app) {
		return get("/application/" + encode(app), Application.class);
	}

	public Application updateApplication(String app, Application application) {
		return put("/application/" + encode(app), application, Application.class);
	}

	public void deleteApplication(String app) {
		delete("/application/" + encode(app));
	}

	public Properties applicationProperties(String app) {
		return get("/application/" + encode(app) + "/property", Properties.class);
	}

	public Property createApplicationProperty(String app, Property property) {
		return post("/application/" + encode(app) + "/property", property, Property.class);
	}

	public Property updateApplicationProperty(String app, Property property) {
		return put("/application/" + encode(app) + "/property/" + property.getName(), property, Property.class);
	}

	public void deleteApplicationProperty(String app, String name) {
		delete("/application/" + encode(app) + "/property/" + encode(name));
	}

	public Roles roles(String app) {
		return get("/application/" + encode(app) + "/role", Roles.class);
	}

	public Role role(String app, String name) {
		return get("/application/" + encode(app) + "/role/" + encode(name), Role.class);
	}

	public Role createRole(String app, Role role) {
		return post("/application/" + encode(app) + "/role", role, Role.class);
	}

	public Role updateRole(String app, String name, Role role) {
		return put("/application/" + encode(app) + "/role/" + encode(name), role, Role.class);
	}

	public void deleteRole(String app, String name) {
		delete("/application/" + encode(app) + "/role/" + encode(name));
	}

	public Permissions permissions(String app) {
		return get("/application/" + encode(app) + "/permission", Permissions.class);
	}

	public Permission permission(String app, String name) {
		return get("/application/" + encode(app) + "/permission/" + encode(name), Permission.class);
	}

	public Permission createPermission(String app, Permission permission) {
		return post("/application/" + encode(app) + "/permission", permission, Permission.class);
	}

	public Permission updatePermission(String app, String name, Permission permission) {
		return put("/application/" + encode(app) + "/permission/" + encode(name), permission, Permission.class);
	}

	public void deletePermission(String app, String name) {
		delete("/application/" + encode(app) + "/permission/" + encode(name));
	}

	public Sites sites() {
		return get("/site", Sites.class);
	}

	public Site site(String name) {
		return get("/site/" + encode(name), Site.class);
	}

	public Site createSite(Site site) {
		return post("/site", site, Site.class);
	}

	public Site updateSite(String name, Site site) {
		return put("/site/" + encode(name), site, Site.class);
	}

	public void deleteSite(String name) {
		delete("/site/" + encode(name));
	}

	public void reloadSite(String name) {
		put("/site/" + encode(name) + "/reload", null, Void.class);
	}

	public Properties siteProperties(String site) {
		return get("/site/" + encode(site) + "/property", Properties.class);
	}

	public Property siteProperty(String site, String name) {
		return get("/site/" + encode(site) + "/property/" + encode(name), Property.class);
	}

	public Property createSiteProperty(String site, Property property) {
		return post("/site/" + encode(site) + "/property", property, Property.class);
	}

	public Property updateSiteProperty(String site, String name, Property property) {
		return put("/site/" + encode(site) + "/property/" + encode(name), property, Property.class);
	}

	public void deleteSiteProperty(String site, String name) {
		delete("/site/" + encode(site) + "/property/" + encode(name));
	}

	public Applications applications(String site) {
		return get("/site/" + encode(site) + "/application", Applications.class);
	}

	public Application application(String site, String app) {
		return get("/site/" + encode(site) + "/application/" + encode(app), Application.class);
	}

	public void activateApplication(String site, String app) {
		post("/site/" + encode(site) + "/application/" + encode(app), null, Void.class);
	}

	public void deactivateApplication(String site, String app) {
		delete("/site/" + encode(site) + "/application/" + encode(app));
	}

	public Grants siteGrants(String site, String app) {
		return get("/site/" + encode(site) + "/application/" + encode(app) + "/grants", Grants.class);
	}

	public Grants updateSiteGrants(String site, String app, Grants grants) {
		return put("/site/" + encode(site) + "/application/" + encode(app) + "/grants", grants, Grants.class);
	}

	public Properties applicationProperties(String site, String app) {
		return get("/site/" + encode(site) + "/application/" + encode(app) + "/property", Properties.class);
	}

	public Property createApplicationProperty(String site, String app, Property property) {
		return post("/site/" + encode(site) + "/application/" + encode(app) + "/property", property, Property.class);
	}

	public Property updateApplicationProperty(String site, String app, String name, Property property) {
		return put("/site/" + encode(site) + "/application/" + encode(app) + "/property/" + encode(name), property,
				Property.class);
	}

	public void deleteApplicationProperty(String site, String app, String name) {
		delete("/site/" + encode(site) + "/application/" + encode(app) + "/property/" + encode(name));
	}

	public Repositories repositories() {
		return get("/repository", Repositories.class);
	}

	public Repository repository(String name) {
		return get("/repository/" + encode(name), Repository.class);
	}

	public Repository createRepository(Repository repository) {
		return post("/repository", repository, Repository.class);
	}

	public Repository updateRepository(String name, Repository repository) {
		return put("/repository/" + encode(name), repository, Repository.class);
	}

	public void deleteRepository(String name) {
		delete("/repository/" + encode(name));
	}

	public Package installPackage(String name, Package packageToInstall) {
		return put("/repository/" + encode(name) + "/install/", packageToInstall, Package.class);
	}

	public Package getPackage(String name, String packageName, String version, String timeStamp) {
		return get("/repository/" + encode(name) + "/" + packageName + "/" + version + "/" + timeStamp, Package.class);
	}

	public Packages getPackages(String name, String packageName) {
		return get("/repository/" + encode(name) + "/" + packageName, Packages.class);
	}

	public Package uploadPackage(String name, File archive) throws IOException {
		MultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
		multipartRequest.add("file", new FileSystemResource(archive));
		HttpHeaders headers = getHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		return exchange("/repository/" + encode(name) + "/upload", multipartRequest, HttpMethod.POST, headers,
				Package.class);
	}

	public Properties platformProperties() {
		return get("/platform/property", Properties.class);
	}

	public Property platformProperty(String name) {
		return get("/platform/property/" + encode(name), Property.class);
	}

	public Property createPlatformProperty(Property property) {
		return post("/platform/property", property, Property.class);
	}

	public Property updatePlatformProperty(String name, Property property) {
		return put("/platform/property/" + encode(name), property, Property.class);
	}

	public void deletePlatformProperty(String name) {
		delete("/platform/property/" + encode(name));
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

	static String encode(String segment) {
		return UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8.name());
	}

}
