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

import java.text.DateFormat;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateFieldConverterZonedDateTimeTest extends DateFieldConverterTest {

	@Override
	public Container<?> getContainer() {
		return new Container<ZonedDateTime>() {
		};
	}

	@Override
	protected Object getDate() throws ParseException {
		return getZonedDateTime(sdf, DATE_STRING);
	}

	@Override
	protected Object getShortDate() throws ParseException {
		return getZonedDateTime(sdfShort, DATE_STRING_SHORT);
	}

	protected ZonedDateTime getZonedDateTime(DateFormat df, String input) throws ParseException {
		return df.parse(input).toInstant().atZone(ZoneId.systemDefault());
	}

}
