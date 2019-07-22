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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.springframework.beans.PropertyAccessException;
import org.springframework.context.MessageSource;

/**
 * 
 * Base {@link FieldConverter} for {@link FieldDef}initions of type {@link FieldType#INT} and {@link FieldType#DECIMAL}
 * .
 * 
 * @author Matthias Müller
 * 
 */
abstract class NumberFieldConverter extends ConverterBase {

	private String defaultPattern;
	private String errorKey;

	public NumberFieldConverter(Environment environment, MessageSource messageSource, String defaultPattern,
			String errorKey) {
		this.environment = environment;
		this.defaultPattern = defaultPattern;
		this.messageSource = messageSource;
		this.errorKey = errorKey;
	}

	@Override
	public void setString(FieldWrapper field) {
		Object object = field.getObject();
		setFormat(field);
		if (null != object) {
			if (!(object instanceof Number)) {
				throw new IllegalArgumentException("error getting String from field '" + field.getName()
						+ "', expected instance of " + Number.class.getName() + " but was "
						+ object.getClass().getName());
			}
			String number = getNumberFormat(field).format(object);
			field.setStringValue(number);
			logSetString(field);
		}
	}

	@Override
	public void setObject(FieldWrapper field, RequestContainer request) {
		String value = request.getParameter(field.getBinding());
		setFormat(field);
		Number number = null;
		if (StringUtils.isNotBlank(value)) {
			try {
				number = getNumberFormat(field).parse(value);
			} catch (ParseException e) {
				handleException(field, errorKey);
			}
		}
		try {
			field.setObject(number);
			logSetObject(field, number);
		} catch (PropertyAccessException e) {
			field.setStringValue(value);
			handleException(field, errorKey);
		}
	}

	protected NumberFormat getNumberFormat(FieldDef field) {
		Locale locale = environment.getLocale();
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
		DecimalFormat decimalFormat = new DecimalFormat(field.getFormat(), symbols);
		return decimalFormat;
	}

	private void setFormat(FieldDef field) {
		if (StringUtils.isBlank(field.getFormat())) {
			field.setFormat(defaultPattern);
		}
	}

}
