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
package org.appng.api.support.field;

import java.util.Date;

import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.api.FieldWrapper;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeanWrapperImpl;

public class IntegerFieldConverterTest extends AbstractFieldConverterTest {

	private static final String NUMBER_DEFAULT_FORMAT = "1234456";
	private static final int NUMBER = 1234456;
	private static final String NUMBER_STRING = "1,234,456";
	private static final String FORMAT = "#,###";

	@Before
	public void setup() throws Exception {
		super.setup(FieldType.INT);
		Mockito.when(request.getParameter(OBJECT)).thenReturn(NUMBER_STRING);
		Mockito.when(messageSource.getMessage(IntegerFieldConverter.ERROR_KEY, new Object[0], environment.getLocale()))
				.thenReturn(IntegerFieldConverter.ERROR_KEY);
	}

	@Override
	@Test
	public void testSetObject() throws Exception {
		Mockito.when(request.getParameter(OBJECT)).thenReturn(NUMBER_DEFAULT_FORMAT);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(NUMBER, fieldWrapper.getObject());
		Assert.assertEquals(IntegerFieldConverter.DEFAULT_INTEGER_PATTERN, fieldWrapper.getFormat());
	}

	@Override
	@Test
	public void testSetObjectEmptyValue() {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertNull(fieldWrapper.getObject());
	}

	@Override
	@Test
	public void testSetObjectInvalidValue() {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("asdasd");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertNull(fieldWrapper.getObject());
		String content = fieldWrapper.getMessages().getMessageList().get(0).getContent();
		Assert.assertEquals(IntegerFieldConverter.ERROR_KEY, content);
	}

	@Test
	public void testSetObjectIntMaxValue() {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("2247483648");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertNull(fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectLongMaxValue() {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("9223372036854775808000000000000");
		beanWrapper = new BeanWrapperImpl(new Container<Long>() {
		});
		fieldWrapper = new FieldWrapper(field, beanWrapper);
		fieldWrapper.setType(FieldType.LONG);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Long.MAX_VALUE, fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectDoubleMaxValue() {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("1.8976931348623157E308");
		beanWrapper = new BeanWrapperImpl(new Container<Double>() {
		});
		fieldWrapper = new FieldWrapper(field, beanWrapper);
		fieldWrapper.setType(FieldType.DECIMAL);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Double.POSITIVE_INFINITY, fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectFloatMaxValue() {
		Mockito.when(request.getParameter(OBJECT)).thenReturn("3.5028235E38");
		beanWrapper = new BeanWrapperImpl(new Container<Float>() {
		});
		fieldWrapper = new FieldWrapper(field, beanWrapper);
		fieldWrapper.setType(FieldType.DECIMAL);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Float.POSITIVE_INFINITY, fieldWrapper.getObject());
	}

	@Override
	@Test
	public void testSetObjectNull() {
		Mockito.when(request.getParameter(OBJECT)).thenReturn(null);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertNull(fieldWrapper.getObject());
	}

	@Override
	@Test
	public void testSetString() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, NUMBER);
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals(NUMBER_DEFAULT_FORMAT, fieldWrapper.getStringValue());
		Assert.assertEquals(IntegerFieldConverter.DEFAULT_INTEGER_PATTERN, fieldWrapper.getFormat());
	}

	@Test
	public void testSetStringDefaultFormat() throws Exception {
		fieldWrapper.setFormat(null);
		fieldConverter.setString(fieldWrapper);
		Assert.assertNull(fieldWrapper.getStringValue());
		Assert.assertEquals(IntegerFieldConverter.DEFAULT_INTEGER_PATTERN, fieldWrapper.getFormat());
	}

	@Test
	public void testSetObjectCustomFormat() throws Exception {
		fieldWrapper.setFormat(FORMAT);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(NUMBER, fieldWrapper.getObject());
	}

	@Override
	@Test
	public void testSetStringNullObject() {
		fieldConverter.setString(fieldWrapper);
		Assert.assertNull(fieldWrapper.getStringValue());
	}

	@Override
	@Test(expected = IllegalArgumentException.class)
	public void testSetStringInvalidType() {
		beanWrapper = new BeanWrapperImpl(new Container<Date>() {
		});
		beanWrapper.setPropertyValue(OBJECT, new Date());
		fieldWrapper = new FieldWrapper(field, beanWrapper);
		fieldConverter.setString(fieldWrapper);
	}

	@Test
	public void testSetStringCustomFormat() throws Exception {
		fieldWrapper.setFormat(FORMAT);
		beanWrapper.setPropertyValue(OBJECT, NUMBER);
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals(NUMBER_STRING, fieldWrapper.getStringValue());
	}

	@Override
	public Container<?> getContainer() {
		return new Container<Integer>() {
		};
	}

	@Override
	@Test
	public void testAddField() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, NUMBER);
		fieldWrapper.setFormat(FORMAT);
		DatafieldOwner dataFieldOwner = getDatafieldOwner();
		fieldConverter.addField(dataFieldOwner, fieldWrapper);
		Datafield datafield = dataFieldOwner.getFields().get(0);
		Assert.assertEquals("object", datafield.getName());
		Assert.assertEquals(NUMBER_STRING, datafield.getValue());
		Assert.assertEquals(0, datafield.getFields().size());
	}

}
