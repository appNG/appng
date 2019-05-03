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
package org.appng.core.security;

import static org.apache.commons.lang3.RandomStringUtils.random;

import java.util.regex.Pattern;

import org.appng.api.auth.PasswordPolicy;

/**
 * The default {@link PasswordPolicy} requiring 6 to 64 non-whitespace characters for a valid password.
 * 
 * @author Matthias Müller
 * 
 */
public class DefaultPasswordPolicy implements PasswordPolicy {

	public static final String REGEX = "[\\S]{6,64}";
	public static final String ERROR_MSSG_KEY = DefaultPasswordPolicy.class.getSimpleName() + ".errorMessage";

	private String errorMessageKey;
	private Pattern pattern;

	public DefaultPasswordPolicy(String regEx, String errorMessageKey) {
		this.errorMessageKey = errorMessageKey != null ? errorMessageKey : ERROR_MSSG_KEY;
		this.pattern = regEx != null ? Pattern.compile(regEx) : Pattern.compile(REGEX);
	}

	public DefaultPasswordPolicy() {
		this(REGEX, ERROR_MSSG_KEY);
	}

	public boolean isValidPassword(char[] password) {
		return pattern.matcher(new String(password)).matches();
	}

	public String getErrorMessageKey() {
		return errorMessageKey;
	}

	public String generatePassword() {
		return random(6, LOWERCASE + UPPERCASE) + random(1, NUMBER) + random(1, PUNCT);
	}

}
