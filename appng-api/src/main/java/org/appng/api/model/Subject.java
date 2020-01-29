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
package org.appng.api.model;

import java.util.Date;
import java.util.List;

/**
 * A {@link Subject} is a fully featured user-account managed by appNG.
 * 
 * @author Matthias Müller
 */
public interface Subject extends Named<Integer>, AuthSubject {

	/**
	 * Returns the {@link Group}s for this {@link Subject}.
	 * 
	 * @return the {@link Group}s
	 */
	List<Group> getGroups();

	/**
	 * Returns the {@link UserType}.
	 * 
	 * @return the {@link UserType}
	 */
	UserType getUserType();

	/**
	 * Checks whether this {@link Subject} is authenticated.
	 * 
	 * @return {@code true} if this {@link Subject} is authenticated, {@code false} otherwise
	 */
	boolean isAuthenticated();

	/**
	 * Checks whether this {@link Subject} has authorization for the given {@link Authorizable}.
	 * 
	 * @param  authorizable
	 *                      the {@link Authorizable}
	 * @return              {@code true} if this {@link Subject} has authorization for the given {@link Authorizable},
	 *                      {@code false} otherwise
	 */
	boolean isAuthorized(Authorizable<?> authorizable);

	/**
	 * Checks whether this {@link Subject} owns at least one {@link Role} of the the given {@link Application}.
	 * 
	 * @param  application
	 *                     the {@link Application}
	 * @return             {@code true} if this {@link Subject} owns at least one {@link Role} of the the given
	 *                     {@link Application} , {@code false} otherwise
	 */
	boolean hasApplication(Application application);

	/**
	 * Returns a date after that the subject is locked, if any.
	 * 
	 * @return the date, or {@code null}
	 * @since  1.21
	 */
	default Date getLockedSince() {
		return null;
	}

	/**
	 * Returns the date of the last login, if any.
	 * 
	 * @return the date.
	 * @since  1.21
	 */
	default Date getLastLogin() {
		return null;
	}

	/**
	 * Returns the date when the password was last changed. Default to the creation date of the subject.
	 * 
	 * @return the date.
	 * @since  1.21
	 */
	default Date getPasswordLastChanged() {
		return null;
	}

	/**
	 * Returns the number of failed login attempts for this subject.
	 * 
	 * @return the number of failed login attempts
	 * @since  1.21
	 */
	default Integer getFailedLoginAttempts() {
		return 0;
	}

	@Override
	default PasswordChangePolicy getPasswordChangePolicy() {
		return PasswordChangePolicy.MAY;
	}

}
