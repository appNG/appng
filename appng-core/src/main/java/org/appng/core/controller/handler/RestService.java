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
package org.appng.core.controller.handler;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Path;
import org.appng.api.config.RestConfig;
import org.appng.core.model.AccessibleApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Adds support for detecting and handling {@link RestController}s. Also detects {@link ExceptionHandler}s on beans
 * annotated with {@link ControllerAdvice}.
 * 
 * @see ServiceRequestHandler
 * 
 * @author Matthias Müller
 */
@Slf4j
public class RestService {

	private static final int REST_PATH_START_INDEX = 5;

	private AccessibleApplication application;

	public RestService(AccessibleApplication application) {
		this.application = application;
	}

	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		HttpServletRequestWrapper wrapped = getWrappedRequest(servletRequest);
		ApplicationContext context = application.getContext();

		HandlerMethod handlerMethod = null;
		List<HttpMessageConverter<?>> messageConverters = RestConfig.getMessageConverters(context);
		List<HandlerMethodArgumentResolver> argumentResolvers =  RestConfig.getArgumentResolvers(context);
		try {
			RequestMappingHandlerMapping rmhm = new RequestMappingHandlerMapping();
			rmhm.setApplicationContext(context);
			UrlPathHelper urlPathHelper = new UrlPathHelper();
			urlPathHelper.setRemoveSemicolonContent(false);
			rmhm.setUrlPathHelper(urlPathHelper);
			rmhm.afterPropertiesSet();

			HandlerExecutionChain handler = rmhm.getHandler(wrapped);
			if (null == handler) {
				LOGGER.warn("no @RestController found for {}", servletRequest.getServletPath());
				servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
				return;
			}
			handlerMethod = (HandlerMethod) handler.getHandler();

			RequestMappingHandlerAdapter rmha = new RequestMappingHandlerAdapter();
			rmha.setApplicationContext(context);
			rmha.setCustomArgumentResolvers(argumentResolvers);
			rmha.setMessageConverters(messageConverters);
			rmha.afterPropertiesSet();
			rmha.handle(wrapped, servletResponse, handlerMethod);
		} catch (Exception e) {
			servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			ExceptionHandlerExceptionResolver eher = new ExceptionHandlerExceptionResolver();
			eher.setApplicationContext(context);
			eher.setCustomArgumentResolvers(argumentResolvers);

			if (!messageConverters.isEmpty()) {
				eher.setMessageConverters(messageConverters);
			}
			Collection<Object> advices = context.getBeansWithAnnotation(ControllerAdvice.class).values();
			Set<Object> mappedHandlers = new HashSet<>(advices);
			if (null != handlerMethod) {
				mappedHandlers.add(handlerMethod.getBean());
			}
			eher.setMappedHandlers(mappedHandlers);
			eher.afterPropertiesSet();
			eher.resolveException(wrapped, servletResponse, handlerMethod, e);
		}

	}

	protected HttpServletRequestWrapper getWrappedRequest(HttpServletRequest servletRequest) {
		return new HttpServletRequestWrapper(servletRequest) {

			@Override
			public String getServletPath() {
				return getMappedPath(super.getServletPath());
			}

			@Override
			public String getRequestURI() {
				return getMappedPath(super.getRequestURI());
			}

			protected String getMappedPath(String servletPath) {
				String[] pathSegments = servletPath.split(Path.SEPARATOR);
				String path = Path.SEPARATOR + StringUtils.join(
						ArrayUtils.subarray(pathSegments, REST_PATH_START_INDEX, pathSegments.length), Path.SEPARATOR);
				return path;
			}
		};
	}

}
