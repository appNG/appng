/*
 * Copyright 2011-2022 the original author or authors.
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
package org.appng.appngizer.model;

import org.appng.api.model.Property.Type;
import org.appng.api.model.SimpleProperty;
import org.junit.Assert;
import org.junit.Test;

public class PropertyTest {

	@Test
	public void testTextProp() {
		SimpleProperty textProp = new SimpleProperty("platform.prop", "foo", "bar");
		textProp.setType(Type.TEXT);
		Property prop = Property.fromDomain(textProp, null, null);
		Assert.assertEquals("foo", prop.getValue());
		Assert.assertEquals("bar", prop.getDefaultValue());
		Assert.assertNull(prop.isClob());
	}

	@Test
	public void testMultilineProp() {
		SimpleProperty textProp = new SimpleProperty("platform.prop", null, null);
		textProp.setType(Type.MULTILINE);
		textProp.setClob("multiline!");
		Property prop = Property.fromDomain(textProp, null, null);
		Assert.assertEquals("multiline!", prop.getValue());
		Assert.assertNull(prop.getDefaultValue());
		Assert.assertTrue(prop.isClob());
	}
}
