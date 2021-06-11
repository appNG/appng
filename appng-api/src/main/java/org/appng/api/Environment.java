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
package org.appng.api;

import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.appng.api.model.Application;
import org.appng.api.model.Subject;

/**
 * The {@link Environment} is able to set, read and remove attributes of a certain {@link Scope}. The main purposes are:
 * <ul>
 * <li>keeping track of the current {@link Subject} via {@link #getSubject()}
 * <li>providing informations about the current {@link Locale} and {@link TimeZone} via {@link #getLocale()} and
 * {@link #getTimeZone()}
 * <li>allowing to share attributes between multiple applications (using {@link Scope#SESSION} and {@link Scope#SITE})
 * <li>keeping track of platform-wide configuration parameters ( {@link Scope#PLATFORM} and {@link Application}s where
 * {@link org.appng.api.model.Application#isPrivileged()} is {@code true})
 * </ul>
 * </ul>
 * 
 * @author Matthias Müller
 * 
 * @see Scope
 */
public interface Environment {

	/**
	 * Sets an attribute for the given {@link Scope} to the given value.
	 * 
	 * @param scope
	 *              the {@link Scope} of the attribute to set
	 * @param name
	 *              the name of the attribute to set
	 * @param value
	 *              the attribute to set
	 */
	void setAttribute(Scope scope, String name, Object value);

	/**
	 * Returns the attribute with the given name for the given {@link Scope}.
	 * 
	 * @param scope
	 *              the {@link Scope} of the attribute to get
	 * @param name
	 *              the name of the attribute to get
	 * 
	 * @return the attribute of the desired type, or {@code null} if the attribute does not exist in the given
	 *         {@link Scope}
	 */
	<T> T getAttribute(Scope scope, String name);

	/**
	 * Removes the attribute with the given name from the given {@link Scope} and returns it.
	 * 
	 * @param scope
	 *              the {@link Scope} of the attribute to remove
	 * @param name
	 *              the name of the attribute to remove
	 * 
	 * @return the attribute of the desired type, or {@code null} if the attribute does not exist in the given
	 *         {@link Scope}
	 */
	<T> T removeAttribute(Scope scope, String name);

	/**
	 * Returns the string-representation of an attribute, calling {@code toString()} on the resulting object.
	 * 
	 * @param scope
	 *              the {@link Scope} of the attribute to get
	 * @param name
	 *              the name of the attribute to get
	 * 
	 * @return the string-representation of the attribute, or {@code null} if the attribute does not exist in the given
	 *         {@link Scope}
	 */
	String getAttributeAsString(Scope scope, String name);

	/**
	 * Returns a {@link Set} of all attribute names for the given {@link Scope}
	 * 
	 * @param scope
	 *              the {@link Scope} the get the attribute names for
	 * 
	 * @return a {@link Set} containing all attribute names, or {@code null} if the {@link Scope} is not available
	 */
	Set<String> keySet(Scope scope);

	/**
	 * Returns the current {@link Subject}.
	 * 
	 * @return the {@link Subject}, if present
	 */
	Subject getSubject();

	/**
	 * Returns the current {@link Locale}. If a {@link Subject} is logged in, a {@link Locale} is returned that
	 * represent the language of the {@link Subject} via {@link Subject#getLanguage()}. If no {@link Subject} is
	 * present, the default-{@link Locale} from the {@link org.appng.api.model.Site} is used.
	 * 
	 * @return the {@link Locale}
	 */
	Locale getLocale();

	/**
	 * Returns the current {@link TimeZone}. If a {@link Subject} is present (no matter whether it is logged in or not),
	 * {@link Subject#getTimeZone()} is being returned. If no {@link Subject} is present, the default-{@link TimeZone}
	 * from the {@link org.appng.api.model.Site} is used.
	 * 
	 * @return the {@link TimeZone}
	 */
	TimeZone getTimeZone();

	/**
	 * Returns {@code true} if the current {@link Subject} is authenticated, {@code false} otherwise
	 * 
	 * @return {@code true} if the current {@link Subject} is authenticated, {@code false} otherwise
	 */
	boolean isSubjectAuthenticated();

	/**
	 * Initializes the {@link Environment}.
	 * 
	 * @param context
	 *                 a {@link ServletContext}
	 * @param session
	 *                 a {@link HttpSession}
	 * @param request
	 *                 a {@link ServletRequest}
	 * @param response
	 *                 a {@link ServletResponse}
	 * @param host
	 *                 the host for the site-{@link Scope}
	 * 
	 * @throws IllegalStateException
	 *                               if this {@link Environment} already has been initialized
	 */
	void init(ServletContext context, HttpSession session, ServletRequest request, ServletResponse response,
			String host);

}
