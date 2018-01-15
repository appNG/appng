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

import org.appng.api.SiteProperties;
import org.appng.appngizer.model.xml.Property;
import org.appng.appngizer.model.xml.Site;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SitePropertyControllerTest extends ControllerTest {

	static {
		XMLUnit.setIgnoreWhitespace(true);
	}

	@Test
	public void test() throws Exception {
		differenceListener.ignoreDifference("/properties/property/description/text()");

		Site created = new Site();
		created.setName("localhost");
		created.setHost("localhost");
		created.setDomain("http://localhost:8081");
		created.setDescription("none");
		created.setActive(false);
		created.setCreateRepositoryPath(true);

		postAndVerify("/site", "xml/site-create.xml", created, HttpStatus.CREATED);

		getAndVerify("/site/localhost/property", "xml/site-property-list.xml", HttpStatus.OK);

		Property prop = new Property();
		prop.setName(SiteProperties.ASSETS_DIR);
		prop.setValue("42");
		prop.setDefaultValue("this has no effect");
		prop.setDescription("foo");
		putAndVerify("/site/localhost/property/" + prop.getName(), "xml/site-property-update.xml", prop, HttpStatus.OK);

		Property xss = new Property();
		xss.setName(SiteProperties.XSS_EXCEPTIONS);
		xss.setValue("#comment\nfoo\nbar");
		xss.setClob(true);
		xss.setDescription("exceptions for XSS protection");
		putAndVerify("/site/localhost/property/" + xss.getName(), "xml/site-property-update2.xml", xss, HttpStatus.OK);

		prop.setName("theAnswer");
		prop.setDefaultValue("42");
		prop.setDescription("to life, the universe and everything");
		postAndVerify("/site/localhost/property", "xml/site-property-create.xml", prop, HttpStatus.CREATED);
		getAndVerify("/site/localhost/property/theAnswer", "xml/site-property-create.xml", HttpStatus.OK);

		deleteAndVerify("/site/localhost/property/theAnswer", null, HttpStatus.NO_CONTENT);
	}

}