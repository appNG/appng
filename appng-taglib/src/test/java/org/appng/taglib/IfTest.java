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
package org.appng.taglib;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.junit.Assert;
import org.junit.Test;

public class IfTest extends TagletTestBase {

	@Test
	public void testInclude() throws JspException {
		If ifTaglet = new If();
		ifTaglet.setCondition("(a == 'foo') or (a.startsWith('f'))");
		Map<String, String> parameters = new HashMap<>();
		parameters.put("a", "foo");
		ifTaglet.setPageContext(setupTagletTest(parameters));
		int result = ifTaglet.doStartTag();
		Assert.assertEquals(Tag.EVAL_BODY_INCLUDE, result);
	}

	@Test
	public void testSkip() throws JspException {
		If ifTaglet = new If();
		ifTaglet.setCondition("(a == 'bar') or (a.startsWith('b'))");
		Map<String, String> parameters = new HashMap<>();
		parameters.put("a", "foo");
		ifTaglet.setPageContext(setupTagletTest(parameters));
		int result = ifTaglet.doStartTag();
		Assert.assertEquals(Tag.SKIP_BODY, result);
	}

}
