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

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * 
 * A {@link FieldConverter} for {@link FieldDef}initions of type {@link FieldType#DATE}.
 * 
 * @author Matthias Müller
 * 
 */
class DateFieldConverter extends ConverterBase {

	protected static final Logger LOG = LoggerFactory.getLogger(DateFieldConverter.class);
	static final String ERROR_KEY = "invalid.date";
	protected static final String DEFAULT_DATEPATTERN = "yyyy-MM-dd HH:mm:ss";

	DateFieldConverter(Environment environment, MessageSource messageSource) {
		this.environment = environment;
		this.messageSource = messageSource;
	}

	@Override
	public void setString(FieldWrapper field) {
		Object object = field.getObject();
		setFormat(field);
		if (null != object) {
			Date date = null;
			if (object instanceof Date) {
				date = (Date) object;
			} else if (object instanceof DateTime) {
				date = ((DateTime) object).toDate();
			}
			if (null != date) {
				String result = getDateFormat(field).format(date);
				field.setStringValue(result);
				logSetString(field);
			} else {
				throw new IllegalArgumentException("error getting String from field '" + field.getName()
						+ "', expected instance of " + Date.class.getName() + " or " + DateTime.class + " but was "
						+ object.getClass().getName());

			}
		}
	}

	@Override
	public void setObject(FieldWrapper field, RequestContainer request) {
		String value = request.getParameter(field.getBinding());
		Date date = null;
		if (StringUtils.isNotBlank(value)) {
			try {
				setFormat(field);
				date = getDateFormat(field).parse(value);
			} catch (ParseException e) {
				handleException(field, ERROR_KEY);
			}
			logSetObject(field, date);
			if (null != date && DateTime.class.equals(field.getTargetClass())) {
				field.setObject(new DateTime(date));
			} else {
				field.setObject(date);
			}

		}
	}

	protected SimpleDateFormat getDateFormat(FieldDef field) {
		SimpleDateFormat dateFormat = new SimpleDateFormat();
		dateFormat.applyPattern(field.getFormat());
		dateFormat.setDateFormatSymbols(DateFormatSymbols.getInstance(environment.getLocale()));
		dateFormat.setTimeZone(environment.getTimeZone());
		return dateFormat;
	}

	protected void setFormat(FieldDef field) {
		if (StringUtils.isBlank(field.getFormat())) {
			field.setFormat(DEFAULT_DATEPATTERN);
		}
	}

	@Override
	protected Logger getLog() {
		return LOG;
	}

}
