/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.api.support;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.model.Application;
import org.appng.api.model.Group;
import org.appng.api.model.Role;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldPermissionType;
import org.appng.xml.platform.FieldPermissions;
import org.appng.xml.platform.Permission;
import org.appng.xml.platform.PermissionMode;

import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link PermissionProcessor} implementation.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class DefaultPermissionProcessor implements PermissionProcessor {

	static final String PREFIX_ANONYMOUS = "anonymous";

	private Site site;
	private Application application;

	private Subject subject;

	/**
	 * Creates a new DefaultPermissionProcessor for the given {@link Subject}. The current {@link Subject} uses the
	 * given {@link Application} on the given {@link Site}.
	 * 
	 * @param subject
	 *                    the current {@link Subject} (if any)
	 * @param site
	 *                    the {@link Site} currently used by the {@link Subject}
	 * @param application
	 *                    the {@link Application} currently used by the {@link Subject}
	 */
	public DefaultPermissionProcessor(Subject subject, Site site, Application application) {
		this.site = site;
		this.application = application;
		this.subject = subject;
		LOGGER.debug("created PermissionProcessor for {}", getPrefix());
	}

	private String getPrefix() {
		return (null == subject ? "{[no user]" : ("user '" + subject.getName() + "'")) + " in application '"
				+ application.getName() + "' of site '" + site.getName() + "'";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.PermissionProcessor#hasPermissions(org.appng.api. PermissionOwner)
	 */
	public boolean hasPermissions(PermissionOwner permissionOwner) {
		boolean hasAccess = true;
		Collection<Permission> permissionList = permissionOwner.getPermissions();
		if (permissionList != null) {
			LOGGER.trace("checking permissions for {}", permissionOwner.getName());
			hasAccess = hasPermissions(permissionList);
		} else {
			LOGGER.trace("no permissions given for {}", permissionOwner.getName());
		}
		if (hasAccess) {
			LOGGER.debug("permission granted for {}", permissionOwner.getName());
		} else {
			LOGGER.debug("permission denied for {}", permissionOwner.getName());
		}
		return hasAccess;
	}

	private boolean hasPermissions(Collection<Permission> permissionList) {
		boolean hasAccess = true;
		for (Permission permission : permissionList) {
			if (!permission.getRef().startsWith(PREFIX_ANONYMOUS)) {
				PermissionMode mode = permission.getMode();
				boolean hasPermission = hasPermission(permission);
				permission.setValue(Boolean.toString(hasPermission));
				if (PermissionMode.SET.equals(mode)) {
					hasAccess &= hasPermission;
				} else if (mode == null) {
					permission.setMode(PermissionMode.READ);
				}
			}
		}
		return hasAccess;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.PermissionProcessor#hasPermission(java.lang.String)
	 */
	public boolean hasPermission(String permissionRef) {
		Permission permission = new Permission();
		permission.setRef(permissionRef);
		return hasPermission(permission);
	}

	private boolean hasPermission(Permission permission) {
		if (null == subject) {
			LOGGER.debug("no subject given, so permission '{}' is not present", permission.getRef());
			return false;
		}
		List<Group> groups = subject.getGroups();
		LOGGER.debug("checking permission '{}' for subject '{}'", permission.getRef(), subject.getName());
		if (groups == null || groups.size() == 0) {
			LOGGER.debug("subject '{}' does not belong to any group, thus has no permissions", subject.getName());
		}
		for (Group group : groups) {
			LOGGER.debug("{} belongs to group {}", subject.getName(), group.getName());
			if (0 == group.getRoles().size()) {
				LOGGER.debug("group '{}' does not contain any applicationroles!", group.getName());
			}
			for (Role role : group.getRoles()) {
				Application applicationFromRole = role.getApplication();
				if (null == applicationFromRole) {
					LOGGER.warn("invalid Role#{}, no Application set for role!", role.getId());
					continue;
				}
				LOGGER.debug("'{}' contains role '{}' from application '{}'", group.getName(), role.getName(),
						applicationFromRole.getName());
				if (null != application && application.getName().equals(applicationFromRole.getName())) {
					Set<org.appng.api.model.Permission> permissionsFromRole = role.getPermissions();
					if (0 == permissionsFromRole.size()) {
						LOGGER.debug("role '{}' does not contain any permissions!", role.getName());
					}
					for (org.appng.api.model.Permission p : permissionsFromRole) {
						if (p.getName().equals(permission.getRef())) {
							LOGGER.debug("found required permission '{}'", p.getName());
							return true;
						} else {
							LOGGER.trace("skipping permission '{}', (required '{}')", p.getName(), permission.getRef());
						}
					}
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.PermissionProcessor#hasWritePermission(org.appng.xml.api .FieldDef)
	 */
	public boolean hasWritePermission(FieldDef fieldDefinition) {
		boolean isWriteable = !Boolean.TRUE.toString().equalsIgnoreCase(fieldDefinition.getReadonly());
		return hasPermission(fieldDefinition, FieldPermissionType.WRITE, isWriteable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.PermissionProcessor#hasReadPermission(org.appng.xml.api .FieldDef)
	 */
	public boolean hasReadPermission(FieldDef fieldDefinition) {
		return hasPermission(fieldDefinition, FieldPermissionType.READ, true);
	}

	private boolean hasPermission(FieldDef fieldDefinition, FieldPermissionType type, boolean precondition) {
		List<FieldPermissions> permissions = fieldDefinition.getPermissions();
		if (null != permissions) {
			for (FieldPermissions fieldPermissions : permissions) {
				FieldPermissionType mode = fieldPermissions.getMode();
				if (mode == null || type.equals(mode)) {
					return precondition && hasPermissions(fieldPermissions.getPermission());
				}
			}
		}
		return precondition;
	}

}
