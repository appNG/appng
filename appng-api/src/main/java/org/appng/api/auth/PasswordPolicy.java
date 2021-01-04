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
package org.appng.api.auth;

import org.appng.api.MessageParam;
import org.appng.api.model.Properties;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A {@link PasswordPolicy} defines how a valid password looks like, is able to create such a password and to check
 * whether a given character-sequence is a valid password according to the requirements.
 * 
 * @author Matthias Müller
 */
public interface PasswordPolicy {

	/**
	 * Holds the result of validating a password.
	 * 
	 * @see   PasswordPolicy#validatePassword(String, char[], char[])
	 * @since 1.21
	 */
	@Getter
	@AllArgsConstructor
	class ValidationResult {
		private final boolean isValid;
		private final MessageParam[] messages;
	}

	/** numbers 0-9 */
	String NUMBER = "0123456789";
	/** lowercase letters a-z */
	String LOWERCASE = "abcedfghijklmnopqrstuvwxyz";
	/** uppercase letters A-Z */
	String UPPERCASE = "ABCEDFGHIJKLMNOPQRSTUVWXYZ";
	/** punctuation characters !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~ */
	String PUNCT = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

	/**
	 * Configures the {@link PasswordPolicy}
	 * 
	 * @param platformProperties
	 *                           the platform's {@link Properties}
	 * @since                    1.21
	 */
	default void configure(Properties platformProperties) {

	}

	/**
	 * Checks whether the given character-sequence is a valid password.
	 * 
	 * @param      password
	 *                      the character-sequence sequence to check
	 * @return              {@code true} if the character-sequence is a valid password, {@code false} otherwise
	 * @deprecated          will be removed in 2.x, use {@link #validatePassword(String, char[], char[])} instead
	 */
	@Deprecated
	boolean isValidPassword(char[] password);

	/**
	 * Validates the password an returns a {@link ValidationResult}
	 * 
	 * @param  username
	 *                         the username (can be {@code null})
	 * @param  currentPassword
	 *                         the current password (can be {@code null})
	 * @param  password
	 *                         the new password (must not be {@code null})
	 * @return                 the validation result
	 * @sine                   1.21
	 */
	default ValidationResult validatePassword(String username, char[] currentPassword, char[] password) {
		return null;
	}

	/**
	 * Returns the message-key of an errormessage for the case that the password doesn't match the requirements.
	 * 
	 * @return     the message key
	 * @deprecated a {@link ValidationResult} should be used, see {@link #validatePassword(String, char[], char[])}
	 */
	@Deprecated
	String getErrorMessageKey();

	/**
	 * Generates a new random password that matches the requirements and returns it.
	 * 
	 * @return the randomly generated password
	 */
	String generatePassword();

}
