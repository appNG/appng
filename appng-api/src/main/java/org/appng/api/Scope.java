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
package org.appng.api;

import org.appng.api.model.Site;

/**
 * Enum type defining the possible scopes of an {@link Environment}-attribute.
 * 
 * @author Matthias Müller
 * 
 */
public enum Scope {

	/**
	 * platform-scope
	 */
	PLATFORM,

	/**
	 * site-scope, the {@link Site} is identified by its host.
	 */
	SITE,

	/**
	 * session-scope
	 */
	SESSION,

	/**
	 * request-scope
	 */
	REQUEST,

	/**
	 * url-scope, as used in JSPs
	 */
	URL;

	/**
	 * 
	 * @param host
	 *            the host of the {@link Site} to get the name for
	 * @return a String used in order to identify an {@link Environment} for a {@link Site}
	 * @throws IllegalArgumentException
	 *             if this scope not equals {@link Scope#SITE}
	 */
	public String forSite(String host) {
		if (SITE.equals(this)) {
			return name() + "." + host;
		}
		throw new IllegalArgumentException("not allowed for scope " + name());
	}

}
