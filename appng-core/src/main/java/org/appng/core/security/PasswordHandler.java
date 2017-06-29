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
package org.appng.core.security;

import org.appng.api.BusinessException;
import org.appng.api.model.AuthSubject;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.service.CoreService;

/**
 * Provides methods to handle passwords. Different implementations of this interface can provide different algorithms to
 * hash, salt and store a password. It can be expected that the constructor of an implementation of this interfaces
 * requires an instance of {@link AuthSubject} or {@link SubjectImpl} as argument.
 * 
 * @author Matthias Herlitzius
 * 
 */
public interface PasswordHandler {

	/**
	 * Hashes and persists the password.
	 * 
	 * @param password
	 *            The cleartext password.
	 */
	void savePassword(String password);

	/**
	 * Checks whether the password is valid.
	 * 
	 * @param password
	 *            The cleartext password.
	 * @return True if the password is valid, false if it is invalid.
	 */
	boolean isValidPassword(String password);

	/**
	 * Returns a digest which can be used for the "Forgot password?" function.
	 * 
	 * @return A digest.
	 */
	String getPasswordResetDigest();

	/**
	 * Checks whether the digest is valid.
	 * 
	 * @param digest
	 *            The digest.
	 * @return True if the digest is valid, false if it is invalid.
	 */
	boolean isValidPasswordResetDigest(String digest);

	/**
	 * Updates the {@link SubjectImpl} instance in the database. Must be called if modifications have been made to the
	 * instance of {@link AuthSubject} / {@link SubjectImpl} by methods of this interface and the instance is not
	 * updated later during the same transaction.
	 * 
	 * @param service
	 *            The {@link CoreService} instance.
	 * @throws BusinessException
	 *             Throws a {@link BusinessException} if the {@link SubjectImpl} can not be updated.
	 */
	void updateSubject(CoreService service) throws BusinessException;

	/**
	 * Migrates passwords of the current {@link PasswordHandler} instance to passwords handled by
	 * {@link CoreService#getDefaultPasswordHandler(org.appng.api.model.AuthSubject)}.
	 * 
	 * @param service
	 *            Instance of {@link CoreService}
	 * @param password
	 *            The current password.
	 */
	void migrate(CoreService service, String password);

}
