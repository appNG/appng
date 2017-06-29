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
package org.appng.api.support;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.appng.api.Request;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class I18nTest {

	@Test
	public void test() {
		Request request = Mockito.mock(Request.class);
		Mockito.when(request.getLocale()).thenReturn(Locale.ENGLISH);
		I18n i18n = new I18n(request);
		Mockito.when(request.getMessage("foo")).thenReturn("bar");
		Date date = new GregorianCalendar(2013, Calendar.MARCH, 8).getTime();
		Assert.assertEquals("03 2013", i18n.format("%1$tm %1$tY", date));
		Assert.assertEquals("2013-03-08", i18n.formatDate(date, "yyyy-MM-dd"));
		Assert.assertEquals("42,000.42", i18n.formatNumber(42000.42d, "0,000.00"));
		Assert.assertEquals("bar", i18n.message("foo"));
	}

}
