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
package org.appng.api.model;

import org.appng.api.model.Property.Type;
import org.junit.Assert;
import org.junit.Test;

public class PropertyTypeTest {

	@Test
	public void testText() {
		validateForObject(Type.TEXT, "foo");
	}

	@Test
	public void testMultiline() {
		validateForObject(Type.MULTILINE, "multi\nline");
	}

	@Test
	public void testBoolean() {
		validateForObject(Type.BOOLEAN, true);
		validateForObject(Type.BOOLEAN, Boolean.FALSE);

		testValueValid(Type.BOOLEAN, "true");
		testValueValid(Type.BOOLEAN, "True");
		testValueValid(Type.BOOLEAN, "TRUE");

		testValueValid(Type.BOOLEAN, "false");
		testValueValid(Type.BOOLEAN, "faLse");
		testValueValid(Type.BOOLEAN, "FALSE");

		testValueInvalid(Type.BOOLEAN, null);
		testValueInvalid(Type.BOOLEAN, "");
		testValueInvalid(Type.BOOLEAN, "bla");
		testValueInvalid(Type.BOOLEAN, "123");
	}

	@Test
	public void testInt() {
		validateForObject(Type.INT, 5);
		validateForObject(Type.INT, Integer.valueOf(5));
		validateForObject(Type.INT, Integer.valueOf(5).shortValue());

		testValueValid(Type.INT, "5");
		testValueValid(Type.INT, "+5");
		testValueValid(Type.INT, "-700");

		testValueInvalid(Type.BOOLEAN, null);
		testValueInvalid(Type.BOOLEAN, "");
		testValueInvalid(Type.INT, "bla");
		testValueInvalid(Type.INT, "-5,3");
		testValueInvalid(Type.INT, "+5,3");
		testValueInvalid(Type.INT, "3,5");
	}

	@Test
	public void testDecimal() {
		validateForObject(Type.DECIMAL, 5d);
		validateForObject(Type.DECIMAL, 5f);
		validateForObject(Type.DECIMAL, Double.valueOf(5));
		validateForObject(Type.DECIMAL, Float.valueOf(5));

		testValueValid(Type.DECIMAL, "5.0");
		testValueValid(Type.DECIMAL, "+5.0");
		testValueValid(Type.DECIMAL, "-700.0");

		testValueInvalid(Type.BOOLEAN, null);
		testValueInvalid(Type.BOOLEAN, "");
		testValueInvalid(Type.DECIMAL, "bla");
		testValueInvalid(Type.DECIMAL, "5.");
		testValueInvalid(Type.DECIMAL, "-5,3");
		testValueInvalid(Type.DECIMAL, "+5,3");
		testValueInvalid(Type.DECIMAL, "3,5");
	}

	private void testValueValid(Type type, String value) {
		Assert.assertTrue(Property.Type.isValidValue(type, value));
	}

	private void testValueInvalid(Type type, String value) {
		Assert.assertFalse(Property.Type.isValidValue(type, value));
	}

	private void validateForObject(Type type, Object value) {
		Assert.assertEquals(type, Type.forObject(value));
	}

}
