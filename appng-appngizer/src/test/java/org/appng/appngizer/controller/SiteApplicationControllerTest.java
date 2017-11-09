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

import org.appng.appngizer.model.xml.Grant;
import org.appng.appngizer.model.xml.Grants;
import org.appng.appngizer.model.xml.Property;
import org.appng.appngizer.model.xml.Site;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SiteApplicationControllerTest extends ControllerTest {

	static {
		XMLUnit.setIgnoreWhitespace(true);
	}

	@Test
	public void test() throws Exception {
		differenceListener.ignoreDifference("/properties/property/description/text()");
		differenceListener.ignoreDifference("/properties[1]/property[2]/value[1]/text()[1]");

		installApplication();

		Site created = new Site();
		created.setName("localhost");
		created.setHost("localhost");
		created.setDomain("http://localhost:8081");
		created.setDescription("none");
		created.setActive(false);
		created.setCreateRepositoryPath(true);

		postAndVerify("/site", "xml/site-create.xml", created, HttpStatus.CREATED);

		Site anotherSite = new Site();
		anotherSite.setName("anotherSite");
		anotherSite.setHost("anotherHost");
		anotherSite.setDomain("http://localhost:8082");
		anotherSite.setDescription("none");
		anotherSite.setActive(false);
		anotherSite.setCreateRepositoryPath(true);
		postAndVerify("/site", "xml/site-create2.xml", anotherSite, HttpStatus.CREATED);

		MockHttpServletResponse response = postAndVerify("/site/localhost/application/demo-application", null, null,
				HttpStatus.SEE_OTHER);
		assertLocation("http://localhost/site/localhost/application/demo-application", response);

		postAndVerify("/site/localhost/application/demo-application", null, null, HttpStatus.METHOD_NOT_ALLOWED);

		getAndVerify("/site/localhost/application", "xml/site-application-list.xml", HttpStatus.OK);
		getAndVerify("/site/localhost/application/demo-application", "xml/site-application-show.xml", HttpStatus.OK);

		Grants grants = new Grants();
		Grant grant = new Grant();
		grant.setValue(false);
		grant.setSite("localhost");
		Grant anotherGrant = new Grant();
		anotherGrant.setValue(true);
		anotherGrant.setSite("anotherSite");
		grants.getGrant().add(anotherGrant);

		putAndVerify("/site/localhost/application/demo-application/grants", "xml/site-application-grants.xml", grants, HttpStatus.OK);

		String propertyPath = "/site/localhost/application/demo-application/property";

		getAndVerify(propertyPath, "xml/site-application-property-list.xml", HttpStatus.OK);

		Property prop = new Property();
		prop.setName("bar");
		prop.setValue("foo");
		prop.setDefaultValue("this has no effect");
		prop.setDescription("this is foo, bar!");
		putAndVerify(propertyPath + "/bar", "xml/site-application-property-update.xml", prop, HttpStatus.OK);
		getAndVerify(propertyPath + "/bar", "xml/site-application-property-update.xml", HttpStatus.OK);

		prop.setName("myNewProp");
		prop.setDefaultValue("foo");
		prop.setDescription("this is foo, bar!");
		postAndVerify(propertyPath, "xml/site-application-property-create.xml", prop, HttpStatus.CREATED);
		postAndVerify(propertyPath, null, prop, HttpStatus.CONFLICT);

		deleteAndVerify(propertyPath + "/myNewProp", null, HttpStatus.NO_CONTENT);

		MockHttpServletResponse deleteResp = deleteAndVerify("/site/localhost/application/demo-application", null,
				HttpStatus.SEE_OTHER);

		assertLocation("http://localhost/site/localhost/application", deleteResp);

		deleteAndVerify("/site/localhost/application/demo-application", null, HttpStatus.NOT_FOUND);
	}

}