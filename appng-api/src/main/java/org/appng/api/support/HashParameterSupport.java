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
package org.appng.api.support;

import java.util.Map;

/**
 * A {@link org.appng.api.ParameterSupport} which uses '#{' as prefix and '}' as suffix.
 * 
 * @author Matthias Müller
 * 
 */
public class HashParameterSupport extends ParameterSupportBase {

	private static final String PREFIX = "\\#\\{";
	private static final String SUFFIX = "\\}";

	/**
	 * Creates a new {@link HashParameterSupport} using the given parameters.
	 * 
	 * @param parameters
	 *            the parameters to use
	 */
	public HashParameterSupport(Map<String, String> parameters) {
		super(PREFIX, SUFFIX, parameters);
	}

}
