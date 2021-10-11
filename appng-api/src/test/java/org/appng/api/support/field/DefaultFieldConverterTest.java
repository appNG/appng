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
package org.appng.api.support.field;

import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.api.FieldWrapper;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionException;

public class DefaultFieldConverterTest extends AbstractFieldConverterTest {

	@Before
	public void setup() throws Exception {
		super.setup(FieldType.TEXT);
		Mockito.when(request.getParameter(OBJECT)).thenReturn("true");
	}

	public Container<?> getContainer() {
		return new Container<Boolean>() {
		};
	}

	@Test
	public void testSetObject() throws Exception {
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Boolean.TRUE, fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectEmptyValue() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(null, fieldWrapper.getObject());
	}

	@Test(expected = ConversionException.class)
	public void testSetObjectInvalidValue() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("blaa");
		fieldConverter.setObject(fieldWrapper, request);
	}

	@Test
	public void testSetObjectNull() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn(null);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertNull(fieldWrapper.getObject());
	}

	@Test
	public void testSetString() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, Boolean.FALSE);
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals("false", fieldWrapper.getStringValue());
	}

	@Test
	public void testSetStringNullObject() throws Exception {
		fieldConverter.setString(fieldWrapper);
		Assert.assertNull(fieldWrapper.getStringValue());
	}

	@Test
	public void testSetStringInvalidType() throws Exception {
		beanWrapper = new BeanWrapperImpl(new Container<Long>() {
		});
		beanWrapper.setPropertyValue(OBJECT, 5L);
		fieldWrapper = new FieldWrapper(field, beanWrapper);
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals("5", fieldWrapper.getStringValue());
	}

	@Test
	public void testAddField() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, "true");
		DatafieldOwner dataFieldOwner = getDatafieldOwner();
		fieldConverter.addField(dataFieldOwner, fieldWrapper);
		Datafield datafield = dataFieldOwner.getFields().get(0);
		Assert.assertEquals("object", datafield.getName());
		Assert.assertEquals("true", datafield.getValue());
		Assert.assertEquals(0, datafield.getFields().size());
	}

	@Test
	public void testRemovalOfNonPrintableControlCharacter() {
		for (int c = 0; c < 32; c++) {
			if (c != 9 && c != 10 && c != 13) {
				String s = Character.toString((char) c);
				Assert.assertEquals("", DefaultFieldConverter.stripNonPrintableCharacter(s));
			}
		}
		int[] allowedContrChar = { 9, 10, 13 };
		for (int c : allowedContrChar) {
			String s = Character.toString((char) c);
			Assert.assertEquals(s, DefaultFieldConverter.stripNonPrintableCharacter(s));
		}
		for (int c = 32; c < 127; c++) {
			String s = Character.toString((char) c);
			Assert.assertEquals(s, DefaultFieldConverter.stripNonPrintableCharacter(s));
		}
		for (int c = 127; c < 160; c++) {
			String s = Character.toString((char) c);
			Assert.assertEquals("", DefaultFieldConverter.stripNonPrintableCharacter(s));
		}
		for (int c = 160; c < 65535; c++) { // 1424
			String s = Character.toString((char) c);
			Assert.assertEquals(s, DefaultFieldConverter.stripNonPrintableCharacter(s));
		}
	}

}
