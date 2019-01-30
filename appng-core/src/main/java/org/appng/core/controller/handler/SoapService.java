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
package org.appng.core.controller.handler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.model.AccessibleApplication;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.ws.server.MessageDispatcher;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.transport.http.WebServiceMessageReceiverHandlerAdapter;
import org.springframework.ws.transport.http.WsdlDefinitionHandlerAdapter;
import org.springframework.ws.transport.http.XsdSchemaHandlerAdapter;
import org.springframework.ws.wsdl.WsdlDefinition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import lombok.extern.slf4j.Slf4j;

/**
 * This class makes it possible to provide a custom URL-Schema for Spring-WS backed {@link Endpoint}s. The functionality
 * is based upon the implementation of {@link MessageDispatcherServlet}.<br />
 * Some example URLs:
 * <ul>
 * <li>WSDL:<br/>
 * http://localhost:8080/service/manager/some-application/soap/someService/someService.wsdl</li>
 * <li>Schema:<br/>
 * http://localhost:8080/service/manager/some-application/soap/someService/schema.xsd</li>
 * <li>SOAP-Endpoint:<br/>
 * http://localhost:8080/service/manager/some-application/soap/someService</li>
 * </ul>
 * 
 * @see ServiceRequestHandler
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class SoapService {

	private static final String PROP_SCHEMA_LOCATION = "schemaLocation";
	private static final String PROP_CONTEXT_PATH = "contextPath";
	private static final String PROP_LOCATION_URI = "locationUri";
	private static final String PROP_SERVICE_NAME = "serviceName";
	private static final String PROP_PORT_TYPE_NAME = "portTypeName";
	private static final String PORT = "Port";
	private static final String SCHEMA_XSD = "schema.xsd";
	private static final String CONTEXT_FILE = "org/appng/core/soap-service.xml";
	private static final String WSDL_SUFFIX = ".wsdl";
	private static final String XSD_SUFFIX = ".xsd";
	private static final String BEAN_SCHEMA = "schema";
	private static final String SLASH = "/";
	private static final String HTTPS = "https";

	private static final ConcurrentMap<String, Map<String, ConfigurableApplicationContext>> SOAP_CONTEXTS = new ConcurrentHashMap<String, Map<String, ConfigurableApplicationContext>>();

	private Site site;
	private AccessibleApplication application;

	private Environment environment;

	SoapService(Site site, AccessibleApplication application, Environment environment) {
		this.site = site;
		this.application = application;
		this.environment = environment;
	}

	void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		Path pathInfo = environment.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO);
		String serviceName = pathInfo.getService();
		if (null == serviceName) {
			String[] soapServices = application.getBeanNames(org.appng.api.SoapService.class);
			PrintWriter writer = servletResponse.getWriter();
			writer.write("<html><head/><body>");
			for (String service : soapServices) {
				writer.write("<a href=\"");
				String wsdlUri = getLocationUri(service, servletRequest) + SLASH + service + WSDL_SUFFIX;
				writer.write(wsdlUri);
				writer.write("\">" + wsdlUri + "</a><br/>");
			}
			writer.write("</body></html>");
			servletResponse.setContentType(HttpHeaders.CONTENT_TYPE_TEXT_HTML);
			servletResponse.setStatus(HttpServletResponse.SC_OK);
			LOGGER.debug("listing SOAP services for site '{}' application '{}'", site.getName(), application.getName());
			return;
		}

		String fileName = pathInfo.getLastElement();
		boolean isWsdl = fileName.equals(serviceName + WSDL_SUFFIX);
		boolean isXsd = fileName.endsWith(XSD_SUFFIX);
		boolean isPost = HttpMethod.POST.name().equalsIgnoreCase(servletRequest.getMethod());

		HandlerAdapter handlerAdapter = null;
		Object handler = null;
		String message = null;
		Object[] messageArgs = new Object[] { fileName, serviceName, site.getName(), application.getName() };

		if (isWsdl || isXsd || isPost) {

			StopWatch stopWatch = new StopWatch(
					"retrieving SOAP-context for " + site.getName() + ":" + application.getName() + ":" + serviceName);
			stopWatch.start();

			org.appng.api.SoapService soapService = application.getBean(serviceName, org.appng.api.SoapService.class);
			if (null == soapService) {
				throw new BusinessException("invalid SOAP service requested: " + serviceName);
			}
			soapService.setSite(site);
			soapService.setApplication(application);
			soapService.setEnvironment(environment);
			LOGGER.debug("found SoapService of type {}", soapService.getClass().getName());

			Map<String, ConfigurableApplicationContext> servicesOfSite = SOAP_CONTEXTS.get(site.getName());
			if (null == servicesOfSite) {
				servicesOfSite = new HashMap<>();
				SOAP_CONTEXTS.put(site.getName(), servicesOfSite);
			}
			String serviceId = application.getName() + "#" + serviceName;
			ConfigurableApplicationContext soapContext = servicesOfSite.get(serviceId);

			if (null == soapContext) {
				String locationUri = getLocationUri(serviceName, servletRequest);
				PropertyResourceConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
				Properties properties = new Properties();
				properties.put(PROP_SCHEMA_LOCATION, soapService.getSchemaLocation());
				properties.put(PROP_CONTEXT_PATH, soapService.getContextPath());
				properties.put(PROP_LOCATION_URI, locationUri);
				properties.put(PROP_SERVICE_NAME, serviceName);
				properties.put(PROP_PORT_TYPE_NAME, serviceName + PORT);
				configurer.setProperties(properties);
				soapContext = new ClassPathXmlApplicationContext(new String[] { CONTEXT_FILE }, false,
						application.getContext());
				soapContext.addBeanFactoryPostProcessor(configurer);
				soapContext.setId(site.getName() + "#" + serviceId);
				soapContext.refresh();
				LOGGER.debug("created SOAP-context {}", soapContext);
				servicesOfSite.put(serviceId, soapContext);
			}

			stopWatch.stop();
			LOGGER.debug(stopWatch.shortSummary());

			if (isWsdl) {
				handlerAdapter = soapContext.getBean(WsdlDefinitionHandlerAdapter.class);
				handler = BeanFactoryUtils.beanOfType(soapContext, WsdlDefinition.class);
				message = "returning '{}' for SOAP service '{}' of site '{}', application '{}'";
			} else if (isXsd) {
				handlerAdapter = new XsdSchemaHandlerAdapter();
				if (SCHEMA_XSD.equals(fileName)) {
					handler = soapContext.getBean(BEAN_SCHEMA, XsdSchema.class);
				} else {
					ClassPathResource xsdResource = new ClassPathResource(fileName);
					if (xsdResource.exists()) {
						handler = new SimpleXsdSchema(xsdResource);
						((InitializingBean) handler).afterPropertiesSet();
					} else {
						LOGGER.warn("no such resource: {}", xsdResource);
					}
				}
				message = "returning '{}' for SOAP service '{}' of site '{}', application '{}'";
			} else if (isPost) {
				handlerAdapter = soapContext.getBean(WebServiceMessageReceiverHandlerAdapter.class);
				handler = soapContext.getBean(MessageDispatcher.class);
				Class<?> endpointClass = soapContext.getType(serviceName);
				Endpoint annotation = AnnotationUtils.findAnnotation(endpointClass, Endpoint.class);
				if (null == annotation) {
					LOGGER.warn("did not find @Endpoint-annotation on {}!", endpointClass);
				}
				message = "calling SOAP service '{}' of site '{}', application '{}'";
				messageArgs = ArrayUtils.subarray(messageArgs, 1, 4);
			}

		}
		if (null != handler && null != handlerAdapter) {
			handlerAdapter.handle(servletRequest, servletResponse, handler);
			LOGGER.debug(message, messageArgs);
		} else {
			LOGGER.debug("not a valid SOAP request: [{}] {}", servletRequest.getMethod(), pathInfo.getServletPath());
			servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private String getLocationUri(String serviceName, HttpServletRequest request) {
		String servicePath = site.getProperties().getString(SiteProperties.SERVICE_PATH);
		String domain = site.getDomain();
		if (!domain.startsWith(HTTPS) && HttpHeaders.isRequestSecure(request)) {
			domain = HTTPS + domain.substring(4, domain.length());
		}
		return domain + servicePath + SLASH + site.getName() + SLASH + application.getName() + SLASH
				+ Platform.SERVICE_TYPE_SOAP + SLASH + serviceName;
	}

	public static synchronized void clearCache(String siteName) {
		Map<String, ConfigurableApplicationContext> contextMap = SOAP_CONTEXTS.remove(siteName);
		if (null != contextMap) {
			for (ConfigurableApplicationContext context : contextMap.values()) {
				context.close();
				LOGGER.debug("closed SOAP-context {}", context);
			}
			contextMap.clear();
		}
	}

}
