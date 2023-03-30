/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.appng.api.model.Subject;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.RequestContainer;
import org.springframework.http.HttpHeaders;

/**
 * A {@link Request} is the framework-internal representation of a {@link HttpServletRequest}, wrapping the afore
 * mentioned and adding framework-specific methods.
 * 
 * @author Matthias Müller
 */
public interface Request extends RequestContainer, RequestSupport, ValidationProvider {

	/**
	 * Returns the {@link ExpressionEvaluator} for this {@link Request}.
	 * 
	 * @return the {@link ExpressionEvaluator}
	 */
	ExpressionEvaluator getExpressionEvaluator();

	/**
	 * Returns the {@link ParameterSupport} for this {@link Request}.
	 * 
	 * @return the {@link ParameterSupport}
	 */
	ParameterSupport getParameterSupportDollar();

	/**
	 * Returns the {@link PermissionProcessor} for this {@link Request}.
	 * 
	 * @return the {@link PermissionProcessor}
	 */
	PermissionProcessor getPermissionProcessor();

	/**
	 * Returns the {@link Subject} for this {@link Request}.
	 * 
	 * @return the {@link Subject}
	 */
	Subject getSubject();

	/**
	 * Returns the {@link Locale} for this {@link Request}.
	 * 
	 * @return the {@link Locale}
	 */
	Locale getLocale();

	/**
	 * Returns a list of URL-Parameters for a JSP-page, which are those path-elements appearing after the name of the
	 * JSP-page itself. So if the page is named {@code contact} and the path is {@code /contact/europe/germany}, the
	 * resulting {@link List} would be equal to {@code Arrays.asList("europe","germany")}.
	 * 
	 * @return the URL-Parameters for a JSP-Page
	 * 
	 * @see Path#getJspUrlParameters()
	 */
	// XXX is this needed?
	List<String> getUrlParameters();

	/**
	 * Checks whether a redirect-target has been set for this {@link Request}.
	 * 
	 * @return {@code true} if a redirect-target has been set for this {@link Request}, {@code false} otherwise
	 */
	boolean isRedirect();

	/**
	 * Checks whether this {@link Request} originates of a HTTP GET-request.
	 * 
	 * @return {@code true} if this {@link Request} originates of a HTTP GET-request, {@code false} otherwise
	 */
	boolean isGet();

	/**
	 * Checks whether this {@link Request} originates of a HTTP POST-request.
	 * 
	 * @return {@code true} if this {@link Request} originates of a HTTP POST-request, {@code false} otherwise
	 */
	boolean isPost();

	/**
	 * Returns the {@link HttpHeaders} for the underlying {@link HttpServletRequest}.
	 * 
	 * @return the headers
	 */
	HttpHeaders headers();

}
