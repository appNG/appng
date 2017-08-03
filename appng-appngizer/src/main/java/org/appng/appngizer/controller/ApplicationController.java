/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.appngizer.controller;

import java.util.ArrayList;
import java.util.List;

import org.appng.api.BusinessException;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.appngizer.model.Application;
import org.appng.appngizer.model.Applications;
import org.appng.appngizer.model.Properties;
import org.appng.appngizer.model.Property;
import org.appng.core.domain.ApplicationImpl;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ApplicationController extends PropertyBase {

	@RequestMapping(value = "/application", method = RequestMethod.GET)
	public ResponseEntity<Applications> listApplications() {
		List<Application> appList = new ArrayList<Application>();
		for (ApplicationImpl application : getCoreService().getApplications()) {
			appList.add(Application.fromDomain(application));
		}
		Applications applications = new Applications(appList);
		applications.applyUriComponents(getUriBuilder());
		return ok(applications);
	}

	@RequestMapping(value = "/application/{name}", method = RequestMethod.GET)
	public ResponseEntity<Application> getApplication(@PathVariable("name") String name) {
		ApplicationImpl application = getApplicationByName(name);
		if (null == application) {
			return notFound();
		}
		Application fromDomain = Application.fromDomain(application);
		fromDomain.addLinks();
		fromDomain.applyUriComponents(getUriBuilder());
		// List<Role> roles = new ArrayList<Role>();
		// fromDomain.setRoles(new Roles(roles, name));
		// List<Permission> permissions = new ArrayList<Permission>();
		// fromDomain.setPermissions(new Permissions(permissions, name));
		// for (org.appng.api.model.Role r : application.getRoles()) {
		// roles.add(Role.fromDomain(r));
		// }
		// for (org.appng.api.model.Permission p : application.getPermissions()) {
		// permissions.add(Permission.fromDomain(p));
		// }
		return ok(fromDomain);
	}

	@RequestMapping(value = "/application/{name}", method = RequestMethod.PUT)
	public ResponseEntity<Application> updateApplication(@PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Application application) throws BusinessException {
		ApplicationImpl applicationByName = getApplicationByName(name);
		if (null == applicationByName) {
			return notFound();
		}
		applicationByName.setHidden(application.isHidden());
		applicationByName.setPrivileged(application.isCore());
		applicationByName.setDisplayName(application.getDisplayName());
		getCoreService().updateApplication(applicationByName, application.isFileBased());
		return ok(Application.fromDomain(applicationByName));
	}

	@RequestMapping(value = "/application/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteApplication(@PathVariable("name") String name) throws BusinessException {
		ApplicationImpl currentApplication = getApplicationByName(name);
		if (null == currentApplication) {
			return notFound();
		}
		getCoreService().deleteApplication(name, new FieldProcessorImpl("delete-application"));
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(getUriBuilder().path("/application").build().toUri());
		return noContent(headers);
	}

	/* Properties */
	@RequestMapping(value = "/application/{name}/property", method = RequestMethod.GET)
	public ResponseEntity<Properties> listProperties(@PathVariable("name") String name) {
		return getProperties(null, getApplicationByName(name));
	}

	@RequestMapping(value = "/application/{name}/property/{prop}", method = RequestMethod.GET)

	public ResponseEntity<Property> getProperty(@PathVariable("name") String name, @PathVariable("prop") String prop) {
		return getPropertyResponse(prop, null, getApplicationByName(name));
	}

	@RequestMapping(value = "/application/{name}/property", method = RequestMethod.POST)
	public ResponseEntity<Property> createProperty(@PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Property property) {
		return createProperty(property, null, getApplicationByName(name));
	}

	@RequestMapping(value = "/application/{name}/property/{prop}", method = RequestMethod.PUT)
	public ResponseEntity<Property> updateProperty(@PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Property property) {
		return updateProperty(property, null, getApplicationByName(name));
	}

	@RequestMapping(value = "/application/{name}/property/{prop}", method = RequestMethod.DELETE)
	public ResponseEntity<Property> deleteProperty(@PathVariable("name") String name,
			@PathVariable("prop") String property) {
		return deleteProperty(property, null, getApplicationByName(name));
	}

	Logger logger() {
		return log;
	}
}
