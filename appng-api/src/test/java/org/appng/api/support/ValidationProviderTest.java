/*
 * Copyright 2011-2017 the original author or authors.
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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;

import org.appng.api.AbstractTest;
import org.appng.api.FieldProcessor;
import org.appng.api.MetaDataProvider;
import org.appng.api.Person.GroupA;
import org.appng.api.ValidationProvider;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.api.support.validation.LocalizedMessageInterpolator;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

/**
 * Test for {@link DefaultValidationProvider}.
 * 
 * @author Gajanan Nilwarn
 * 
 */
public class ValidationProviderTest extends AbstractTest {

	private static final String MUST_NOT_BE_NULL = "must not be null";
	private ValidationProvider validationProvider;

	@Before
	public void setup() {
		MessageSource messageSource = getMessageSource();
		validationProvider = new DefaultValidationProvider(
				new LocalizedMessageInterpolator(Locale.ENGLISH, messageSource), messageSource, Locale.ENGLISH);
	}

	@Test
	public void testAddValidationMetaDataInnerClass() throws Exception {
		MetaData metaData = new MetaData();
		metaData.setBindClass(getClass().getName() + "." + InnerClass.class.getSimpleName());
		FieldDef field = new FieldDef();
		metaData.getFields().add(field);
		field.setType(FieldType.TEXT);
		field.setName("foo");
		field.setBinding("foo");

		URLClassLoader classLoader = new URLClassLoader(new URL[0]);
		Mockito.when(site.getSiteClassLoader()).thenReturn(classLoader);
		validationProvider.addValidationMetaData(metaData, classLoader);
		XmlValidator.validate(metaData);
	}

	public class InnerClass {
		private String foo;

		@NotNull
		@Size(min = 5)
		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

	@Test
	public void testAddValidationMetaData() throws Exception {
		MetaData metaData = MetaDataProvider.getMetaData();
		FieldDef name = MetaDataProvider.getField("offsprings[0].name", FieldType.TEXT);
		FieldDef firstname = MetaDataProvider.getField("offsprings[0].firstname", FieldType.TEXT);
		FieldDef nameFromMap = MetaDataProvider.getField("offspringNames['Han'].name", FieldType.TEXT);
		metaData.getFields().add(name);
		metaData.getFields().add(firstname);
		metaData.getFields().add(nameFromMap);
		URLClassLoader classLoader = new URLClassLoader(new URL[0]);
		Mockito.when(site.getSiteClassLoader()).thenReturn(classLoader);
		XmlValidator.validate(metaData, "-before");
		validationProvider.addValidationMetaData(metaData, classLoader, Default.class, GroupA.class);
		XmlValidator.validate(metaData);
	}

	@Test
	public void testAddValidationMetaDataAsRule() throws Exception {
		MetaData metaData = MetaDataProvider.getMetaData();
		FieldDef email = MetaDataProvider.getField("email", FieldType.TEXT);
		metaData.getFields().add(email);
		URLClassLoader classLoader = new URLClassLoader(new URL[0]);
		Mockito.when(site.getSiteClassLoader()).thenReturn(classLoader);
		XmlValidator.validate(metaData, "-before");
		MessageSource messageSource = getMessageSource();
		validationProvider = new DefaultValidationProvider(
				new LocalizedMessageInterpolator(Locale.ENGLISH, messageSource), messageSource, Locale.ENGLISH, true);
		validationProvider.addValidationMetaData(metaData, classLoader, Default.class, GroupA.class);
		XmlValidator.validate(metaData);
	}

	@Test
	public void testAddValidationMetaDataWithGroup() throws Exception {
		MetaData metaData = MetaDataProvider.getMetaData();
		URLClassLoader classLoader = new URLClassLoader(new URL[0]);
		Mockito.when(site.getSiteClassLoader()).thenReturn(classLoader);
		validationProvider.addValidationMetaData(metaData, classLoader, GroupA.class);
		XmlValidator.validate(metaData);
	}

	@Test
	public void testValidateBean() {
		MetaData metaData = new MetaData();
		FieldDef field = new FieldDef();
		field.setBinding("name");
		metaData.getFields().add(field);
		FieldDef errorField = new FieldDef();
		errorField.setBinding("keepImport['gender']");
		metaData.getFields().add(errorField);
		FieldProcessorImpl fp = new FieldProcessorImpl("test", metaData);
		Foobar foobar = new Foobar();
		validationProvider.validateBean(foobar, fp);
		Assert.assertTrue("should have one error", fp.hasErrors());
		Messages messages = fp.getField("name").getMessages();
		Assert.assertEquals(1, messages.getMessageList().size());
		String content = messages.getMessageList().get(0).getContent();
		Assert.assertEquals(MUST_NOT_BE_NULL, content);

		FieldProcessorImpl fp2 = new FieldProcessorImpl("test", new MetaData());
		validationProvider.validateBean(foobar, fp2);
		Assert.assertFalse("should have no errors", fp2.hasErrors());
		Assert.assertEquals(null, fp2.getField("name"));
	}

	@Test
	public void testValidateField() {
		MetaData metaData = new MetaData();
		FieldDef fieldDef = new FieldDef();
		fieldDef.setBinding("name");
		metaData.getFields().add(fieldDef);
		FieldProcessor fp = new FieldProcessorImpl("bla", metaData);
		validationProvider.validateField(new Foobar(), fp, "name");
		Assert.assertTrue("should have 1 error", fp.hasErrors());
		Assert.assertEquals(MUST_NOT_BE_NULL, fieldDef.getMessages().getMessageList().get(0).getContent());
	}

	@Test
	public void testValidateFieldExclude() {
		MetaData metaData = new MetaData();
		FieldDef fieldDef = new FieldDef();
		fieldDef.setBinding("name");
		metaData.getFields().add(fieldDef);
		FieldProcessor fp = new FieldProcessorImpl("bla", metaData);
		validationProvider.validateBean(new Foobar(), fp, new String[] { "name" });
		Assert.assertFalse(fp.hasErrors());
		Assert.assertNull(fieldDef.getMessages());
	}

	class Foobar {
		String name;

		@NotNull
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}
}
