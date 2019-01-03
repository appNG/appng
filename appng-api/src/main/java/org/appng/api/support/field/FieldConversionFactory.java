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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Linkpanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;

/**
 * A {@link FieldConverter} encapsulating all the other {@link FieldConverter}s, thus providing the ability to convert
 * any {@link FieldType}.
 * 
 * @author Matthias Müller
 * 
 */
public class FieldConversionFactory implements FieldConverter, InitializingBean {

	private static final Logger LOG = LoggerFactory.getLogger(FieldConversionFactory.class);
	private Map<FieldType, FieldConverter> converters = new HashMap<FieldType, FieldConverter>();

	private Environment environment;
	private MessageSource messageSource;
	private ConversionService conversionService;
	private ExpressionEvaluator expressionEvaluator;

	public FieldConversionFactory() {

	}

	public FieldConversionFactory(ExpressionEvaluator expressionEvaluator) {
		this.expressionEvaluator = expressionEvaluator;
	}

	public void reset(FieldWrapper field) {
		FieldConverter converter = getConverter(field);
		if (null != converter) {
			converter.reset(field);
			log(field, converter);
		}
	}

	public Datafield addField(DatafieldOwner dataFieldOwner, FieldWrapper fieldWrapper) {

		Condition condition = fieldWrapper.getCondition();
		boolean addField = true;
		if (condition != null) {
			addField = expressionEvaluator.evaluate(condition.getExpression());
			condition.setExpression(String.valueOf(addField));
		}

		fieldWrapper.setReadonly(expressionEvaluator.getString(fieldWrapper.getReadonly()));
		fieldWrapper.setHidden(expressionEvaluator.getString(fieldWrapper.getHidden()));

		if (addField) {
			FieldConverter converter = getConverter(fieldWrapper);
			Datafield datafield = null;
			if (null == converter) {
				datafield = new Datafield();
				datafield.setName(fieldWrapper.getName());
				dataFieldOwner.getFields().add(datafield);
			} else {
				log(fieldWrapper, converter);
				datafield = converter.addField(dataFieldOwner, fieldWrapper);
			}
			if (null != datafield) {
				addChildFields(fieldWrapper, datafield, fieldWrapper.getBeanWrapper());
			} else {
				LOG.debug("datafield '{}' is null", fieldWrapper.getBinding());
			}
			return datafield;
		}
		return null;
	}

	private void log(FieldWrapper fieldWrapper, FieldConverter converter) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("converter for field '{}' of type {} is {}", fieldWrapper.getBinding(), fieldWrapper.getType(),
					converter.getClass().getName());
		}
	}

	private void addChildFields(final FieldDef fieldDef, final Datafield datafield, BeanWrapper beanWrapper) {
		final List<FieldDef> childFields = fieldDef.getFields();

		if (!childFields.isEmpty()) {

			DatafieldOwner dataFieldOwner = new DatafieldOwner() {

				public List<Linkpanel> getLinkpanels() {
					return null;
				}

				public List<Datafield> getFields() {
					return datafield.getFields();
				}
			};
			for (final FieldDef childField : childFields) {
				FieldWrapper fieldWrapper = new FieldWrapper(childField, beanWrapper);
				fieldWrapper.backupFields();
				LOG.debug("adding child field '{}', type: {}", fieldWrapper.getBinding(), fieldWrapper.getType());
				addField(dataFieldOwner, fieldWrapper);
				fieldWrapper.restoreFields();
			}
		}
	}

	public void setString(FieldWrapper field) {
		FieldConverter fieldConverter = getConverter(field);
		if (null != fieldConverter) {
			log(field, fieldConverter);
			fieldConverter.setString(field);
		}
		List<FieldDef> childFields = field.getFields();
		for (FieldDef childField : childFields) {
			FieldWrapper childWrapper = new FieldWrapper(childField, field.getBeanWrapper());
			setString(childWrapper);
		}
	}

	public void setObject(FieldWrapper field, RequestContainer request) {
		FieldConverter fieldConverter = getConverter(field);
		if (null != fieldConverter) {
			log(field, fieldConverter);
			fieldConverter.setObject(field, request);
		}
		List<FieldDef> childFields = field.getFields();
		for (FieldDef childField : childFields) {
			FieldWrapper childWrapper = new FieldWrapper(childField, field.getBeanWrapper());
			setObject(childWrapper, request);
		}
	}

	public void afterPropertiesSet() {
		converters.put(FieldType.DATE, new DateFieldConverter(environment, messageSource));
		converters.put(FieldType.INT, new IntegerFieldConverter(environment, messageSource));
		converters.put(FieldType.LONG, new IntegerFieldConverter(environment, messageSource));
		converters.put(FieldType.DECIMAL, new DecimalFieldConverter(environment, messageSource));
		FieldConverter fileConverter = new FileFieldConverter(conversionService);
		converters.put(FieldType.FILE, fileConverter);
		converters.put(FieldType.FILE_MULTIPLE, fileConverter);
		FieldConverter defaultConverter = new DefaultFieldConverter(expressionEvaluator, conversionService,
				environment, messageSource);
		converters.put(FieldType.TEXT, defaultConverter);
		converters.put(FieldType.PASSWORD, defaultConverter);
		converters.put(FieldType.LONGTEXT, defaultConverter);
		converters.put(FieldType.RICHTEXT, defaultConverter);
		converters.put(FieldType.CHECKBOX, defaultConverter);
		converters.put(FieldType.IMAGE, defaultConverter);
		converters.put(FieldType.URL, defaultConverter);

		FieldConverter listFieldConverter = new ListFieldConverter(conversionService);

		converters.put(FieldType.LIST_CHECKBOX, listFieldConverter);
		converters.put(FieldType.LIST_RADIO, listFieldConverter);
		converters.put(FieldType.LIST_SELECT, listFieldConverter);
		converters.put(FieldType.LIST_TEXT, listFieldConverter);
		converters.put(FieldType.LIST_OBJECT, listFieldConverter);
		converters.put(FieldType.LINKPANEL, new LinkPanelFieldHandler(expressionEvaluator, environment, messageSource));
		converters.put(FieldType.COORDINATE, new CoordinateFieldConverter());
		converters.put(FieldType.OBJECT, new ObjectFieldConverter());
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public ConversionService getConversionService() {
		return conversionService;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	private FieldConverter getConverter(FieldWrapper fieldWrapper) {
		return converters.get(fieldWrapper.getType());
	}
}
