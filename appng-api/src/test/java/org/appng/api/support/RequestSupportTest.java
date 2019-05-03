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
package org.appng.api.support;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.validation.constraints.NotNull;

import org.appng.api.AbstractTest;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.FieldWrapper;
import org.appng.api.MetaDataProvider;
import org.appng.api.Person;
import org.appng.api.Request;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.FormUpload;
import org.appng.forms.impl.FormUploadBean;
import org.appng.tools.locator.Coordinate;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;

public class RequestSupportTest extends RequestSupportImpl {

	@Mock
	private Request request;
	@Mock
	private Environment env;
	private static ExpressionEvaluator params = new ExpressionEvaluator(new HashMap<>());

	final @Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
		conversionServiceFactoryBean.afterPropertiesSet();
		ConversionService conversionService = conversionServiceFactoryBean.getObject();

		Mockito.when(request.getEnvironment()).thenReturn(env);
		Mockito.when(request.getExpressionEvaluator()).thenReturn(params);
		Mockito.when(env.getTimeZone()).thenReturn(TimeZone.getDefault());
		Mockito.when(env.getLocale()).thenReturn(Locale.GERMAN);
		setConversionService(conversionService);
		setMessageSource(AbstractTest.getMessageSource());
		setEnvironment(env);
		afterPropertiesSet();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTypes() {
		List<FieldType> allTypes = new ArrayList<>(Arrays.asList(FieldType.values()));
		allTypes.remove(FieldType.FILE);
		allTypes.remove(FieldType.FILE_MULTIPLE);
		for (FieldType fieldType : allTypes) {
			Assert.assertFalse(isFile(fieldType));
		}
		Assert.assertTrue(isFile(FieldType.FILE));
		Assert.assertTrue(isFile(FieldType.FILE_MULTIPLE));

		Assert.assertTrue(isListType(FieldType.LIST_CHECKBOX));
		Assert.assertTrue(isListType(FieldType.LIST_RADIO));
		Assert.assertTrue(isListType(FieldType.LIST_SELECT));
		Assert.assertTrue(isListType(FieldType.LIST_TEXT));
	}

	@Test
	public void testCanConvert() {
		Assert.assertTrue(canConvert(String.class, Integer.class));
	}

	@Test
	public void testConvert() {
		Integer expected = 4;
		Assert.assertEquals(expected, convert("4", Integer.class));
	}

	@Test
	public void testConvertDefaultValue() {
		Integer expected = 4;
		Assert.assertEquals(expected, convert(null, Integer.class, 4));
	}

	public void testGetBindObjectNoClass() throws BusinessException {
		Request request = Mockito.mock(Request.class);
		MetaData metaData = new MetaData();
		Object instance = getBindObject(new FieldProcessorImpl("foo", metaData), request, getClass().getClassLoader());
		Assert.assertNull(instance);
	}

	@Test(expected = BusinessException.class)
	public void testGetBindObjectIsInterface() throws BusinessException {
		Request request = Mockito.mock(Request.class);
		MetaData metaData = new MetaData();
		metaData.setBindClass(List.class.getName());
		getBindObject(new FieldProcessorImpl("foo", metaData), request, getClass().getClassLoader());

	}

	@Test(expected = BusinessException.class)
	public void testGetBindObjectClassNotFound() throws BusinessException {
		Request request = Mockito.mock(Request.class);
		MetaData metaData = new MetaData();
		metaData.setBindClass("foo.bar");
		getBindObject(new FieldProcessorImpl("foo", metaData), request, getClass().getClassLoader());
	}

	@Test
	public void testGetBindObjectInnerClass() throws BusinessException {
		MetaData metaData = new MetaData();
		FieldDef field = new FieldDef();
		field.setName("name");
		field.setType(FieldType.TEXT);
		field.setBinding("name");
		metaData.getFields().add(field);

		Request request = Mockito.mock(Request.class);
		addParameter(request, "name", "Antilles");

		metaData.setBindClass(getClass().getName() + "." + InnerClass.class.getSimpleName());
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Object bindObject = getBindObject(new FieldProcessorImpl("test", metaData), request, contextClassLoader);
		Assert.assertEquals(InnerClass.class, bindObject.getClass());
		Assert.assertEquals("Antilles", ((InnerClass) bindObject).getName());
		try {
			new DefaultValidationProvider().addValidationMetaData(metaData, contextClassLoader);
		} catch (ClassNotFoundException e) {
			throw new BusinessException(e);
		}

		metaData.setBindClass(InnerStaticClass.class.getName());
		bindObject = getBindObject(new FieldProcessorImpl("test", metaData), request, contextClassLoader);
		Assert.assertEquals(InnerStaticClass.class, bindObject.getClass());
		Assert.assertEquals("Antilles", ((InnerStaticClass) bindObject).getName());

	}

	@Test(expected = BusinessException.class)
	public void testGetBindObjectInnerProtectedClass() throws BusinessException {
		MetaData metaData = new MetaData();
		metaData.setBindClass(InnerProtectedClass.class.getName());
		Request request = Mockito.mock(Request.class);
		getBindObject(new FieldProcessorImpl("test", metaData), request, getClass().getClassLoader());
	}

	static class InnerStaticClass {
		private String name;

		@NotNull
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public class InnerClass extends InnerStaticClass {

		@NotNull
		@Override
		public String getName() {
			return super.getName();
		}
	}

	class InnerProtectedClass {

	}

	@Test
	public void testGetBindObject() throws BusinessException {
		Request request = Mockito.mock(Request.class);
		addParameter(request, "name", "Antilles");
		addParameter(request, "firstname", "Wedge");
		addParameters(request, "integerList", "1", "2", "3", "4", "5");
		addParameter(request, "strings", "foobar");
		addParameter(request, "birthDate", "2256.04.30");
		addParameter(request, "savings", "123456,789");
		addParameter(request, "coordinate.latitude", "987654,321");
		addParameter(request, "coordinate.longitude", "123456,789");
		addParameter(request, "father", "father");
		addParameter(request, "description", "description");

		FormUpload formUploadBean = new FormUploadBean(new File(""), "antilles.jpg", "image/jpeg", null, 1024L);
		Mockito.when(request.getFormUploads("picture")).thenReturn(Arrays.asList(formUploadBean));

		FormUpload fu2 = new FormUploadBean(new File(""), "luke.jpg", "image/jpeg", null, 1024L);
		FormUpload fu3 = new FormUploadBean(new File(""), "xwing.jpg", "image/jpeg", null, 1024L);
		List<FormUpload> morePictures = Arrays.asList(fu2, fu3);
		Mockito.when(request.getFormUploads("morePictures")).thenReturn(morePictures);

		MetaData metaData = MetaDataProvider.getMetaData();
		FieldDef readonlyField = MetaDataProvider.getField("description", FieldType.LONGTEXT);
		readonlyField.setReadonly("tRue");
		metaData.getFields().add(readonlyField);
		FieldProcessor fp = new FieldProcessorImpl("action", metaData);
		Object bindObject = getBindObject(fp, request, getClass().getClassLoader());
		Assert.assertNotNull(bindObject);
		Assert.assertEquals(Person.class, bindObject.getClass());
		Person person = (Person) bindObject;
		Assert.assertEquals("Antilles", person.getName());
		Assert.assertEquals(null, person.getDescription());
		Assert.assertEquals("Wedge", person.getFirstname());
		Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), person.getIntegerList());
		Assert.assertEquals(formUploadBean, person.getPicture());
		Assert.assertEquals(morePictures, person.getMorePictures());
		Assert.assertEquals(1, person.getStrings().size());
		Assert.assertEquals("foobar", person.getStrings().get(0));
		Assert.assertEquals(new Coordinate(987654.321d, 123456.789d), person.getCoordinate());
		Assert.assertEquals(MetaDataProvider.SDF.format(person.getBirthDate()), "2256.04.30");
		Assert.assertEquals(123456.789D, person.getSavings(), 0.0d);
	}

	private void addParameter(Request request, String name, String value) {
		Mockito.when(request.getParameterList(name)).thenReturn(Arrays.asList(value));
		Mockito.when(request.getParameter(name)).thenReturn(value);
	}

	private void addParameters(Request request, String name, String... values) {
		Mockito.when(request.getParameterList(name)).thenReturn(Arrays.asList(values));
	}

	@Test
	public void testGetBindObjectNullValues() throws BusinessException {
		addParameter(request, "name", "");
		addParameter(request, "firstname", "");
		addParameter(request, "strings", "");
		addParameter(request, "birthDate", "");
		addParameter(request, "savings", "");
		addParameter(request, "father", "");
		addParameter(request, "age", "");

		FieldProcessor fp = new FieldProcessorImpl("action", MetaDataProvider.getMetaData());
		Object bindObject = getBindObject(fp, request, getClass().getClassLoader());
		Assert.assertNotNull(bindObject);
		Assert.assertEquals(Person.class, bindObject.getClass());
		Person person = (Person) bindObject;
		Assert.assertEquals("", person.getName());
		Assert.assertEquals(null, person.getAge());
		Assert.assertEquals("", person.getFirstname());
		Assert.assertEquals(Arrays.asList(), person.getIntegerList());
		Assert.assertEquals(null, person.getPicture());
		Assert.assertEquals(Collections.emptyList(), person.getMorePictures());
		Assert.assertEquals(1, person.getStrings().size());
		Assert.assertEquals("", person.getStrings().get(0));
		Assert.assertEquals(null, person.getBirthDate());
		Assert.assertEquals(null, person.getSavings());
	}

	@Test
	public void testGetBindObjectInvalidFormat() throws BusinessException {
		addParameter(request, "birthDate", "4556");
		addParameter(request, "savings", "asdasd");
		addParameter(request, "age", "asdasd");
		Mockito.when(env.getLocale()).thenReturn(Locale.GERMANY);
		FieldProcessor fp = new FieldProcessorImpl("action", MetaDataProvider.getMetaData());
		fp.getField("age").setCondition(null);

		Object bindObject = getBindObject(fp, request, getClass().getClassLoader());
		Assert.assertNotNull(bindObject);
		Assert.assertEquals(Person.class, bindObject.getClass());
		Person person = (Person) bindObject;

		Assert.assertEquals(null, person.getBirthDate());
		Assert.assertEquals(null, person.getSavings());
		Assert.assertEquals(null, person.getAge());

		FieldDef field = fp.getField("savings");
		Messages messages = field.getMessages();
		List<Message> m1 = messages.getMessageList();
		Assert.assertEquals(1, m1.size());
		Assert.assertEquals("invalid.digit", m1.get(0).getContent());

		List<Message> m2 = fp.getField("birthDate").getMessages().getMessageList();
		Assert.assertEquals(1, m2.size());
		Assert.assertEquals("invalid.date", m2.get(0).getContent());

		List<Message> m3 = fp.getField("age").getMessages().getMessageList();
		Assert.assertEquals(1, m3.size());
		Assert.assertEquals("invalid.integer", m3.get(0).getContent());
	}

	@Test
	public void testSetPropertyValue() {
		Person p1 = new Person();
		p1.setFirstname("johndoe");
		Person p2 = new Person();
		setPropertyValue(p1, p2, "firstname");
		Assert.assertEquals(p1.getFirstname(), p2.getFirstname());
	}

	@Test
	public void testSetPropertyValueError() {
		Person p1 = new Person();
		p1.setFirstname("johndoe");
		Person p2 = new Person();
		setPropertyValue(p1, p2, "firstname");
		Assert.assertEquals(p1.getFirstname(), p2.getFirstname());
	}

	@Test
	public void testSetPropertyValues() {
		Person p1 = new Person();
		p1.setFirstname("John");
		p1.setName("Doe");
		p1.setSize(1.83f);
		p1.setSavings(978d);

		MetaData metaData = new MetaData();
		metaData.setBindClass(Person.class.getName());
		List<FieldDef> fields = metaData.getFields();
		fields.add(MetaDataProvider.getField("firstname", FieldType.TEXT));
		fields.add(MetaDataProvider.getField("name", FieldType.TEXT));
		fields.add(MetaDataProvider.getField("savings", FieldType.DECIMAL, "${current.savings > 1000}"));
		fields.add(MetaDataProvider.getField("size", FieldType.DECIMAL, "${current.size > 1.8}"));

		Person p2 = new Person();
		setPropertyValues(p1, p2, metaData);
		Assert.assertEquals(p1.getFirstname(), p2.getFirstname());
		Assert.assertEquals(p1.getName(), p2.getName());
		Assert.assertEquals(p1.getSize(), p2.getSize());
		Assert.assertEquals(null, p2.getSavings());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetPropertyValuesWrongSourceClass() {
		MetaData metaData = new MetaData();
		metaData.setBindClass(Person.class.getName());
		setPropertyValues("foo", new Person(), metaData);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetPropertyValuesWrongTargetClass() {
		MetaData metaData = new MetaData();
		metaData.setBindClass(Person.class.getName());
		setPropertyValues(new Person(), "foo", metaData);
	}

	@Test
	public void testConvertEmptyDate() throws Exception {
		FieldDef field = getField(FieldType.DATE, "birthDate", "yyyy-MM-dd");
		FieldWrapper fieldWrapper = new FieldWrapper(field, new BeanWrapperImpl(new Person()));
		addParameter(request, "birthDate", "");
		fieldConverter.setObject(fieldWrapper, request);
		assertResult(fieldWrapper);
	}

	@Test
	public void testConvertEmptyInt() throws Exception {
		FieldDef field = getField(FieldType.INT, "id", "###");
		FieldWrapper fieldWrapper = new FieldWrapper(field, new BeanWrapperImpl(new Person()));
		addParameter(request, "id", "");
		fieldConverter.setObject(fieldWrapper, request);
		assertResult(fieldWrapper);
	}

	@Test
	public void testConvertEmptyLong() throws Exception {
		FieldDef field = getField(FieldType.LONG, "age", "###");
		FieldWrapper fieldWrapper = new FieldWrapper(field, new BeanWrapperImpl(new Person()));
		addParameter(request, "age", "");
		fieldConverter.setObject(fieldWrapper, request);
		assertResult(fieldWrapper);
	}

	@Test
	public void testConvertInvalidLong() throws ConversionFailedException, ParseException, BusinessException {
		FieldDef field = getField(FieldType.LONG, "age", "###");
		FieldWrapper fieldWrapper = new FieldWrapper(field, new BeanWrapperImpl(new Person()));
		addParameter(request, "age", "assddfdf");
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertNull(fieldWrapper.getObject());
		String content = field.getMessages().getMessageList().get(0).getContent();
		Assert.assertEquals("invalid.integer", content);
	}

	@Test
	public void testConvertEmptyDecimal() throws Exception {
		FieldDef field = getField(FieldType.DECIMAL, "savings", "#.#");
		FieldWrapper fieldWrapper = new FieldWrapper(field, new BeanWrapperImpl(new Person()));
		addParameter(request, "savings", "");
		fieldConverter.setObject(fieldWrapper, request);
		assertResult(fieldWrapper);
	}

	private void assertResult(FieldWrapper field) throws ConversionFailedException, ParseException, BusinessException {
		Object result = field.getObject();
		Assert.assertNull(result);
		Assert.assertNull(field.getMessages());
	}

	private FieldDef getField(FieldType type, String name, String format) {
		FieldDef fieldDef = new FieldDef();
		fieldDef.setType(type);
		fieldDef.setFormat(format);
		fieldDef.setName(name);
		fieldDef.setBinding(name);
		return fieldDef;
	}
}
