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
package org.appng.appngizer.controller;

import java.util.ArrayList;
import java.util.List;

import org.appng.appngizer.model.Properties;
import org.appng.appngizer.model.Property;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.service.PropertySupport;
import org.springframework.http.ResponseEntity;

abstract class PropertyBase extends ControllerBase {

	ResponseEntity<Properties> getProperties(org.appng.api.model.Site site, org.appng.api.model.Application app) {
		List<Property> propsList = new ArrayList<>();
		String siteName = null == site ? null : site.getName();
		String applicationName = null == app ? null : app.getName();
		for (PropertyImpl prop : getCoreService().getPropertiesList(site, app)) {
			propsList.add(Property.fromDomain(prop, site, app));
		}
		Properties properties = new Properties(propsList, siteName, applicationName);
		properties.applyUriComponents(getUriBuilder());
		return ok(properties);
	}

	ResponseEntity<Property> getPropertyResponse(String property, org.appng.api.model.Site site,
			org.appng.api.model.Application app) {
		PropertyImpl propertyImpl = getProperty(property, site, app);
		if (null == propertyImpl) {
			return notFound();
		}
		Property fromDomain = Property.fromDomain(propertyImpl, site, app);
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	ResponseEntity<Property> createProperty(org.appng.appngizer.model.xml.Property property,
			org.appng.api.model.Site site, org.appng.api.model.Application application) {
		PropertyImpl existingProperty = getProperty(property.getName(), site, application);
		if (null != existingProperty) {
			return conflict();
		}
		PropertyImpl propertyImpl = Property.toDomain(property);
		getCoreService().createProperty(site, application, propertyImpl);
		return created(getPropertyResponse(property.getName(), site, application).getBody());
	}

	ResponseEntity<Property> updateProperty(org.appng.appngizer.model.xml.Property property,
			org.appng.api.model.Site site, org.appng.api.model.Application app) {
		PropertyImpl propertyImpl = getProperty(property.getName(), site, app);
		if (null == propertyImpl) {
			return notFound();
		}

		PropertyImpl domainProp = Property.toDomain(property);
		propertyImpl.setString(domainProp.getActualString());
		propertyImpl.setDescription(domainProp.getDescription());
		propertyImpl.setClob(domainProp.getClob());

		getCoreService().saveProperty(propertyImpl);
		Property fromDomain = Property.fromDomain(propertyImpl, site, app);
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	PropertyImpl getProperty(String property, org.appng.api.model.Site site,
			org.appng.api.model.Application application) {
		String prefix = PropertySupport.getPropertyPrefix(site, application);
		return getCoreService().getProperty(prefix + property);
	}

	ResponseEntity<Property> deleteProperty(String property, org.appng.api.model.Site site,
			org.appng.api.model.Application app) {
		PropertyImpl propertyImpl = getProperty(property, site, app);
		if (null == propertyImpl) {
			return notFound();
		}
		getCoreService().deleteProperty(propertyImpl);
		return noContent(null);
	}

}
