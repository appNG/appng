/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.core.domain;

import org.junit.Assert;
import org.junit.Test;

public class ValidationPatternTest {

	@Test
	public void testHost() {
		assertMatches("localhost", ValidationPatterns.HOST_PATTERN);
		assertMatches("some.example.com", ValidationPatterns.HOST_PATTERN);
		assertMatches("127.0.0.1", ValidationPatterns.HOST_PATTERN);
		assertMatches("xn--sterreich-z7a.at", ValidationPatterns.HOST_PATTERN);
		assertMatches("007.de", ValidationPatterns.HOST_PATTERN);
	}

	@Test
	public void testDomain() {
		assertMatches("127.0.0.1", ValidationPatterns.DOMAIN_PATTERN);
		assertMatches("127.0.0.1:8080", ValidationPatterns.DOMAIN_PATTERN);
		assertMatches("http://127.0.0.1:8080", ValidationPatterns.DOMAIN_PATTERN);

		assertMatches("some.example.com", ValidationPatterns.DOMAIN_PATTERN);
		assertMatches("some.example.com:8080", ValidationPatterns.DOMAIN_PATTERN);
		assertMatches("https://some.example.com:8080", ValidationPatterns.DOMAIN_PATTERN);
		assertMatches("https://www.xn--sterreich-z7a.at", ValidationPatterns.DOMAIN_PATTERN);
		assertMatches("http://007.de", ValidationPatterns.DOMAIN_PATTERN);
	}

	private void assertMatches(String value, String pattern) {
		Assert.assertTrue(value + " should match" + pattern, value.matches(pattern));
	}

}
