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

import org.joda.time.DateTime;

/**
 * @author Claus Stümke, aiticon GmbH, 2016
 */
public class DateFieldConverterJodaDateTimeTest extends DateFieldConverterTest {

	@Override
	public Container<?> getContainer() {
		return new Container<DateTime>() {
		};
	}

	@Override
	protected DateTime getDate() throws ParseException {
		return new DateTime(super.getDate());
	}

	@Override
	protected DateTime getShortDate() throws ParseException {
		return new DateTime(super.getShortDate());
	}

	@Override
	public void testSetObject() throws Exception {
		super.testSetObject();
	}

}
