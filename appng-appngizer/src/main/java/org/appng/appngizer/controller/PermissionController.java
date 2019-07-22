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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PermissionController extends ControllerBase {

	@GetMapping(value = "/application/{app}/permission")
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

	@GetMapping(value = "/application/{app}/permission/{name}")
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

	@PostMapping(value = "/application/{app}/permission")
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

	@PutMapping(value = "/application/{app}/permission/{name}")
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

	@DeleteMapping(value = "/application/{app}/permission/{name}")
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
