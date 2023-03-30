/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.appngizer.model;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class Utils {
	private static DatatypeFactory dtf;

	static {
		try {
			dtf = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new IllegalStateException("error creating DatatypeFactory", e);
		}
	}

	public static Date getDate(XMLGregorianCalendar d) {
		return d == null ? null : d.toGregorianCalendar().getTime();
	}

	public static XMLGregorianCalendar getCal(Date d) {
		if (d == null) {
			return null;
		}
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(d);
		return dtf.newXMLGregorianCalendar(cal);
	}
}
