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
package org.appng.api.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ParameterSupport;
import org.appng.el.ExpressionEvaluator;

/**
 * 
 * Basic {@link ParameterSupport} implementation.
 * 
 * @author Matthias Müller
 * 
 */
public class ParameterSupportBase implements ParameterSupport {

	private static final String ESCAPE_SEQ = "\\\\";
	/*
	 * parameter names must start with a letter, followed by any number of letters, numbers and the underscore sign
	 */
	private static final String PARAMETER_PATTERN = "[a-zA-Z]+[a-zA-Z0-9_]*";
	private static final String PARAMETER_DOT_PATTERN = "[a-zA-Z]+[a-zA-Z0-9_\\.]*";
	private String prefix;
	private String suffix;
	private Pattern pattern;

	private Map<String, String> parameters;

	protected ParameterSupportBase(String prefix, String suffix, Map<String, String> parameters) {
		if (StringUtils.isBlank(prefix)) {
			throw new IllegalArgumentException("prefix can not be blank");
		}
		if (StringUtils.isBlank(suffix)) {
			throw new IllegalArgumentException("suffix can not be blank");
		}
		this.prefix = prefix;
		this.suffix = suffix;
		this.parameters = parameters;
		this.pattern = Pattern.compile(prefix + PARAMETER_PATTERN + suffix);
	}

	/**
	 * Call this method to allow dots in parameter names, which is disabled by default.
	 */
	public void allowDotInName() {
		this.pattern = Pattern.compile(prefix + PARAMETER_DOT_PATTERN + suffix);
	}

	protected ParameterSupportBase(String prefix, String suffix) {
		this(prefix, suffix, new HashMap<String, String>());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.ParameterSupport#replaceParameters(java.lang.String)
	 */
	public final String replaceParameters(String source) {
		String result = source;
		if (StringUtils.isNotBlank(source)) {
			for (String match : getParameters(source)) {
				String value = parameters.get(match);
				result = result.replaceAll(prefix + match + suffix, null == value ? "" : value.replace("$", "\\$"));
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.ParameterSupport#getParameters(java.lang.String)
	 */
	public final List<String> getParameters(String source) {
		Matcher matcher = pattern.matcher(source);
		List<String> matches = new ArrayList<String>();
		while (matcher.find()) {
			String match = matcher.group();
			String value = match.substring(getBareLength(prefix), match.length() - getBareLength(suffix));
			matches.add(value);
		}
		return Collections.unmodifiableList(matches);
	}

	/*
	 * returns the length of the given affix, therefore remove escape sequences
	 */
	private int getBareLength(String affix) {
		return affix.replaceAll(ESCAPE_SEQ, "").length();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.ParameterSupport#getParameterNames()
	 */
	public final Collection<String> getParameterNames() {
		return Collections.unmodifiableSet(parameters.keySet());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.ParameterSupport#getParameters()
	 */
	public final Map<String, String> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.appng.api.ParameterSupport#getExpressionEvaluator()
	 */
	public final ExpressionEvaluator getExpressionEvaluator() {
		return new ExpressionEvaluator(getParameters());
	}
}
