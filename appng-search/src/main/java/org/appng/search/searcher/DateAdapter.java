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
package org.appng.search.searcher;

import java.text.DateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A {@link XmlAdapter} responsible for adapting a {@link String} to a {@link Date} and vice versa.
 * 
 * @author Matthias Müller
 *
 */
public class DateAdapter extends XmlAdapter<String, Date> {

	private DateFormat dateFormat;

	public DateAdapter(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	public String marshal(Date date) throws Exception {
		return dateFormat.format(date);
	}

	public Date unmarshal(String date) throws Exception {
		return dateFormat.parse(date);
	}

}
