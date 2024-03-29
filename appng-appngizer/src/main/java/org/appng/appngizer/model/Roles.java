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
package org.appng.appngizer.model;

import java.util.List;

import org.springframework.web.util.UriComponentsBuilder;

public class Roles extends org.appng.appngizer.model.xml.Roles implements UriAware {

	public Roles() {
		super();
	}

	public Roles(List<Role> roles, String app) {
		this.getRole().addAll(roles);
		setSelf("/application/" + app + "/role");
	}

	@Override
	public void applyUriComponents(UriComponentsBuilder builder) {
		if (null != self) {
			UriAware.super.applyUriComponents(builder.cloneBuilder());
		}
		if (null != role) {
			for (org.appng.appngizer.model.xml.Role role : role) {
				((UriAware) role).applyUriComponents(builder.cloneBuilder());
			}
		}
	}
}