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
package org.appng.api.support.field;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

public class DateFieldConverterTest extends AbstractFieldConverterTest {

	protected static final String DATE_STRING = "2012-12-05 12:50:00";
	protected static final String DATE_STRING_SHORT = "2012-12-05";
	protected static final String SHORT_FORMAT = "yyyy-MM-dd";

	protected SimpleDateFormat sdf;
	protected SimpleDateFormat sdfShort;

	@Before
	public void setup() throws Exception {
		super.setup(FieldType.DATE);
		Mockito.when(request.getParameter(OBJECT)).thenReturn(DATE_STRING);
		this.sdf = new SimpleDateFormat(DateFieldConverter.DEFAULT_DATEPATTERN);
		this.sdfShort = new SimpleDateFormat(SHORT_FORMAT);
		Mockito.when(messageSource.getMessage(DateFieldConverter.ERROR_KEY, new Object[0], environment.getLocale()))
				.thenReturn(DateFieldConverter.ERROR_KEY);
	}

	@Override
	@Test
	public void testSetObject() throws Exception {
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(getDate(), fieldWrapper.getObject());
		Assert.assertEquals(DateFieldConverter.DEFAULT_DATEPATTERN, fieldWrapper.getFormat());
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
		Assert.assertEquals(DateFieldConverter.ERROR_KEY, content);
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
		beanWrapper.setPropertyValue(OBJECT, getDate());
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals(DATE_STRING, fieldWrapper.getStringValue());
		Assert.assertEquals(DateFieldConverter.DEFAULT_DATEPATTERN, fieldWrapper.getFormat());
	}

	@Test
	public void testSetStringDefaultFormat() throws Exception {
		fieldWrapper.setFormat(null);
		fieldConverter.setString(fieldWrapper);
		Assert.assertNull(fieldWrapper.getStringValue());
		Assert.assertEquals(DateFieldConverter.DEFAULT_DATEPATTERN, fieldWrapper.getFormat());
	}

	@Test
	public void testSetObjectCustomFormat() throws Exception {
		fieldWrapper.setFormat(SHORT_FORMAT);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(getShortDate(), fieldWrapper.getObject());
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
		beanWrapper = new BeanWrapperImpl(new Container<Long>() {
		});
		beanWrapper.setPropertyValue(OBJECT, 12L);
		fieldWrapper = new FieldWrapper(field, beanWrapper);
		fieldConverter.setString(fieldWrapper);
	}

	@Test
	public void testSetStringCustomFormat() throws Exception {
		fieldWrapper.setFormat(SHORT_FORMAT);
		beanWrapper.setPropertyValue(OBJECT, getShortDate());
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals(DATE_STRING_SHORT, fieldWrapper.getStringValue());
	}

	@Override
	public Container<?> getContainer() {
		return new Container<Date>() {
		};
	}

	@Override
	@Test
	public void testAddField() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, getDate());
		DatafieldOwner dataFieldOwner = getDatafieldOwner();
		fieldConverter.addField(dataFieldOwner, fieldWrapper);
		Datafield datafield = dataFieldOwner.getFields().get(0);
		Assert.assertEquals("object", datafield.getName());
		Assert.assertEquals(DATE_STRING, datafield.getValue());
		Assert.assertEquals(0, datafield.getFields().size());
	}

	protected Object getDate() throws ParseException {
		return sdf.parse(DATE_STRING);
	}

	protected Object getShortDate() throws ParseException {
		return sdfShort.parse(DATE_STRING_SHORT);
	}

}
