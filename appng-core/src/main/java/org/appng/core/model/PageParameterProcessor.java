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
package org.appng.core.model;

import static org.appng.api.Scope.SESSION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Request;
import org.appng.xml.platform.GetParams;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.ParamType;
import org.appng.xml.platform.PostParams;
import org.appng.xml.platform.UrlParams;
import org.appng.xml.platform.UrlSchema;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class PageParameterProcessor {

	private static final String PATTERN_FRACTION = "(\\.\\d+)?";
	private static final String PATTERN_DIGIT = "[-+]?\\d+";
	private static final String TRUE = "TRUE";
	private static final String FALSE = "FALSE";
	private static final String SORT_PREFIX = "sort";

	private Environment env;
	private Request request;
	private Map<String, String> parameters;
	private Set<String> sessionParamNames;
	private String sessionParamKey;

	PageParameterProcessor(String sessionParamKey, Set<String> sessionParamNames, Environment env, Request request,
			String page) {
		this.env = env;
		this.sessionParamKey = sessionParamKey;
		this.request = request;
		this.parameters = new HashMap<String, String>();
		this.sessionParamNames = sessionParamNames;
	}

	private void processGetParams(GetParams getParams) {
		if (null != getParams && request.isGet()) {
			processParams(getParams.getParamList());
		}
	}

	/**
	 * POST/GET -> URL -> SESSION
	 * 
	 * @param applicationParameter
	 * @param urlSchema
	 * @return
	 */
	boolean processPageParams(List<String> applicationUrlParameters, UrlSchema urlSchema) {
		processPostParams(urlSchema.getPostParams());
		processGetParams(urlSchema.getGetParams());
		boolean urlParamsAdded = processUrlParams(applicationUrlParameters, urlSchema.getUrlParams());
		processSessionParams();
		return urlParamsAdded;
	}

	private void processParams(List<Param> paramList) {
		if (null != paramList) {
			for (Param param : paramList) {
				String name = param.getName();
				String value = request.getParameter(name);
				value = synchronizeParamWithSession(param, value);
				if (null != value && !parameters.containsKey(name)) {
					param.setValue(value);
					addParam(name, value);
				}
			}
		}

		Set<String> parameterNames = request.getParameterNames();
		for (String parameter : parameterNames) {
			if (parameter.startsWith(SORT_PREFIX)) {
				String sortParam = request.getParameter(parameter);
				addParam(parameter, sortParam);
			}
		}
	}

	private void processPostParams(PostParams postParams) {
		if (null != postParams && request.isPost()) {
			processParams(postParams.getParamList());
		}
	}

	private void processSessionParams() {
		Map<String, String> sessionParams = getSessionParams();
		for (String param : sessionParamNames) {
			String value = sessionParams.get(param);
			addParam(param, value);
		}
	}

	private Map<String, String> getSessionParams() {
		Map<String, String> sessionParams = env.getAttribute(SESSION, getSessionParamKey());
		if (null == sessionParams) {
			sessionParams = new HashMap<String, String>();
			env.setAttribute(SESSION, getSessionParamKey(), sessionParams);
		}
		return sessionParams;
	}

	private String getSessionParamKey() {
		return sessionParamKey;
	}

	private boolean processUrlParams(List<String> applicationUrlParameters, UrlParams urlParams) {
		boolean paramAdded = false;
		if (null != urlParams) {
			for (int i = 0; i < urlParams.getParamList().size(); i++) {
				Param param = urlParams.getParamList().get(i);
				String name = param.getName();
				String defaultValue = param.getDefault();
				String value = null;
				boolean useUrlParam = applicationUrlParameters.size() > i;
				if (useUrlParam) {
					value = applicationUrlParameters.get(i);
				} else {
					String existingParam = parameters.get(name);
					if (StringUtils.isNotEmpty(existingParam)) {
						value = parameters.get(name);
					}
				}
				String newValue = synchronizeParamWithSession(param, value);
				if (null != newValue && !(newValue.equals(value) || newValue.equals(defaultValue))) {
					paramAdded |= true;
					LOGGER.info("retrieved new value for url-param '{}' from session: '{}'", name, newValue);
				}
				param.setValue(newValue);
				addParam(name, newValue);
			}
		}
		return paramAdded;
	}

	private void addParam(String name, String value) {
		if (null != value) {
			parameters.put(name, value);
		}
	}

	private String synchronizeParamWithSession(Param param, final String inputValue) {
		Map<String, String> sessionParams = getSessionParams();
		String name = param.getName();
		String defaultValue = param.getDefault();
		ParamType type = param.getType();
		boolean isSessionParam = sessionParamNames.contains(name);
		String value = inputValue;
		if (isSessionParam) {
			if (null == value) {
				String valueFromSession = sessionParams.get(name);
				if (StringUtils.isNotEmpty(valueFromSession)) {
					value = valueFromSession;
					LOGGER.debug("session-param: {} = '{}'", name, value);
				} else {
					value = defaultValue;
					LOGGER.debug("session-param not found in session, using default: {} = '{}'", name, value);
				}
			} else {
				LOGGER.debug("adding parameter to session: {} = '{}'", name, value);
				sessionParams.put(name, value);
				env.setAttribute(SESSION, getSessionParamKey(), sessionParams);
			}
		} else if (StringUtils.isEmpty(value)) {
			value = defaultValue;
			LOGGER.debug("using default-value: {} = '{}'", name, value);
		} else {
			LOGGER.debug("using value: {} = '{}'", name, value);
		}
		if (value != null && type != null) {
			boolean isParamOk = isParamOk(value, type);
			if (!isParamOk) {
				isParamOk = isParamOk(defaultValue, type);
				if (isParamOk) {
					value = defaultValue;
				} else {
					value = null;
				}
				if (isSessionParam) {
					sessionParams.put(name, value);
				}
			}
		}
		return value;
	}

	private boolean isParamOk(String value, ParamType type) {
		if (null == value) {
			return true;
		}
		switch (type) {
		case BOOLEAN:
			return value.toUpperCase().equals(TRUE) || value.toUpperCase().equals(FALSE);
		case DECIMAL:
			return value.matches(PATTERN_DIGIT + PATTERN_FRACTION);
		case INT:
			return value.matches(PATTERN_DIGIT);
		default:
			return true;
		}
	}

	Map<String, String> getParameters() {
		return parameters;
	}

}
