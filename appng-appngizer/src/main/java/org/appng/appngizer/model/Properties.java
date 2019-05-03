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

import java.util.List;

import org.springframework.web.util.UriComponentsBuilder;

public class Properties extends org.appng.appngizer.model.xml.Properties implements UriAware {

	public Properties(List<Property> properties, String site, String app) {
		getProperty().addAll(properties);
		if (null == site && null == app) {
			setSelf("/platform/property");
		} else if (null == app) {
			setSelf("/site/" + site + "/property");
		} else if (null == site) {
			setSelf("/application/" + app + "/property");
		} else {
			setSelf("/site/" + site + "/application/" + app + "/property");
		}
	}

	public Properties() {
	}

	@Override
	public void applyUriComponents(UriComponentsBuilder builder) {
		for (org.appng.appngizer.model.xml.Property property : property) {
			((UriAware) property).applyUriComponents(builder.cloneBuilder());
		}
		UriAware.super.applyUriComponents(builder);
	}
}
