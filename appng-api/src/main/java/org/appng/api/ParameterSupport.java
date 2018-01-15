/*
 * Copyright 2011-2018 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.appng.el.ExpressionEvaluator;

/**
 * Supports finding and replacing parameters inside a {@link String}. The parameters need to use a prefix and a suffix
 * so they can be found.
 * 
 * @author Matthias Müller
 * 
 */
public interface ParameterSupport {

	/**
	 * Replaces all parameters in the given {@code String} and returns a new one containing the replacements.
	 * 
	 * @param source
	 *            the {@code String} to replace the parameters in
	 * @return a new {@code String} with the parameters replaced
	 */
	String replaceParameters(String source);

	/**
	 * Returns the names of all parameters used in the given {@code String}, without affixes.
	 * 
	 * @param source
	 *            the {@code String} to search the parameters for
	 * @return the names of all parameters used in the given {@code String}, without affixes.
	 */
	List<String> getParameters(String source);

	/**
	 * Returns the names of all parameters this {@link ParameterSupport} is aware of.
	 * 
	 * @return the names of all parameters this {@link ParameterSupport} is aware of
	 */
	Collection<String> getParameterNames();

	/**
	 * Returns an immutable {@link Map} containing all parameters this {@link ParameterSupport} is aware of.
	 * 
	 * @return an immutable {@link Map} containing all parameters this {@link ParameterSupport} is aware of.
	 */
	Map<String, String> getParameters();

	/**
	 * Returns an {@link ExpressionEvaluator} created with the parameters of this {@link ParameterSupport}.
	 * 
	 * @return an {@link ExpressionEvaluator} created with the parameters of this {@link ParameterSupport}.
	 */
	ExpressionEvaluator getExpressionEvaluator();

}
