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
package org.appng.appngizer.model;

import org.appng.core.domain.PropertyImpl;
import org.appng.core.service.PropertySupport;

public class Property extends org.appng.appngizer.model.xml.Property implements UriAware, Comparable<Property> {

	public static Property fromDomain(org.appng.api.model.Property property, org.appng.api.model.Site site,
			org.appng.api.model.Application application) {
		Property prop = new Property();
		String sitePrefix = PropertySupport.getPropertyPrefix(site, application);
		prop.setName(property.getName().substring(sitePrefix.length()));
		prop.setDescription(property.getDescription());
		if (null == property.getClob() && null != property.getDefaultString()) {
			prop.setDefaultValue(property.getDefaultString());
			prop.setValue(property.getString());
		} else {
			prop.setValue(property.getClob());
			prop.setDefaultValue(null);
			prop.setClob(true);
		}

		if (null == prop.getValue()) {
			prop.setValue(prop.getDefaultValue());
			if (null == prop.getValue()) {
				prop.setValue(property.getClob());
			}
		}
		if (null == site && null == application) {
			prop.setSelf("/platform/property/" + prop.getName());
		} else if (null == application) {
			prop.setSelf("/site/" + site.getName() + "/property/" + prop.getName());
		} else if (null == site) {
			prop.setSelf("/application/" + application.getName() + "/property/" + prop.getName());
		} else {
			prop.setSelf("/site/" + site.getName() + "/application/" + application.getName() + "/property/"
					+ prop.getName());
		}
		return prop;
	}

	public static PropertyImpl toDomain(org.appng.appngizer.model.xml.Property p) {
		PropertyImpl prop = new PropertyImpl();
		prop.setName(p.getName());
		prop.setDescription(p.getDescription());
		if (Boolean.TRUE.equals(p.isClob())) {
			prop.setClob(p.getValue());
			prop.setDefaultString(null);
			prop.setActualString(null);
		} else {
			prop.setDefaultString(p.getDefaultValue());
			prop.setActualString(p.getValue());
		}
		return prop;
	}

	public int compareTo(Property o) {
		return getName().compareTo(o.getName());
	}

}
