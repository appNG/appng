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
package org.appng.api.support.environment;

import org.appng.api.support.SiteClassLoader;
import org.appng.xml.application.Permission;
import org.junit.Assert;
import org.junit.Test;

public class AttributeWrapperTest {

	@Test
	public void test() {
		Assert.assertEquals("appng", new AttributeWrapper("appng", "foo").getSiteName());

		SiteClassLoader siteClassLoader = new SiteClassLoader("thesite");
		AttributeWrapper attributeWrapper = new AttributeWrapper("appng", new Permission()) {
			private static final long serialVersionUID = 1L;

			protected ClassLoader getClassloader(Object value) {
				return siteClassLoader;
			}
		};
		Assert.assertEquals(siteClassLoader.getSiteName(), attributeWrapper.getSiteName());
	}

}
