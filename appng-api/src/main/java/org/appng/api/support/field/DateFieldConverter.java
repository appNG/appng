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

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.Environment;
import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * A {@link FieldConverter} for {@link FieldDef}initions of type {@link FieldType#DATE}.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
class DateFieldConverter extends ConverterBase {

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
		String result;
		if (null != object) {
			Date date = null;
			if (object instanceof Date) {
				date = (Date) object;
			} else if (object instanceof LocalDate) {
				date = Date.from(LocalDate.class.cast(object).atStartOfDay().atZone(getZoneId()).toInstant());
			} else if (object instanceof LocalDateTime) {
				date = Date.from(LocalDateTime.class.cast(object).atZone(getZoneId()).toInstant());
			} else if (object instanceof OffsetDateTime) {
				date = Date.from(OffsetDateTime.class.cast(object).toInstant());
			} else if (object instanceof ZonedDateTime) {
				date = Date.from(ZonedDateTime.class.cast(object).toInstant());
			} else if (object instanceof org.joda.time.DateTime) {
				date = (org.joda.time.DateTime.class.cast(object)).toDate();
			} else if (object instanceof org.joda.time.LocalDate) {
				date = (org.joda.time.LocalDate.class.cast(object)).toDate();
			} else if (object instanceof org.joda.time.LocalDateTime) {
				date = (org.joda.time.LocalDateTime.class.cast(object)).toDate(environment.getTimeZone());
			} else {
				throw new IllegalArgumentException(String.format("Unsupported type '%s' for field '%s' of type '%s'!",
						object.getClass().getName(), field.getBinding(), FieldType.DATE.value()));
			}
			if (null != date) {
				result = getDateFormat(field).format(date);
				field.setStringValue(result);
				logSetString(field);
			}
		}
	}

	@Override
	public void setObject(FieldWrapper field, RequestContainer request) {
		String value = request.getParameter(field.getBinding());
		if (StringUtils.isNotBlank(value)) {
			setFormat(field);
			Class<?> targetClass = field.getTargetClass();

			if (null != targetClass) {
				Object object = null;
				try {
					Date date = getDateFormat(field).parse(value);
					if (Date.class.equals(targetClass)) {
						object = date;
					} else if (Temporal.class.isAssignableFrom(targetClass)) {
						ZonedDateTime zonedDateTime = date.toInstant().atZone(getZoneId());
						if (LocalDate.class.equals(targetClass)) {
							object = zonedDateTime.toLocalDate();
						} else if (LocalDateTime.class.equals(targetClass)) {
							object = zonedDateTime.toLocalDateTime();
						} else if (OffsetDateTime.class.equals(targetClass)) {
							object = zonedDateTime.toOffsetDateTime();
						} else if (ZonedDateTime.class.equals(targetClass)) {
							object = zonedDateTime;
						}
					} else if (org.joda.time.DateTime.class.equals(targetClass)) {
						object = new org.joda.time.DateTime(date);
					} else if (org.joda.time.LocalDate.class.equals(targetClass)) {
						object = org.joda.time.LocalDate.fromDateFields(date);
					} else if (org.joda.time.LocalDateTime.class.equals(targetClass)) {
						object = org.joda.time.LocalDateTime.fromDateFields(date);
					} else {
						LOGGER.warn("Unsupported type '{}' for field '{}' of type '{}'!", targetClass.getName(),
								field.getBinding(), FieldType.DATE.value());
					}
					if (null != object) {
						field.setObject(object);
						logSetObject(field, object);
					}
				} catch (ParseException | DateTimeParseException e) {
					handleException(field, ERROR_KEY);
				}

			}
		}
	}

	private ZoneId getZoneId() {
		return ZoneId.of(environment.getTimeZone().getID());
	}

	protected FastDateFormat getDateFormat(FieldDef field) {
		return FastDateFormat.getInstance(field.getFormat(), environment.getTimeZone(), environment.getLocale());
	}

	protected void setFormat(FieldDef field) {
		if (StringUtils.isBlank(field.getFormat())) {
			field.setFormat(DEFAULT_DATEPATTERN);
		}
	}

	@Override
	protected Logger getLog() {
		return LOGGER;
	}

}
