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
package org.appng.taglib;

import javax.servlet.jsp.PageContext;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.taglib.Attribute.Mode;
import org.junit.Assert;
import org.junit.Test;

public class AttributeTest extends TagletTestBase {

	private PageContext pageContext;

	@Test(expected = IllegalArgumentException.class)
	public void testReadFromUrlInvalid() {
		Attribute attribute = init(Mode.READ, Scope.URL);
		attribute.doStartTag();
		Assert.assertEquals("fromUrl", attribute.getValue());
	}

	@Test
	public void testReadFromUrlNoParam() {
		Attribute attribute = init(Mode.READ, Scope.URL);
		attribute.setName("4");
		attribute.doStartTag();
		Assert.assertEquals("", attribute.getValue());
	}

	@Test
	public void testReadFromUrl() {
		Attribute attribute = init(Mode.READ, Scope.URL);
		attribute.setName("3");
		attribute.doStartTag();
		Assert.assertEquals("fromUrl", attribute.getValue());
	}

	@Test
	public void testReadFromSite() {
		Attribute attribute = init(Mode.READ, Scope.SITE);
		attribute.doStartTag();
		Assert.assertEquals("fromSite", attribute.getValue());
	}

	@Test
	public void testReadFromRequest() {
		Attribute attribute = init(Mode.READ, Scope.REQUEST);
		attribute.doStartTag();
		Assert.assertEquals("fromRequest", attribute.getValue());
	}

	@Test
	public void testReadFromSession() {
		Attribute attribute = init(Mode.READ, Scope.SESSION);
		attribute.doStartTag();
		Assert.assertEquals("fromSession", attribute.getValue());
	}

	@Test
	public void testReadFromPlatform() {
		Attribute attribute = init(Mode.READ, Scope.PLATFORM);
		attribute.doStartTag();
		Assert.assertEquals("fromPlatform", attribute.getValue());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testWriteToUrl() {
		Attribute attribute = init(Mode.WRITE, Scope.URL);
		attribute.doStartTag();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testWriteToSite() {
		Attribute attribute = init(Mode.WRITE, Scope.SITE);
		attribute.doStartTag();
	}

	@Test
	public void testWriteToRequest() {
		Attribute attribute = init(Mode.WRITE, Scope.REQUEST);
		attribute.setValue("writeToRequest");
		attribute.doStartTag();
		String variable = DefaultEnvironment.get(pageContext).getAttribute(Scope.REQUEST, "variable");
		Assert.assertEquals("writeToRequest", variable);
	}

	@Test
	public void testWriteToSession() {
		Attribute attribute = init(Mode.WRITE, Scope.SESSION);
		attribute.setValue("writeToSession");
		attribute.doStartTag();
		String variable = DefaultEnvironment.get(pageContext).getAttribute(Scope.SESSION, "variable");
		Assert.assertEquals("writeToSession", variable);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testWriteToPlatform() {
		Attribute attribute = init(Mode.WRITE, Scope.PLATFORM);
		attribute.doStartTag();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidScope() {
		Attribute attribute = new Attribute();
		attribute.setScope("sadsad");
		attribute.doStartTag();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidMode() {
		Attribute attribute = new Attribute();
		attribute.setMode("sadsad");
		attribute.doStartTag();
	}

	@Test
	public void testObject() {
		Attribute attribute = init(Mode.READ, Scope.PLATFORM);
		attribute.setName(Platform.Environment.SITES);
		attribute.doStartTag();
		Assert.assertTrue(attribute.getValue().startsWith("{localhost=Mock for Site, hashCode:"));
	}

	@Test
	public void testObjectPath() {
		Attribute attribute = init(Mode.READ, Scope.PLATFORM);
		attribute.setName(Platform.Environment.SITES + ".localhost");
		attribute.doStartTag();
		Assert.assertTrue(attribute.getValue().startsWith("Mock for Site, hashCode:"));
		attribute.setName(Platform.Environment.SITES + "['localhost']");
		attribute.doStartTag();
		Assert.assertTrue(attribute.getValue().startsWith("Mock for Site, hashCode:"));
		attribute.setName(Platform.Environment.SITES + "['localhost'].name");
		attribute.doStartTag();
		Assert.assertEquals("localhost", attribute.getValue());
		attribute.setName(Platform.Environment.SITES + "['localhost'].getName()");
		attribute.doStartTag();
		Assert.assertEquals("localhost", attribute.getValue());
		attribute.setName(Platform.Environment.SITES + ".localhost.getName()");
		attribute.doStartTag();
		Assert.assertEquals("localhost", attribute.getValue());
	}

	@Test
	public void testObjectPathNoValue() {
		Attribute attribute = init(Mode.READ, Scope.PLATFORM);
		attribute.setName("does.not.exist");
		attribute.doStartTag();
		Assert.assertEquals("", attribute.getValue());
	}

	private Attribute init(Mode mode, Scope scope) {
		Attribute attribute = new Attribute();
		attribute.setMode(mode.name());
		attribute.setScope(scope.name());
		attribute.setName("variable");
		pageContext = setupTagletTest();
		attribute.setPageContext(pageContext);
		return attribute;
	}

	protected static PageContext setupTagletTest() {
		return setupTagletTest(null);
	}
}
