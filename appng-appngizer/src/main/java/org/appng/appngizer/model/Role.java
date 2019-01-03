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
package org.appng.appngizer.model;

import org.appng.core.domain.RoleImpl;
import org.springframework.web.util.UriComponentsBuilder;

public class Role extends org.appng.appngizer.model.xml.Role implements UriAware {

	public static RoleImpl toDomain(org.appng.appngizer.model.xml.Role r) {
		RoleImpl roleImpl = new RoleImpl();
		roleImpl.setName(r.getName());
		roleImpl.setDescription(r.getDescription());
		return roleImpl;
	}

	public static Role fromDomain(org.appng.api.model.Role roleImpl) {
		Role role = new Role();
		role.setApplication(roleImpl.getApplication().getName());
		role.setName(roleImpl.getName());
		role.setDescription(roleImpl.getDescription());
		return role;
	}

	@Override
	public void applyUriComponents(UriComponentsBuilder builder) {
		if (null != permissions) {
			for (org.appng.appngizer.model.xml.Permission p : permissions.getPermission()) {
				((UriAware) p).applyUriComponents(builder.cloneBuilder());
			}
		}

		String uriString = builder.path("/application/{application}/role/{name}")
				.buildAndExpand(application, encode(name)).toUriString();

		setSelf(uriString);
	}
}