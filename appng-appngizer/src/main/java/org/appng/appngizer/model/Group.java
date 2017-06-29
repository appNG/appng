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
package org.appng.appngizer.model;

import org.appng.core.domain.GroupImpl;

public class Group extends org.appng.appngizer.model.xml.Group implements UriAware {

	public static GroupImpl toDomain(org.appng.appngizer.model.xml.Group g) {
		GroupImpl groupImpl = new GroupImpl();
		groupImpl.setName(g.getName());
		groupImpl.setDescription(g.getDescription());
		return groupImpl;
	}

	public static Group fromDomain(org.appng.api.model.Group g) {
		Group group = new Group();
		group.setName(g.getName());
		group.setDescription(g.getDescription());
		group.setSelf("/group/" + g.getName());
		return group;
	}

}