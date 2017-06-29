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
package org.appng.api.support.field;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Icon;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;

/**
 * Basic {@link FieldConverter}-implementation to be extended by {@link FieldConverter}s for concrete {@link FieldType}
 * s.
 * 
 * @author Matthias Müller
 * 
 */
abstract class ConverterBase implements FieldConverter {

	protected Environment environment;
	protected ConversionService conversionService;
	protected MessageSource messageSource;
	protected ExpressionEvaluator expressionEvaluator;

	protected void handleException(FieldWrapper field, String key) {
		getLog().warn("error in field {}: can not convert '{}' to type '{}' using format '{}'", field.getBinding(), field.getStringValue(),
				field.getTargetClass().getName(), field.getFormat());
		Message message = new Message();
		message.setClazz(MessageType.ERROR);
		message.setRef(field.getName());
		Object[] args = new Object[0];
		String errorMessage = messageSource.getMessage(key, args, environment.getLocale());
		message.setContent(errorMessage);
		Messages messages = new Messages();
		messages.getMessageList().add(message);
		field.setMessages(messages);
		field.setStringValue(null);
	}

	public void reset(FieldWrapper field) {
		// may be overridden by subclasses
	}

	public Datafield addField(DatafieldOwner dataFieldOwner, FieldWrapper fieldWrapper) {
		Datafield datafield = createDataField(fieldWrapper);
		dataFieldOwner.getFields().add(datafield);
		setString(fieldWrapper);
		String stringValue = fieldWrapper.getStringValue();
		if (null == stringValue) {
			stringValue = "";
		}
		datafield.setValue(stringValue);
		return datafield;
	}

	protected Datafield createDataField(FieldWrapper fieldWrapper) {
		final Datafield datafield = new Datafield();
		datafield.setName(fieldWrapper.getName());
		datafield.setType(fieldWrapper.getType());
		setIcons(fieldWrapper, datafield);
		return datafield;
	}

	public void setString(FieldWrapper field) {
		Object object = field.getObject();
		if (null != object) {
			if (conversionService.canConvert(object.getClass(), String.class)) {
				String value = conversionService.convert(object, String.class);
				field.setStringValue(value);
				logSetString(field);
			} else {
				getLog().debug("can not convert from {} to {}", object.getClass(), String.class);
			}
		}
	}

	public void setObject(FieldWrapper field, RequestContainer request) {
		// may be overridden by subclasses
	}

	protected void setIcons(FieldWrapper fieldWrapper, final Datafield datafield) {
		for (Icon icon : fieldWrapper.getIcons()) {
			String iconCondition = icon.getCondition();
			if (StringUtils.isBlank(iconCondition) || expressionEvaluator.evaluate(iconCondition)) {
				Icon iconCopy = new Icon();
				iconCopy.setContent(icon.getContent());
				iconCopy.setType(icon.getType());
				datafield.getIcons().add(iconCopy);
			}
		}
	}

	protected void logSetObject(FieldWrapper wrapper, Object logValue) {
		Class<?> wrappedClass = wrapper.getBeanWrapper().getWrappedClass();
		Class<?> targetClass = wrapper.getTargetClass();
		debug("setting property '{}' on instance of '{}' to value '{}' (type: {})", wrapper.getBinding(),
				wrappedClass.getName(), logValue, targetClass.getName());
	}

	protected void logSetString(FieldWrapper wrapper) {
		Class<?> wrappedClass = wrapper.getBeanWrapper().getWrappedClass();
		debug("setting string-value for property '{}' on instance of '{}' to '{}'", wrapper.getBinding(),
				wrappedClass.getName(), wrapper.getStringValue());
	}

	private void debug(String message, Object... args) {
		if (getLog().isDebugEnabled()) {
			getLog().debug(message, args);
		}
	}

	protected abstract Logger getLog();

	protected Environment getEnvironment() {
		return environment;
	}

	protected void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	protected MessageSource getMessageSource() {
		return messageSource;
	}

	protected void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	protected ExpressionEvaluator getExpressionEvaluator() {
		return expressionEvaluator;
	}

	protected void setExpressionEvaluator(ExpressionEvaluator expressionEvaluator) {
		this.expressionEvaluator = expressionEvaluator;
	}

	protected ConversionService getConversionService() {
		return conversionService;
	}

	protected void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

}
