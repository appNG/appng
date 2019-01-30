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
package org.appng.appngizer.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.appng.appngizer.model.Permission;
import org.appng.appngizer.model.Permissions;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.model.AccessibleApplication;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PermissionController extends ControllerBase {

	@RequestMapping(value = "/application/{app}/permission", method = RequestMethod.GET)
	public ResponseEntity<Permissions> listPermissions(@PathVariable("app") String app) {
		AccessibleApplication appByName = getApplicationByName(app);
		if (null == appByName) {
			return notFound();
		}
		List<Permission> permissionList = new ArrayList<>();
		List<? extends org.appng.api.model.Permission> permissionsForApplication = getCoreService()
				.getPermissionsForApplication(appByName.getId());
		for (org.appng.api.model.Permission p : permissionsForApplication) {
			permissionList.add(Permission.fromDomain(p));
		}
		Permissions permissions = new Permissions(permissionList, app);
		permissions.applyUriComponents(getUriBuilder());
		return ok(permissions);
	}

	@RequestMapping(value = "/application/{app}/permission/{name}", method = RequestMethod.GET)
	public ResponseEntity<Permission> getPermission(@PathVariable("app") String app,
			@PathVariable("name") String name) {
		AccessibleApplication appByName = getApplicationByName(app);
		if (null == appByName) {
			return notFound();
		}
		org.appng.api.model.Permission permission = getCoreService().getPermission(app, name);
		if (null == permission) {
			return notFound();
		}
		Permission fromDomain = Permission.fromDomain(permission);
		fromDomain.setApplication(app);
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	@RequestMapping(value = "/application/{app}/permission", method = RequestMethod.POST)
	public ResponseEntity<Permission> createPermission(@PathVariable("app") String app,
			@RequestBody org.appng.appngizer.model.xml.Permission permission) {
		ApplicationImpl appByName = getApplicationByName(app);
		if (null == appByName) {
			return notFound();
		}
		org.appng.api.model.Permission existingPermission = getCoreService().getPermission(app, permission.getName());
		if (null != existingPermission) {
			return conflict();
		}
		PermissionImpl newPermission = Permission.toDomain(permission);
		newPermission.setApplication(appByName);
		getCoreService().savePermission(newPermission);
		getCoreService().getPermission(app, permission.getName());
		return ok(Permission.fromDomain(newPermission));
	}

	@RequestMapping(value = "/application/{app}/permission/{name}", method = RequestMethod.PUT)
	public ResponseEntity<Permission> updatePermission(@PathVariable("app") String app,
			@PathVariable("name") String name, @RequestBody org.appng.appngizer.model.xml.Permission permission) {
		boolean nameChanged = nameChanged(permission, name);
		if (nameChanged) {
			PermissionImpl existingPermission = getCoreService().getPermission(app, permission.getName());
			if (null != existingPermission) {
				return conflict();
			}
		}
		PermissionImpl currentPermission = getCoreService().getPermission(app, name);
		if (null == currentPermission) {
			return notFound();
		}
		currentPermission.setName(permission.getName());
		currentPermission.setDescription(permission.getDescription());
		getCoreService().savePermission(currentPermission);
		Permission fromDomain = Permission.fromDomain(currentPermission);
		if (nameChanged) {
			URI uri = getUriBuilder().path(fromDomain.getSelf()).build().toUri();
			return seeOther(uri);
		}
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	@RequestMapping(value = "/application/{app}/permission/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deletePermission(@PathVariable("app") String app, @PathVariable("name") String name) {
		org.appng.api.model.Permission existingPermission = getCoreService().getPermission(app, name);
		if (null == existingPermission) {
			return notFound();
		}
		getCoreService().deletePermission(existingPermission);
		return noContent(null);
	}

	Logger logger() {
		return LOGGER;
	}
}
