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

import org.appng.api.model.Property;
import org.junit.Assert;
import org.junit.Test;

public class PropertyImplTest {
	
	@Test
	public void testDetermineType() {
		PropertyImpl booleanProp = new PropertyImpl("booleanProp", "true");
		booleanProp.determineType();
		Assert.assertEquals(Property.Type.BOOLEAN, booleanProp.getType());
		
		PropertyImpl intProp = new PropertyImpl("intProp", "5");
		intProp.determineType();
		Assert.assertEquals(Property.Type.INT, intProp.getType());
		
		PropertyImpl decimalProp = new PropertyImpl("decimalProp", "5.42");
		decimalProp.determineType();
		Assert.assertEquals(Property.Type.DECIMAL, decimalProp.getType());
		
		PropertyImpl multilineProp = new PropertyImpl("multilineProp", null);
		multilineProp.setClob("textProp");
		multilineProp.determineType();
		Assert.assertEquals(Property.Type.MULTILINE, multilineProp.getType());
		
		PropertyImpl textProp = new PropertyImpl("textProp", "test");
		textProp.determineType();
		Assert.assertEquals(Property.Type.TEXT, textProp.getType());
	}

}
