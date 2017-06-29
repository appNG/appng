/*
 * Copyright 2011-2017 the original author or authors.
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

/**
 * A {@link PasswordPolicy} defines how a valid password looks like, is able to create such a password and to check
 * whether a given character-sequence is a valid password according to the requirements.
 * 
 * @author Matthias Müller
 * 
 */
public interface PasswordPolicy {
	/** numbers 0-9 */
	String NUMBER = "0123456789";
	/** lowercase letters a-z */
	String LOWERCASE = "abcedfghijklmnopqrstuvwxyz";
	/** uppercase letters A-Z */
	String UPPERCASE = "ABCEDFGHIJKLMNOPQRSTUVWXYZ";
	/** punctuation characters !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~ */
	String PUNCT = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

	/**
	 * Checks whether the given character-sequence is a valid password.
	 * 
	 * @param password
	 *            the character-sequence sequence to check
	 * @return {@code true} if the character-sequence is a valid password, {@code false} otherwise
	 */
	boolean isValidPassword(char[] password);

	/**
	 * Returns the message-key of an errormessage for the case that the password doesn't match the requirements.
	 * 
	 * @return the message key
	 */
	String getErrorMessageKey();

	/**
	 * Generates a new random password that matches the requirements and returns it.
	 * 
	 * @return the randomly generated password
	 */
	String generatePassword();

}
