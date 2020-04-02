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
package org.appng.core.security;

import org.appng.api.BusinessException;
import org.appng.api.model.AuthSubject;
import org.appng.core.service.CoreService;

/**
 * Provides methods to handle passwords. Different implementations of this interface can provide different algorithms to
 * hash, salt and store a password. It can be expected that the constructor of an implementation of this interface
 * requires an {@link AuthSubject} as argument.
 * 
 * @author Matthias Herlitzius
 */
public interface PasswordHandler {

	/**
	 * Hashes and sets the password, clears the salt, sets the last changed date for the password
	 * 
	 * @param password
	 *                 The cleartext password.
	 * @see            AuthSubject#setDigest(String)
	 * @see            AuthSubject#setSalt(String)
	 * @see            AuthSubject#setPasswordLastChanged(java.util.Date)
	 */
	void applyPassword(String password);

	/**
	 * Checks whether the password is valid for the current {@link AuthSubject}.
	 * 
	 * @param  password
	 *                  The cleartext password.
	 * @return          {@code true} if the password is valid, false if it is invalid.
	 * @see             AuthSubject#getDigest()
	 */
	boolean isValidPassword(String password);

	/**
	 * Calculates, sets and returns a salted digest which can be used for the "Forgot password?" function.
	 * 
	 * @return A digest.
	 * @see    AuthSubject#setSalt(String)
	 * @see    #isValidPasswordResetDigest(String)
	 */
	String calculatePasswordResetDigest();

	/**
	 * Checks whether the digest is valid for the current {@link AuthSubject}.
	 * 
	 * @param  digest
	 *                The digest.
	 * @return        {@code true} if the digest is valid, false if it is invalid.
	 * @see           AuthSubject#getSalt()
	 * @see           #calculatePasswordResetDigest()
	 */
	boolean isValidPasswordResetDigest(String digest);

	/**
	 * Migrates passwords of the current {@link PasswordHandler} instance to passwords handled by
	 * {@link CoreService#getDefaultPasswordHandler(org.appng.api.model.AuthSubject)}.
	 * 
	 * @param service
	 *                 Instance of {@link CoreService}
	 * @param password
	 *                 The current password.
	 */
	void migrate(CoreService service, String password);

	/**
	 * @deprecated will be removed in 2.x
	 */
	@Deprecated
	default void updateSubject(CoreService service) throws BusinessException {
	}

	/**
	 * @deprecated will be removed in 2.x
	 */
	@Deprecated
	default String getPasswordResetDigest() {
		return calculatePasswordResetDigest();
	}

	/**
	 * @deprecated will be removed in 2.x
	 */
	@Deprecated
	default void savePassword(String password) {
		applyPassword(password);
	}

}
