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
package org.appng.testsupport.validation;

import java.io.IOException;

import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Result;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.junit.Test;

public abstract class DifferenceHandlerTest {

	protected Result result;

	protected DifferenceHandlerTest() {
		result = new Result();
		Datafield field = new Datafield();
		field.setName("creationDate");
		field.setType(FieldType.DATE);
		field.setValue("2012-09-21");
		result.getFields().add(field);
	}

	@Test(expected = AssertionError.class)
	public void testValidateFail() throws IOException {
		run(new DifferenceHandler() {

			@Override
			public int differenceFound(Difference difference) {
				return RETURN_ACCEPT_DIFFERENCE;
			}
		});
	}

	protected void run(DifferenceListener differenceListener) throws IOException {
		WritingXmlValidator.validateXml(result, "xml/DifferenceHandler-control.xml", differenceListener);
	}
}
