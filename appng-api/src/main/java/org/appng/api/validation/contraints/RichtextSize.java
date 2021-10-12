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
package org.appng.api.validation.contraints;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * A validation constraint for checking the plain text length of a string that contains HTML markup.
 * 
 * @author Matthias Müller
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = RichtextSizeValidator.class)
public @interface RichtextSize {

	String message() default "{validation.string.max}";

	Class<?>[] groups() default {};

	long max();

	long min() default 0;

	Class<? extends Payload>[] payload() default {};
}
