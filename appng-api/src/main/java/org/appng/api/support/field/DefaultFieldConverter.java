/*
 * Copyright 2011-2018 the original author or authors.
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
import org.appng.tools.ui.StringNormalizer;
import org.appng.xml.platform.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;

/**
 * Default {@link FieldConverter}-implementation.
 * 
 * @author Matthias Müller
 * 
 */
class DefaultFieldConverter extends ConverterBase {

	protected static final Logger LOG = LoggerFactory.getLogger(DefaultFieldConverter.class);

	DefaultFieldConverter(ExpressionEvaluator expressionEvaluator, ConversionService conversionService,
			Environment environment, MessageSource messageSource) {
		this.environment = environment;
		this.messageSource = messageSource;
		this.conversionService = conversionService;
		this.expressionEvaluator = expressionEvaluator;
	}

	public void setObject(FieldWrapper field, RequestContainer request) {
		String value = stripNonPrintableCharacter(request.getParameter(field.getBinding()));
		Object object = null;
		Class<?> targetClass = field.getTargetClass();
		if (null != targetClass) {
			boolean notBlank = StringUtils.isNotBlank(value);
			if (!targetClass.isPrimitive() || notBlank) {
				object = conversionService.convert(value, targetClass);
				Object logValue = object;
				if (notBlank && FieldType.PASSWORD.equals(field.getType())) {
					logValue = value.replaceAll(".", "*");
				}
				logSetObject(field, logValue);
				field.setObject(object);
			}
		}
	}
	
	static String stripNonPrintableCharacter(String value) {
		return StringNormalizer.removeNonPrintableCharacters(value);
	}

	protected Logger getLog() {
		return LOG;
	}

}
