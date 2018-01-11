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
package org.appng.tools.markup;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class XHTMLTest {

	public XHTMLTest() {
	}

	@Test
	public void testRemoveAttr() {
		String attr = "id";
		String content = "<body id=\"top\" background=\"blue\">";
		String expResult = "<body background=\"blue\">";
		String result = XHTML.removeAttr(content, attr);
		assertEquals(expResult, result);

		attr = "body";
		content = "<body>";
		expResult = "<body>";
		result = XHTML.removeAttr(content, attr);
		assertEquals(expResult, result);
	}

	@Test
	public void testSetAttr() {
		String attr = "background";
		String value = "blue";

		String content = "<body>";
		String expResult = "<body background=\"blue\">";
		String result = XHTML.setAttr(content, attr, value);
		assertEquals(expResult, result);

		content = "<body id=\"foo\">";
		expResult = "<body background=\"blue\" id=\"foo\">";
		result = XHTML.setAttr(content, attr, value);
		assertEquals(expResult, result);

		content = "<body id=\"foo\">";
		expResult = "<body id=\"bar\">";
		result = XHTML.setAttr(content, "id", "bar");
		assertEquals(expResult, result);
	}

	@Test
	public void testSetBody() {
		String content = "<a href=\"http://www.aiticon.de\"></a>";
		String value = "aiticon-web";
		String expResult = "<a href=\"http://www.aiticon.de\">aiticon-web</a>";
		String result = XHTML.setBody(content, value);
		assertEquals(expResult, result);

		content = "<a href=\"http://www.aiticon.de\"> extra ></a>";
		value = "intra";
		expResult = "<a href=\"http://www.aiticon.de\">intra</a>";
		result = XHTML.setBody(content, value);
		assertEquals(expResult, result);
	}

	@Test
	public void testGetBody() {
		String content = "<a href=\"http://www.aiticon.de\">aiticon-web</a>";
		String expResult = "aiticon-web";
		String result = XHTML.getBody("a", content);
		assertEquals(expResult, result);

		content = "<a href=\"http://www.aiticon.de\"></a>";
		expResult = "";
		result = XHTML.getBody("a", content);
		assertEquals(expResult, result);
		Assert.assertNull(XHTML.getBody("a", "asdad"));
	}

	@Test
	public void testGetAttr() {
		String content = "<a href=\"http://www.aiticon.de\"name=\"blub\">Aiticon</a>";
		String attr = "href";
		String expResult = "http://www.aiticon.de";
		String result = XHTML.getAttr(content, attr);
		assertEquals(expResult, result);

		content = "<a href=\"http://www.aiticon.de\"name=\"\">Aiticon</a>";
		attr = "name";
		expResult = "";
		result = XHTML.getAttr(content, attr);
		assertEquals(expResult, result);
	}

	@Test
	public void testGetAttr2() {
		String result = XHTML.getAttr("<input data-type=\"foobar\" type=\"checkbox\" />", "type");
		assertEquals("checkbox", result);
	}

	@Test
	public void testGetTag() {
		String content = "<body id=\"top\" background=\"blue\">";
		String expResult = "body";
		String result = XHTML.getTag(content);
		assertEquals(expResult, result);
	}
}
