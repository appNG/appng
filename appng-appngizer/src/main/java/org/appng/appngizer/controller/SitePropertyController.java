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
import org.appng.core.domain.SiteImpl;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
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
public class SitePropertyController extends PropertyBase {

	@GetMapping(value = { "/site/{site}/property", "/site/{site}/properties" })
	public ResponseEntity<Properties> listProperties(@PathVariable("site") String site) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		return getProperties(siteByName, null);
	}

	@PutMapping(value = "/site/{site}/properties")
	public ResponseEntity<Properties> updateProperties(@PathVariable("site") String site,
			@RequestBody org.appng.appngizer.model.xml.Properties properties) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}

		List<Property> propsList = new ArrayList<>();
		for (org.appng.appngizer.model.xml.Property property : properties.getProperty()) {
			ResponseEntity<Property> updated = updateProperty(property, siteByName, null);
			collectProperties(propsList, updated, property.getName(), HttpStatus.OK);
		}
		return new ResponseEntity<Properties>(new Properties(propsList, site, null), HttpStatus.OK);
	}

	private void collectProperties(List<Property> propsList, ResponseEntity<Property> updatedProperty, String name,
			HttpStatus expected) {
		Property prop;
		if (expected.equals(updatedProperty.getStatusCode())) {
			prop = updatedProperty.getBody();
		} else {
			prop = new Property();
			prop.setName(name);
		}
		prop.setStatusCode(updatedProperty.getStatusCode().value());
		prop.setStatusMessage(updatedProperty.getStatusCode().getReasonPhrase());
		propsList.add(prop);
	}

	@PostMapping(value = "/site/{site}/properties")
	public ResponseEntity<Properties> createProperties(@PathVariable("site") String site,
			@RequestBody org.appng.appngizer.model.xml.Properties properties) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		List<Property> propsList = new ArrayList<>();
		for (org.appng.appngizer.model.xml.Property property : properties.getProperty()) {
			ResponseEntity<Property> created = createProperty(property, siteByName, null);
			collectProperties(propsList, created, property.getName(), HttpStatus.CREATED);
		}
		return new ResponseEntity<Properties>(new Properties(propsList, site, null), HttpStatus.OK);
	}

	@DeleteMapping(value = "/site/{site}/properties")
	public ResponseEntity<Properties> deleteProperties(@PathVariable("site") String site,
			@RequestBody org.appng.appngizer.model.xml.Properties properties) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		List<Property> propsList = new ArrayList<>();
		for (org.appng.appngizer.model.xml.Property property : properties.getProperty()) {
			ResponseEntity<Property> deleted = deleteProperty(property.getName(), siteByName, null);
			collectProperties(propsList, deleted, property.getName(), HttpStatus.OK);
		}
		return new ResponseEntity<Properties>(new Properties(propsList, site, null), HttpStatus.OK);
	}

	@GetMapping(value = "/site/{site}/property/{prop}")
	public ResponseEntity<Property> getProperty(@PathVariable("site") String site, @PathVariable("prop") String prop) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		return getPropertyResponse(prop, siteByName, null);
	}

	@PostMapping(value = "/site/{site}/property")
	public ResponseEntity<Property> createProperty(@PathVariable("site") String site,
			@RequestBody org.appng.appngizer.model.xml.Property property) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		return createProperty(property, siteByName, null);
	}

	@PutMapping(value = "/site/{site}/property/{prop}")
	public ResponseEntity<Property> updateProperty(@PathVariable("site") String site,
			@RequestBody org.appng.appngizer.model.xml.Property property) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		return updateProperty(property, siteByName, null);
	}

	@DeleteMapping(value = "/site/{site}/property/{prop}")
	public ResponseEntity<Property> deleteProperty(@PathVariable("site") String site,
			@PathVariable("prop") String property) {
		SiteImpl siteByName = getSiteByName(site);
		if (null == siteByName) {
			return notFound();
		}
		return deleteProperty(property, siteByName, null);
	}

	Logger logger() {
		return LOGGER;
	}
}
