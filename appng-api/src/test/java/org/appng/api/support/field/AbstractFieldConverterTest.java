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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.api.FieldWrapper;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Linkpanel;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

public abstract class AbstractFieldConverterTest {

	protected static final String OBJECT = "object";

	@Mock
	protected RequestContainer request;

	@Mock
	protected MessageSource messageSource;

	@Mock
	protected Environment environment;
	protected BeanWrapper beanWrapper;
	protected FieldWrapper fieldWrapper;
	protected FieldConverter fieldConverter;

	protected FieldDef field;

	protected void setup(FieldType type) throws Exception {
		setup(type, new HashMap<>());
	}

	protected void setup(FieldType type, Map<String, Object> params) throws Exception {
		MockitoAnnotations.initMocks(this);
		Mockito.when(environment.getLocale()).thenReturn(Locale.ENGLISH);
		Mockito.when(environment.getTimeZone()).thenReturn(TimeZone.getDefault());
		this.beanWrapper = new BeanWrapperImpl(getContainer());
		this.fieldWrapper = getFieldWrapper(type);

		ConversionService conversionService = new DefaultConversionService();
		ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(params);
		FieldConversionFactory conversionFactory = new FieldConversionFactory(expressionEvaluator);
		conversionFactory.setConversionService(conversionService);
		conversionFactory.setEnvironment(environment);
		conversionFactory.setMessageSource(messageSource);
		conversionFactory.afterPropertiesSet();
		this.fieldConverter = conversionFactory;
	}

	protected DatafieldOwner getDatafieldOwner() {
		return new DatafieldOwner() {
			private List<Datafield> fields = new ArrayList<>();
			private List<Linkpanel> linkpanels = new ArrayList<>();

			public List<Linkpanel> getLinkpanels() {
				return linkpanels;
			}

			public List<Datafield> getFields() {
				return fields;
			}
		};
	}

	public abstract void testAddField() throws Exception;

	public abstract Container<?> getContainer();

	public abstract void testSetObject() throws Exception;

	public abstract void testSetObjectEmptyValue() throws Exception;

	public abstract void testSetObjectInvalidValue() throws Exception;

	public abstract void testSetObjectNull() throws Exception;

	public abstract void testSetString() throws Exception;

	public abstract void testSetStringNullObject() throws Exception;

	public abstract void testSetStringInvalidType() throws Exception;

	private FieldWrapper getFieldWrapper(FieldType type) {
		this.field = new FieldDef();
		field.setBinding(OBJECT);
		field.setName(OBJECT);
		field.setType(type);
		FieldWrapper fieldWrapper = new FieldWrapper(field, beanWrapper);
		return fieldWrapper;
	}

	public abstract class Container<T> {
		private T object;

		public T getObject() {
			return object;
		}

		public void setObject(T object) {
			this.object = object;
		}
	}

}
