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
package org.appng.core.controller.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.model.AccessibleApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Adds support for detecting and handling {@link RestController}s.
 * 
 * @see ServiceRequestHandler
 * 
 * @author Matthias Müller
 *
 */
public class RestService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestService.class);
	private static final int REST_PATH_START_INDEX = 5;

	private Site site;
	private AccessibleApplication application;
	private Environment environment;

	public RestService(Site site, AccessibleApplication application, Environment environment) {
		this.site = site;
		this.application = application;
		this.environment = environment;
	}

	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		HttpServletRequestWrapper wrapped = getWrappedRequest(servletRequest);
		RequestMappingHandlerMapping rmhm = new RequestMappingHandlerMapping();
		ApplicationContext context = application.getContext();
		rmhm.setApplicationContext(context);
		rmhm.afterPropertiesSet();
		try {
			HandlerExecutionChain handler = rmhm.getHandler(wrapped);
			if (null == handler) {
				LOGGER.warn("no @RestController found for {}", servletRequest.getServletPath());
				servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
				return;
			}
			HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();

			RequestMappingHandlerAdapter rmha = new RequestMappingHandlerAdapter();
			rmha.setApplicationContext(context);
			rmha.setCustomArgumentResolvers(Arrays.asList(getArgumentResolver()));
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			@SuppressWarnings("rawtypes")
			Map<String, HttpMessageConverter> converterMap = context.getBeansOfType(HttpMessageConverter.class);
			converterMap.values().forEach(hmc -> messageConverters.add(hmc));
			rmha.setMessageConverters(messageConverters);
			rmha.afterPropertiesSet();
			rmha.handle(wrapped, servletResponse, handlerMethod);
		} catch (Exception e) {
			LOGGER.error("error while calling @RestController", e);
			servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}

	}

	protected HandlerMethodArgumentResolver getArgumentResolver() {
		return new HandlerMethodArgumentResolver() {

			public boolean supportsParameter(MethodParameter parameter) {
				return isSite(parameter) || isEnvironment(parameter) || isApplication(parameter);
			}

			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
					NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
				return isSite(parameter) ? site
						: (isEnvironment(parameter) ? environment : (isApplication(parameter) ? application : null));
			}

			private boolean isEnvironment(MethodParameter parameter) {
				return isParameterType(parameter, Environment.class);
			}

			protected boolean isSite(MethodParameter parameter) {
				return isParameterType(parameter, Site.class);
			}

			private boolean isApplication(MethodParameter parameter) {
				return isParameterType(parameter, Application.class);
			}

			private boolean isParameterType(MethodParameter parameter, Class<?> type) {
				return parameter.getParameterType().equals(type);
			}
		};
	}

	protected HttpServletRequestWrapper getWrappedRequest(HttpServletRequest servletRequest) {
		HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(servletRequest) {

			@Override
			public String getServletPath() {
				return getMappedPath(super.getServletPath());
			}

			@Override
			public String getRequestURI() {
				return getMappedPath(super.getRequestURI());
			}

			protected String getMappedPath(String servletPath) {
				String[] splitted = servletPath.split(Path.SEPARATOR);
				String path = Path.SEPARATOR + StringUtils
						.join(ArrayUtils.subarray(splitted, REST_PATH_START_INDEX, splitted.length), Path.SEPARATOR);
				return path;
			}
		};
		return wrapped;
	}

}
