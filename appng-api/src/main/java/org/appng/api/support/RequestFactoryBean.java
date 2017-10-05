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

import java.io.File;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.MessageInterpolator;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.ValidationProvider;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.api.support.validation.LocalizedMessageInterpolator;
import org.appng.forms.XSSUtil;
import org.appng.forms.impl.RequestBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;

/**
 * 
 * A {@link FactoryBean} responsible for initializing a {@link Request}.
 * 
 * @author Matthias Müller
 * 
 */
public class RequestFactoryBean implements FactoryBean<Request>, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestFactoryBean.class);

	private Environment environment;

	private MessageSource messageSource;

	private ConversionService conversionService;

	private HttpServletRequest httpServletRequest;

	private ApplicationRequest request;

	RequestFactoryBean() {
	}

	@Autowired
	public RequestFactoryBean(HttpServletRequest httpServletRequest, Environment environment,
			ConversionService conversionService, MessageSource messageSource) {
		this.request = new ApplicationRequest();
		this.httpServletRequest = httpServletRequest;
		this.environment = environment;
		this.conversionService = conversionService;
		this.messageSource = messageSource;
	}

	public Request getObject() {
		return request;
	}

	public void afterPropertiesSet() {
		RequestSupportImpl requestSupport = new RequestSupportImpl(conversionService, environment, messageSource);
		requestSupport.afterPropertiesSet();
		Properties platformProperties = environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		boolean contraintsAsRule = platformProperties.getBoolean(Platform.Property.CONSTRAINTS_AS_RULE, false);
		MessageInterpolator messageInterpolator = new LocalizedMessageInterpolator(environment.getLocale(),
				messageSource);
		ValidationProvider validationProvider = new DefaultValidationProvider(messageInterpolator, messageSource,
				environment.getLocale(), contraintsAsRule);
		org.appng.forms.Request formRequest = (org.appng.forms.Request) httpServletRequest
				.getAttribute(org.appng.forms.Request.REQUEST_PARSED);
		boolean isPlatformPresent = null != platformProperties;
		if (null == formRequest) {
			formRequest = new RequestBean();
			if (isPlatformPresent) {
				Integer maxUploadSize = platformProperties.getInteger(Platform.Property.MAX_UPLOAD_SIZE);
				String uploadDir = platformProperties.getString(Platform.Property.UPLOAD_DIR);
				ServletContext servletContext = httpServletRequest.getServletContext();
				String realPath = servletContext.getRealPath(uploadDir.startsWith("/") ? uploadDir : "/" + uploadDir);
				if (null == realPath) {
					LOGGER.warn("invalid value for platform property '{}', folder '{}' does not exist!",
							Platform.Property.UPLOAD_DIR, uploadDir);
				} else {
					formRequest.setTempDir(new File(realPath));
				}
				formRequest.setMaxSize(maxUploadSize);
				Site site = RequestUtil.getSite(environment, httpServletRequest);
				if (null != site) {
					String xssExceptions = site.getProperties().getClob(SiteProperties.XSS_EXCEPTIONS);
					String[] exceptions = StringUtils.isNotBlank(xssExceptions) ? xssExceptions.split(StringUtils.LF)
							: new String[0];
					XSSUtil xssUtil = XSSHelper.getXssUtil(platformProperties, exceptions);
					((RequestBean) formRequest).setXssUtil(xssUtil);
				}
			}
			formRequest.process(httpServletRequest);
		}
		request.setRequestSupport(requestSupport);
		request.setValidationProvider(validationProvider);
		request.setWrappedRequest(formRequest);

		List<String> urlParameters = environment.getAttribute(Scope.REQUEST, EnvironmentKeys.JSP_URL_PARAMETERS);
		request.setUrlParameters(urlParameters);

	}

	public Class<?> getObjectType() {
		return Request.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
