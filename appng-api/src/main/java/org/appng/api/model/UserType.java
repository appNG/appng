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
package org.appng.api.model;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the different types a {@link Subject} can be of.
 * 
 * @author Matthias Müller
 * 
 */
public enum UserType {

	/**
	 * Type for local users, which are completely configured within appNG, including their credentials.
	 */
	LOCAL_USER,

	/**
	 * A global user, which is known by appNG, but the credentials are managed by an external system (e.g. LDAP or
	 * NTLM/Kerberos).
	 */
	GLOBAL_USER,

	/**
	 * A global group (without specific users), which is known by appNG, but the credentials are managed by an external
	 * system (e.g. LDAP or NTLM/Kerberos).
	 */
	GLOBAL_GROUP;

	public static List<String> names() {
		return Arrays.asList(LOCAL_USER.name(), GLOBAL_USER.name(), GLOBAL_GROUP.name());
	}

}
