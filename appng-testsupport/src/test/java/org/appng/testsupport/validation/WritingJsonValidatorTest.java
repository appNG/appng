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
package org.appng.testsupport.validation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.appng.xml.platform.Action;
import org.appng.xml.platform.Datasource;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WritingJsonValidatorTest {

	static {
		WritingJsonValidator.writeJson = false;
		WritingJsonValidator.logJson = false;
	}

	@Test
	public void testValidateAction() throws IOException, URISyntaxException {
		WritingJsonValidator.validate(new Action(), "json/WritingJsonValidatorTest-testValidateAction.json");
	}

	@Test
	public void testValidateDataSource() throws IOException, URISyntaxException {
		WritingJsonValidator.validate(new Datasource(), "json/WritingJsonValidatorTest-testValidateDataSource.json");
	}

	@Test
	public void testValidateObject() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Person p = getPerson(sdf);
		WritingJsonValidator.validate(p, "json/WritingJsonValidatorTest-testValidateObject.json");
	}

	@Test
	public void testValidateObjectStrictOrder() throws Exception {
		WritingJsonValidator.sortPropertiesAlphabetically = true;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Person p = getPerson(sdf);
		WritingJsonValidator.validate(p, "json/WritingJsonValidatorTest-testValidateObjectStrictOrder.json");
		WritingJsonValidator.sortPropertiesAlphabetically = false;
	}

	@Test
	public void testValidateObjectWithMapper() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Person p = getPerson(sdf);
		WritingJsonValidator.validate(new ObjectMapper().setDateFormat(sdf), p,
				"json/WritingJsonValidatorTest-testValidateObjectWithMapper.json");
	}

	protected Person getPerson(SimpleDateFormat sdf) throws ParseException {
		return new Person("John", "Doe", sdf.parse("2015-04-27"));
	}

	class Person {

		private Date birthDate;
		private String name;
		private String firstName;
		private Object nullValue;

		public Person(String firstName, String name, Date birthDate) {
			this.firstName = firstName;
			this.name = name;
			this.birthDate = birthDate;
		}

		public Date getBirthDate() {
			return birthDate;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getName() {
			return name;
		}

		public Object getNullValue() {
			return nullValue;
		}

	}

}
