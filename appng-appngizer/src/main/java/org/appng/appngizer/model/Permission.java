/*
 * Copyright 2011-2018 the original author or authors.
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

import org.appng.core.domain.PermissionImpl;

public class Permission extends org.appng.appngizer.model.xml.Permission implements UriAware {

	public static PermissionImpl toDomain(org.appng.appngizer.model.xml.Permission p) {
		PermissionImpl permImpl = new PermissionImpl();
		permImpl.setName(p.getName());
		permImpl.setDescription(p.getDescription());
		return permImpl;
	}

	public static Permission fromDomain(org.appng.api.model.Permission permImpl) {
		Permission perm = new Permission();
		perm.setName(permImpl.getName());
		perm.setDescription(permImpl.getDescription());
		perm.setApplication(permImpl.getApplication().getName());
		perm.setSelf("/application/" + permImpl.getApplication().getName() + "/permission/" + permImpl.getName());
		return perm;
	}

}