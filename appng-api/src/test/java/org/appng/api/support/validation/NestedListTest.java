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
package org.appng.api.support.validation;

import java.util.Locale;

import org.appng.api.AbstractTest;
import org.appng.api.ValidationProvider;
import org.appng.api.support.XmlValidator;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.MetaData;
import org.junit.Test;
import org.springframework.context.MessageSource;

public class NestedListTest extends AbstractTest {

	@Test
	public void testAddValidationMetaData() throws Exception {

		MessageSource messageSource = getMessageSource();
		ValidationProvider validationProvider = new DefaultValidationProvider(
				new LocalizedMessageInterpolator(Locale.ENGLISH, messageSource), messageSource, Locale.ENGLISH);

		MarshallService marshallService = MarshallService.getMarshallService();
		MetaData metaData = marshallService.unmarshall(NestedListTest.class.getResourceAsStream("nestedlist.xml"),
				MetaData.class);
		validationProvider.addValidationMetaData(metaData, getClass().getClassLoader());
		XmlValidator.validate(metaData);
	}

}
