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
package org.appng.appngizer.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.appng.appngizer.model.Group;
import org.appng.appngizer.model.Groups;
import org.appng.appngizer.model.Role;
import org.appng.appngizer.model.Roles;
import org.appng.core.domain.GroupImpl;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
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
public class GroupController extends ControllerBase {

	@GetMapping(value = "/group")
	public ResponseEntity<Groups> listGroups() {
		List<Group> groupList = new ArrayList<>();
		for (org.appng.api.model.Group g : getCoreService().getGroups()) {
			groupList.add(Group.fromDomain(g));
		}
		Groups groups = new Groups(groupList);
		groups.applyUriComponents(getUriBuilder());
		return ok(groups);
	}

	@GetMapping(value = "/group/{name:.+}")
	public ResponseEntity<Group> getGroup(@PathVariable("name") String name) {
		GroupImpl group = getCoreService().getGroupByName(name, true);
		if (null == group) {
			return notFound();
		}
		Group fromDomain = Group.fromDomain(group);
		addRoles(group, fromDomain);
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	public void addRoles(GroupImpl group, Group fromDomain) {
		fromDomain.setRoles(new Roles());
		for (org.appng.api.model.Role r : group.getRoles()) {
			fromDomain.getRoles().getRole().add(Role.fromDomain(r));
		}
	}

	@PostMapping(value = "/group")
	public ResponseEntity<Group> createGroup(@RequestBody org.appng.appngizer.model.xml.Group group) {
		GroupImpl currentGroup = getCoreService().getGroupByName(group.getName());
		if (null != currentGroup) {
			return conflict();
		}
		GroupImpl groupImpl = Group.toDomain(group);
		updateRoles(group.getRoles(), groupImpl);
		getCoreService().createGroup(groupImpl);
		return created(getGroup(group.getName()).getBody());
	}

	@PutMapping(value = "/group/{name:.+}")
	public ResponseEntity<Group> updateGroup(@PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Group group) {
		boolean nameChanged = nameChanged(group, name);
		if (nameChanged) {
			GroupImpl existingGroup = getCoreService().getGroupByName(group.getName(), false);
			if (null != existingGroup) {
				return conflict();
			}
		}
		GroupImpl currentGroup = getCoreService().getGroupByName(name, true);
		if (null == currentGroup) {
			return notFound();
		}
		currentGroup.setName(group.getName());
		currentGroup.setDescription(group.getDescription());
		updateRoles(group.getRoles(), currentGroup);
		getCoreService().updateGroup(currentGroup);
		Group fromDomain = Group.fromDomain(currentGroup);
		if (nameChanged) {
			URI uri = getUriBuilder().path(fromDomain.getSelf()).build().toUri();
			return seeOther(uri);
		}
		addRoles(currentGroup, fromDomain);
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	private void updateRoles(org.appng.appngizer.model.xml.Roles roles, GroupImpl group) {
		if (null != roles) {
			group.getRoles().clear();
			if (null != roles.getRole()) {
				for (org.appng.appngizer.model.xml.Role r : roles.getRole()) {
					org.appng.api.model.Role role = getCoreService()
							.getApplicationRoleForApplication(r.getApplication(), r.getName());
					if (null != role) {
						group.getRoles().add(role);
					}
				}
			}
		}
	}

	@DeleteMapping(value = "/group/{name:.+}")
	public ResponseEntity<Void> deleteGroup(@PathVariable("name") String name) {
		GroupImpl currentGroup = getCoreService().getGroupByName(name);
		if (null == currentGroup) {
			return notFound();
		} else {
			if (currentGroup.isDefaultAdmin()) {
				return conflict();
			}
			getCoreService().deleteGroup(currentGroup);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(getUriBuilder().path("/group").build().toUri());
		return noContent(headers);
	}

	Logger logger() {
		return LOGGER;
	}

}
