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
import org.appng.appngizer.model.Permission;
import org.appng.appngizer.model.Role;
import org.appng.appngizer.model.Roles;
import org.appng.appngizer.model.xml.Permissions;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.model.AccessibleApplication;
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
public class RoleController extends ControllerBase {

	@RequestMapping(value = "/application/{app}/role", method = RequestMethod.GET)
	public ResponseEntity<Roles> listRoles(@PathVariable("app") String app) {
		AccessibleApplication appByName = getApplicationByName(app);
		if (null == appByName) {
			return notFound();
		}
		List<Role> roleList = new ArrayList<Role>();
		for (RoleImpl r : getCoreService().getApplicationRolesForApplication(appByName.getId())) {
			roleList.add(Role.fromDomain(r));
		}
		Roles roles = new Roles(roleList, app);
		roles.applyUriComponents(getUriBuilder());
		return ok(roles);
	}

	@RequestMapping(value = "/application/{app}/role/{name}", method = RequestMethod.GET)
	public ResponseEntity<Role> getRole(@PathVariable("app") String app, @PathVariable("name") String name) {
		org.appng.api.model.Role role = getCoreService().getApplicationRoleForApplication(app, name);
		if (null == role) {
			return notFound();
		}
		Role fromDomain = Role.fromDomain(role);
		fromDomain.setPermissions(new Permissions());
		for (org.appng.api.model.Permission p : role.getPermissions()) {
			fromDomain.getPermissions().getPermission().add(Permission.fromDomain(p));
		}
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	@RequestMapping(value = "/application/{app}/role", method = RequestMethod.POST)
	public ResponseEntity<Role> createRole(@PathVariable("app") String app,
			@RequestBody org.appng.appngizer.model.xml.Role role) {
		org.appng.api.model.Role existing = getCoreService().getApplicationRoleForApplication(app, role.getName());
		if (null != existing) {
			return conflict();
		}
		RoleImpl roleImpl = Role.toDomain(role);
		ApplicationImpl applicationByName = getApplicationByName(app);
		roleImpl.setApplication(applicationByName);
		addPermissionsAndSave(app, role, roleImpl);
		return created(getRole(app, role.getName()).getBody());
	}

	@RequestMapping(value = "/application/{app}/role/{name}", method = RequestMethod.PUT)
	public ResponseEntity<Role> updateRole(@PathVariable("app") String app, @PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Role role) {
		org.appng.core.domain.RoleImpl appRole = getCoreService().getApplicationRoleForApplication(app, name);
		if (null == appRole) {
			return notFound();
		}
		addPermissionsAndSave(app, role, appRole);
		return ok(getRole(app, name).getBody());
	}

	protected void addPermissionsAndSave(String app, org.appng.appngizer.model.xml.Role role,
			org.appng.core.domain.RoleImpl appRole) {
		if (null != role.getPermissions()) {
			appRole.setDescription(role.getDescription());
			appRole.getPermissions().clear();
			for (org.appng.appngizer.model.xml.Permission permission : role.getPermissions().getPermission()) {
				org.appng.api.model.Permission p = getCoreService().getPermission(app, permission.getName());
				if (null != p) {
					appRole.getPermissions().add(p);
				} else {
					log.info("no such permission: {}", permission.getName());
				}
			}
		}
		getCoreService().saveRole((RoleImpl) appRole);
	}

	@RequestMapping(value = "/application/{app}/role/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteRole(@PathVariable("app") String app, @PathVariable("name") String name)
			throws BusinessException {
		org.appng.api.model.Role appRole = getCoreService().getApplicationRoleForApplication(app, name);
		if (null == appRole) {
			return notFound();
		}
		getCoreService().deleteRole(appRole);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(getUriBuilder().path("/application/{app}/role").buildAndExpand(app).toUri());
		return noContent(headers);
	}

	Logger logger() {
		return log;
	}
}
