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
package org.appng.core.security;

import static org.apache.commons.lang3.RandomStringUtils.random;

import java.util.regex.Pattern;

import org.appng.api.MessageParam;
import org.appng.api.Platform;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.model.Properties;

/**
 * The default {@link PasswordPolicy} requiring 6 to 64 non-whitespace characters for a valid password.
 * 
 * @deprecated use {@link ConfigurablePasswordPolicy} instead
 * 
 * @author Matthias Müller
 */
@Deprecated
public class DefaultPasswordPolicy implements PasswordPolicy {

	public static final String REGEX = "[\\S]{6,64}";
	public static final String ERROR_MSSG_KEY = DefaultPasswordPolicy.class.getSimpleName() + ".errorMessage";

	private String errorMessageKey;
	private Pattern pattern;

	@Deprecated
	public DefaultPasswordPolicy() {
		this(REGEX, ERROR_MSSG_KEY);
	}

	@Deprecated
	public DefaultPasswordPolicy(String regEx, String errorMessageKey) {
		this.errorMessageKey = errorMessageKey != null ? errorMessageKey : ERROR_MSSG_KEY;
		this.pattern = regEx != null ? Pattern.compile(regEx) : Pattern.compile(REGEX);
	}

	@Override
	public void configure(Properties platformConfig) {
		String pwdRegEx = platformConfig.getString(Platform.Property.PASSWORD_POLICY_REGEX, REGEX);
		this.errorMessageKey = platformConfig.getString(Platform.Property.PASSWORD_POLICY_ERROR_MSSG_KEY,
				ERROR_MSSG_KEY);
		this.pattern = Pattern.compile(pwdRegEx);
	}

	public boolean isValidPassword(char[] password) {
		return pattern.matcher(new String(password)).matches();
	}

	@Override
	public ValidationResult validatePassword(String username, char[] currentPassword, char[] password) {
		MessageParam messageParam = new MessageParam() {

			public String getMessageKey() {
				return getErrorMessageKey();
			}

			public Object[] getMessageArgs() {
				return null;
			}
		};
		return isValidPassword(password) ? new ValidationResult(true, null)
				: new ValidationResult(false, new MessageParam[] { messageParam });
	}

	public String getErrorMessageKey() {
		return errorMessageKey;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public String generatePassword() {
		return random(6, LOWERCASE + UPPERCASE) + random(1, NUMBER) + random(1, PUNCT);
	}

}
