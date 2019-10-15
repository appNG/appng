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

import java.text.ParseException;
import java.time.LocalDate;

import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.xml.platform.Datafield;
import org.junit.Assert;

public class DateFieldConverterLocalDateTest extends DateFieldConverterZonedDateTimeTest {

	private static final String DATE_NO_TIME = "2012-12-05 00:00:00";

	@Override
	public Container<?> getContainer() {
		return new Container<LocalDate>() {
		};
	}

	@Override
	public void testSetString() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, getDate());
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals(DATE_NO_TIME, fieldWrapper.getStringValue());
		Assert.assertEquals(DateFieldConverter.DEFAULT_DATEPATTERN, fieldWrapper.getFormat());
	}

	@Override
	public void testAddField() throws Exception {
		beanWrapper.setPropertyValue(OBJECT, getDate());
		DatafieldOwner dataFieldOwner = getDatafieldOwner();
		fieldConverter.addField(dataFieldOwner, fieldWrapper);
		Datafield datafield = dataFieldOwner.getFields().get(0);
		Assert.assertEquals("object", datafield.getName());
		Assert.assertEquals(DATE_NO_TIME, datafield.getValue());
		Assert.assertEquals(0, datafield.getFields().size());
	}

	@Override
	protected Object getDate() throws ParseException {
		return getZonedDateTime(sdf, DATE_STRING).toLocalDate();
	}

	@Override
	protected Object getShortDate() throws ParseException {
		return getZonedDateTime(sdfShort, DATE_STRING_SHORT).toLocalDate();
	}

}
