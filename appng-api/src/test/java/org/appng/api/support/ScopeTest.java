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
package org.appng.api.support;

import org.appng.api.Scope;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link Scope}
 * 
 * @author Matthias Müller
 * 
 */
public class ScopeTest {

	@Test
	public void testSite() {
		Assert.assertEquals("SITE", Scope.SITE.name());
		Assert.assertEquals("SITE.foo", Scope.SITE.forSite("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPaltform() {
		Assert.assertEquals("PLATFORM", Scope.PLATFORM.name());
		Scope.PLATFORM.forSite("foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSession() {
		Assert.assertEquals("SESSION", Scope.SESSION.name());
		Scope.SESSION.forSite("foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRequest() {
		Assert.assertEquals("REQUEST", Scope.REQUEST.name());
		Scope.REQUEST.forSite("foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUrl() {
		Assert.assertEquals("URL", Scope.URL.name());
		Scope.URL.forSite("foo");
	}

}
