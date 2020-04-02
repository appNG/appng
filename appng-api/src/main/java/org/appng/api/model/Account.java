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

import java.util.Locale;

/**
 * Basic account data.
 * 
 * @author Matthias Müller
 * 
 */
public interface Account {

	/**
	 * Returns the username of this {@link Account}, e.g. 'admin'.
	 * 
	 * @return the username
	 */
	String getAuthName();

	/**
	 * Returns the real name of this {@link Account}, e.g. 'appNG Administrator'.
	 * 
	 * @return the real name
	 */
	String getRealname();

	/**
	 * Returns the language tag for this {@link Account} in the <a href="https://tools.ietf.org/html/bcp47">IETF BCP
	 * 47</a> notation: {@code <lang>-<country>}.<br/>
	 * Examples:
	 * <ul>
	 * <li>en
	 * <li>en-US
	 * <li>de
	 * <li>de-CH
	 * </ul>
	 * 
	 * @return the language
	 * @see Locale#forLanguageTag(String)
	 */
	String getLanguage();

	/**
	 * Returns the timezone for this {@link Account}, e.g. 'Europe/Berlin'.
	 * 
	 * @return the timezone
	 */
	String getTimeZone();

	/**
	 * Returns the email for this {@link Account}, e.g. 'admin@example.com'.
	 * 
	 * @return the email
	 */
	String getEmail();

}
