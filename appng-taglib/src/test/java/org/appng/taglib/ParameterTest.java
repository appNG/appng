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
package org.appng.taglib;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockBodyContent;

public class ParameterTest extends BodyTagSupport implements ParameterOwner {

	private Map<String, String> parameters = new HashMap<>();

	@Test
	public void testParameter() throws IOException, JspException {
		StringWriter targetWriter = new StringWriter();

		Parameter p1 = new Parameter();
		p1.setName("param1");
		p1.setBodyContent(new MockBodyContent("value1", targetWriter));
		p1.setParent(this);
		p1.doEndTag();

		Parameter p2 = new Parameter();
		p2.setUnescape(true);
		p2.setBodyContent(new MockBodyContent("&quot;&Auml;&quot;", targetWriter));
		p2.setName("param2");
		p2.setParent(this);
		p2.doEndTag();

		Parameter p3 = new Parameter();
		p3.setName("param3");
		p3.setParent(this);
		p3.doEndTag();

		Assert.assertEquals("value1", parameters.get("param1"));
		Assert.assertEquals("\"Ä\"", parameters.get("param2"));
		Assert.assertNull(parameters.get("param3"));
		Assert.assertEquals("", targetWriter.toString());
	}

	public void addParameter(String name, String value) {
		parameters.put(name, value);
	}

}
