/*
 * Copyright 2011-2020 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.appng.api.Person;
import org.appng.api.Person.GroupA;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

public class LocalizedMessageInterpolatorTest {

	@Test
	public void test() {
		ResourceBundleMessageSource appMssgSrc = new ResourceBundleMessageSource();
		appMssgSrc.setBasename("testmessages");
		appMssgSrc.setAlwaysUseMessageFormat(true);
		appMssgSrc.setFallbackToSystemLocale(true);
		appMssgSrc.setDefaultEncoding("UTF-8");

		LocalizedMessageInterpolator lmi = new LocalizedMessageInterpolator(Locale.ENGLISH, appMssgSrc);
		Configuration<?> configuration = Validation.byDefaultProvider().configure();
		ValidatorFactory validatorFactory = configuration.messageInterpolator(lmi).buildValidatorFactory();
		Validator validator = validatorFactory.getValidator();

		Person person = new Person();
		person.setFirstname("");
		Set<ConstraintViolation<Person>> violations = validator.validateProperty(person, "firstname", GroupA.class);
		Assert.assertEquals(1, violations.size());
		for (ConstraintViolation<Person> cv : violations) {
			validateContraint(cv, Size.class, "Value must be between 1 and 5");
		}

		violations = validator.validateProperty(person, "name");
		Assert.assertEquals(2, violations.size());
		for (ConstraintViolation<Person> cv : violations) {
			validateContraint(cv, NotBlank.class, "Thou must provide a value!");
			validateContraint(cv, NotNull.class, "must not be null");
		}

	}

	private void validateContraint(ConstraintViolation<Person> cv, Class<?> type, String message) {
		Annotation annotation = cv.getConstraintDescriptor().getAnnotation();
		if (type.isAssignableFrom(annotation.getClass())) {
			Assert.assertEquals(message, cv.getMessage());
			return;
		}
	}
}
