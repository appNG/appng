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

import org.appng.appngizer.model.Properties;
import org.appng.appngizer.model.Property;
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
public class PlatformPropertyController extends PropertyBase {

	@RequestMapping(value = "/platform/property", method = RequestMethod.GET)
	public ResponseEntity<Properties> listProperties() {
		return getProperties(null, null);
	}

	@RequestMapping(value = "/platform/property/{prop}", method = RequestMethod.GET)
	public ResponseEntity<Property> getProperty(@PathVariable("prop") String prop) {
		return getPropertyResponse(prop, null, null);
	}

	@RequestMapping(value = "/platform/property", method = RequestMethod.POST)
	public ResponseEntity<Property> createProperty(@RequestBody org.appng.appngizer.model.xml.Property property) {
		return createProperty(property, null, null);
	}

	@RequestMapping(value = "/platform/property/{prop}", method = RequestMethod.PUT)
	public ResponseEntity<Property> updateProperty(@RequestBody org.appng.appngizer.model.xml.Property property) {
		return updateProperty(property, null, null);
	}

	@RequestMapping(value = "/platform/property/{prop}", method = RequestMethod.DELETE)
	public ResponseEntity<Property> deleteProperty(@PathVariable("prop") String property) {
		return deleteProperty(property, null, null);
	}

	Logger logger() {
		return log;
	}
}
