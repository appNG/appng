/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.forms;

import org.jsoup.safety.Whitelist;
import org.junit.Assert;
import org.junit.Test;
import org.owasp.esapi.ESAPI;

public class XSSUtilTest {
	
	@Test
	public void test(){
		XSSUtil xssUtil = new XSSUtil(ESAPI.encoder(), new Whitelist());
		Assert.assertEquals("", xssUtil.stripXss("<script>alert('XSS!')<%2Fscript>"));
		Assert.assertEquals("", xssUtil.stripXss("<meta http-equiv%3D\"refresh\" content%3D\"0; url%3Dhttps:%2F%2Fwww.aiticon.com%2F\">"));
	}

}
