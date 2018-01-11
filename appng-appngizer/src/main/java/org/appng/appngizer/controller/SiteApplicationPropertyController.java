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
package org.appng.appngizer.controller;

import org.appng.api.model.Application;
import org.appng.appngizer.model.Properties;
import org.appng.appngizer.model.Property;
import org.appng.core.domain.SiteImpl;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SiteApplicationPropertyController extends PropertyBase {

	@RequestMapping(value = "/site/{site}/application/{app}/property", method = RequestMethod.GET)
	public ResponseEntity<Properties> listProperties(@PathVariable("site") String site,
			@PathVariable("app") String app) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		Application appByName = siteByName.getApplication(app);
		if (null == appByName) {
			return notFound();
		}
		return getProperties(siteByName, appByName);
	}

	@RequestMapping(value = "/site/{site}/application/{app}/property/{prop}", method = RequestMethod.GET)
	public ResponseEntity<Property> getProperty(@PathVariable("site") String site, @PathVariable("app") String app,
			@PathVariable("prop") String prop) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		Application appByName = siteByName.getApplication(app);
		if (null == appByName) {
			return notFound();
		}
		return getPropertyResponse(prop, siteByName, appByName);
	}

	@RequestMapping(value = "/site/{site}/application/{app}/property", method = RequestMethod.POST)
	public ResponseEntity<Property> createProperty(@PathVariable("site") String site, @PathVariable("app") String app,
			@RequestBody org.appng.appngizer.model.xml.Property property) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		Application appByName = siteByName.getApplication(app);
		if (null == appByName) {
			return notFound();
		}
		return createProperty(property, siteByName, appByName);
	}

	@RequestMapping(value = "/site/{site}/application/{app}/property/{prop}", method = RequestMethod.PUT)
	public ResponseEntity<Property> updateProperty(@PathVariable("site") String site, @PathVariable("app") String app,
			@RequestBody org.appng.appngizer.model.xml.Property property) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		Application appByName = siteByName.getApplication(app);
		if (null == appByName) {
			return notFound();
		}
		return updateProperty(property, siteByName, appByName);
	}

	@RequestMapping(value = "/site/{site}/application/{app}/property/{prop}", method = RequestMethod.DELETE)
	public ResponseEntity<Property> deleteProperty(@PathVariable("site") String site, @PathVariable("app") String app,
			@PathVariable("prop") String property) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		Application appByName = siteByName.getApplication(app);
		if (null == appByName) {
			return notFound();
		}
		return deleteProperty(property, siteByName, appByName);
	}

	Logger logger() {
		return log;
	}
}
