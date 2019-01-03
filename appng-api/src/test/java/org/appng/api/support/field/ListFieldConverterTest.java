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
package org.appng.api.support.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.api.FieldWrapper;
import org.appng.api.Person;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.io.ClassPathResource;

public class ListFieldConverterTest extends AbstractFieldConverterTest {

	private static final ArrayList<Integer> EMPTY_LIST = new ArrayList<Integer>();
	List<Integer> numbers = Arrays.asList(1, 2, 3);
	List<String> stringNumbers = Arrays.asList("1", "2", "3");

	@Before
	public void setup() throws Exception {
		super.setup(FieldType.LIST_SELECT);
		Mockito.when(request.getParameterList(OBJECT)).thenReturn(stringNumbers);
		Mockito.when(messageSource.getMessage(IntegerFieldConverter.ERROR_KEY, new Object[0], environment.getLocale()))
				.thenReturn(IntegerFieldConverter.ERROR_KEY);
		beanWrapper.setPropertyValue(OBJECT, EMPTY_LIST);
	}

	@Test
	public void testSetObject() throws Exception {
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(numbers, fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectEmptyValue() {
		Mockito.when(request.getParameterList(OBJECT)).thenReturn(Arrays.asList(""));
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(EMPTY_LIST, fieldWrapper.getObject());
	}

	@Test(expected = ConversionException.class)
	public void testSetObjectInvalidValue() {
		Mockito.when(request.getParameterList(OBJECT)).thenReturn(Arrays.asList("assdsd"));
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(EMPTY_LIST, fieldWrapper.getObject());
		String content = fieldWrapper.getMessages().getMessageList().get(0).getContent();
		Assert.assertEquals(IntegerFieldConverter.ERROR_KEY, content);
	}

	@Test
	public void testSetObjectNull() {
		Mockito.when(request.getParameterList(OBJECT)).thenReturn(null);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(EMPTY_LIST, fieldWrapper.getObject());
	}

	@Test
	public void testSetString() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, numbers);
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals("1,2,3", fieldWrapper.getStringValue());
	}

	@Test
	public void testSetStringNullObject() {
		beanWrapper.setPropertyValue(OBJECT, null);
		fieldConverter.setString(fieldWrapper);
		Assert.assertNull(fieldWrapper.getStringValue());
	}

	@Test
	public void testSetStringInvalidType() {
		beanWrapper = new BeanWrapperImpl(new Container<Long>() {
		});
		beanWrapper.setPropertyValue(OBJECT, 5L);
		fieldWrapper = new FieldWrapper(field, beanWrapper);
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals("5", fieldWrapper.getStringValue());
	}

	@Test
	public void testAddField() {
		beanWrapper.setPropertyValue(OBJECT, numbers);
		DatafieldOwner dataFieldOwner = getDatafieldOwner();
		fieldConverter.addField(dataFieldOwner, fieldWrapper);
		Datafield datafield = dataFieldOwner.getFields().get(0);
		Assert.assertEquals(3, datafield.getFields().size());
		Assert.assertEquals("", datafield.getValue());
		Assert.assertEquals("object", datafield.getName());
		Assert.assertEquals("object[0]", datafield.getFields().get(0).getName());
		Assert.assertEquals("1", datafield.getFields().get(0).getValue());
		Assert.assertEquals("object[1]", datafield.getFields().get(1).getName());
		Assert.assertEquals("2", datafield.getFields().get(1).getValue());
		Assert.assertEquals("object[2]", datafield.getFields().get(2).getName());
		Assert.assertEquals("3", datafield.getFields().get(2).getValue());
	}

	@Test
	public void testAddEmptyField() {
		beanWrapper.setPropertyValue(OBJECT, null);
		DatafieldOwner dataFieldOwner = getDatafieldOwner();
		fieldConverter.addField(dataFieldOwner, fieldWrapper);
		Assert.assertEquals(1, dataFieldOwner.getFields().size());
		Assert.assertEquals("", dataFieldOwner.getFields().get(0).getValue());
	}

	@Test
	public void testAddNestedFields() throws Exception {
		Person a = new Person();
		Person b = new Person();
		b.setName("Jane Doe");
		Person c = new Person();
		c.setName("John Doe");

		a.getOffsprings().add(b);
		a.getOffsprings().add(c);
		b.getOffsprings().add(c);

		ClassPathResource classPathResource = new ClassPathResource(
				"xml/ListFieldConverterTest-testAddNestedFields.xml");
		MarshallService marshallService = MarshallService.getMarshallService();
		MetaData metaData = marshallService.unmarshall(classPathResource.getInputStream(), MetaData.class);

		beanWrapper = new BeanWrapperImpl(a);
		FieldDef fieldDef = metaData.getFields().get(0);
		this.fieldWrapper = new FieldWrapper(fieldDef, beanWrapper);

		Result result = new Result();
		DatafieldOwner dataFieldOwner = new DatafieldOwner() {

			public List<Linkpanel> getLinkpanels() {
				return null;
			}

			public List<Datafield> getFields() {
				return result.getFields();
			}
		};
		fieldConverter.addField(dataFieldOwner, fieldWrapper);
		String marshallNonRoot = marshallService.marshallNonRoot(result);
		ClassPathResource controlSource = new ClassPathResource(
				"xml/ListFieldConverterTest-testAddNestedFields-result.xml");
		String expected = new String(
				IOUtils.readFully(controlSource.getInputStream(), (int) controlSource.contentLength()));
		Assert.assertEquals(expected, marshallNonRoot);
	}

	@Override
	public Container<?> getContainer() {
		return new Container<List<Integer>>() {
		};
	}
}
