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
package org.appng.appngizer.controller;

import java.util.Arrays;
import java.util.List;

import org.appng.appngizer.model.xml.Property;
import org.appng.testsupport.validation.XPathDifferenceHandler;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PlatformPropertyControllerTest extends ControllerTest {

	static {
		XMLUnit.setIgnoreWhitespace(true);
	}

	@Test
	public void test() throws Exception {
		List<Integer> ids = Arrays.asList(2, 3, 5, 8, 10, 11, 22, 30, 47, 48);
		this.differenceListener = new XPathDifferenceHandler(false);
		ids.forEach(idx -> differenceListener
				.ignoreDifference("/properties[1]/property[" + idx + "]/description[1]/text()[1]"));

		// shared secret is generated
		differenceListener.ignoreDifference("/properties[1]/property[41]/value[1]/text()[1]");
		differenceListener.ignoreDifference("/properties[1]/property[41]/defaultValue[1]/text()[1]");
		getAndVerify("/platform/property", "xml/platform-property-list.xml", HttpStatus.OK);

		Property prop = new Property();
		prop.setName("cacheFolder");
		prop.setValue("42");
		prop.setDefaultValue("this has no effect");
		prop.setDescription("foo");
		putAndVerify("/platform/property/cacheFolder", "xml/platform-property-update.xml", prop, HttpStatus.OK);

		prop.setName("theAnswer");
		prop.setDefaultValue("42");
		prop.setDescription("to life, the universe and everything");
		postAndVerify("/platform/property", "xml/platform-property-create.xml", prop, HttpStatus.CREATED);
		getAndVerify("/platform/property/theAnswer", "xml/platform-property-create.xml", HttpStatus.OK);

		deleteAndVerify("/platform/property/theAnswer", null, HttpStatus.NO_CONTENT);
	}

}