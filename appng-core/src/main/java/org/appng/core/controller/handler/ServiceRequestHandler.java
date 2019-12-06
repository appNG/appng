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

import static org.appng.api.Platform.SERVICE_TYPE_ACTION;
import static org.appng.api.Platform.SERVICE_TYPE_DATASOURCE;
import static org.appng.api.Platform.SERVICE_TYPE_REST;
import static org.appng.api.Platform.SERVICE_TYPE_SOAP;
import static org.appng.api.Platform.SERVICE_TYPE_WEBSERVICE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLClassLoader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.appng.api.AttachmentWebservice;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.Webservice;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.ElementHelper;
import org.appng.api.support.HttpHeaderUtils;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.AbstractRequestProcessor;
import org.appng.core.model.AccessibleApplication;
import org.appng.core.model.ApplicationProvider;
import org.appng.core.model.PlatformTransformer;
import org.appng.core.service.TemplateService;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ApplicationReference;
import org.appng.xml.platform.Content;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.Output;
import org.appng.xml.platform.OutputFormat;
import org.appng.xml.platform.OutputType;
import org.appng.xml.platform.PageReference;
import org.appng.xml.platform.PagesReference;
import org.appng.xml.platform.Section;
import org.appng.xml.platform.Sectionelement;
import org.appng.xml.platform.Structure;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link RequestHandler} which handles {@link HttpServletRequest}s for different types of services.<br/>
 * The schema for a complate path to a service is
 * <p>
 * {@code <site-domain>/<service-path>/<site-name>/<application-name>/<service-type/<service-name>/<additional-params>}
 * </p>
 * The service-path is configurable, see {@link SiteProperties#SERVICE_PATH}.<br/>
 * Supported service-types are:<br/>
 * <ul>
 * <li><b>webservice</b><br/>
 * Used for calling a {@link Webservice} or {@link AttachmentWebservice}.<br/>
 * Example:
 * <ul>
 * <li>http://localhost:8080/service/manager/appng-manager/webservice/logViewer&lt;get-params>
 * </ul>
 * 
 * <li><b>datasource</b><br/>
 * Used for calling a datasource provided by a {@link Application}.<br/>
 * Provides different formats: json,xml and html.<br/>
 * Examples:
 * <ul>
 * <li>http://localhost:8080/service/manager/appng-manager/datasource/xml/sites
 * <li>http://localhost:8080/service/manager/appng-manager/datasource/json/sites
 * <li>http://localhost:8080/service/manager/appng-manager/datasource/html/sites
 * </ul>
 * 
 * <li><b>action</b><br/>
 * Used for calling an action provided by a {@link Application}.<br/>
 * Provides different formats: json,xml and html.<br/>
 * Examples:
 * <ul>
 * <li>http://localhost:8080/service/manager/appng-manager/action/xml/siteEvent/create?form_action=create
 * <li>http://localhost:8080/service/manager/appng-manager/action/json/siteEvent/create?form_action=create
 * <li>http://localhost:8080/service/manager/appng-manager/action/html/siteEvent/create?form_action=create
 * </ul>
 * 
 * <li><b>soap</b><br/>
 * Used for calling a {@link org.appng.api.SoapService} provided by a {@link Application}.<br/>
 * Example (GET for the wsdl):
 * <ul>
 * <li>http://localhost:8080/service/manager/appng-demoapplication/soap/PersonService/PersonService.wsdl
 * </ul>
 * POST for the SOAP-Request:
 * <ul>
 * <li>http://localhost:8080/service/manager/appng-demoapplication/soap/PersonService
 * </ul>
 * 
 * <li><b>rest</b><br/>
 * Used for addressing a {@link org.springframework.web.bind.annotation.RestController} offered by an
 * {@link Application}<br/>
 * Example:
 * <ul>
 * <li>http://localhost:8080/service/manager/appng-manager/rest/sites
 * <li>http://localhost:8080/service/manager/appng-manager/rest/site/1
 * </ul>
 * </ul>
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class ServiceRequestHandler implements RequestHandler {

	protected static final String FORMAT_JSON = "json";
	protected static final String FORMAT_HTML = "html";
	protected static final String FORMAT_XML = "xml";
	private MarshallService marshallService;
	private PlatformTransformer transformer;
	private final File debugFolder;

	public ServiceRequestHandler(MarshallService marshallService, PlatformTransformer transformer, File debugFolder) {
		this.marshallService = marshallService;
		this.transformer = transformer;
		this.debugFolder = debugFolder;
	}

	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment environment,
			Site site, PathInfo path) throws IOException {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			if (path.isService()) {

				// TODO support for outputformat/-type
				String siteName = path.getSiteName();
				String applicationName = path.getApplicationName();
				String serviceType = path.getElementAt(path.getApplicationIndex() + 1);

				Site siteToUse = RequestUtil.waitForSite(environment, siteName);
				if (null == siteToUse) {
					LOGGER.warn("No such site: '{}', returning {} (path: {})", siteName, HttpStatus.NOT_FOUND.value(),
							path.getServletPath());
					servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
					return;
				} else if (!siteToUse.hasState(SiteState.STARTED)) {
					LOGGER.warn("Site '{}' is in state {}, returning {} (path: {})", siteName, siteToUse.getState(),
							HttpStatus.SERVICE_UNAVAILABLE.value(), path.getServletPath());
					servletResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
					return;
				}
				URLClassLoader siteClassLoader = siteToUse.getSiteClassLoader();
				Thread.currentThread().setContextClassLoader(siteClassLoader);
				ApplicationProvider application = (ApplicationProvider) ((SiteImpl) siteToUse)
						.getSiteApplication(applicationName);
				if (null == application) {
					LOGGER.warn("No such application '{}' for site '{}' returning {} (path: {})", applicationName,
							siteName, HttpStatus.NOT_FOUND.value(), path.getServletPath());
					servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
					return;
				}
				ApplicationRequest applicationRequest = application.getApplicationRequest(servletRequest,
						servletResponse);

				String result = null;
				String contenttype = null;

				boolean applyPermissionsOnServiceRef = site.getProperties().getBoolean("applyPermissionsOnServiceRef",
						true);

				if (SERVICE_TYPE_ACTION.equals(serviceType)) {
					path.checkPathLength(8);
					String format = path.getElementAt(path.getApplicationIndex() + 2);
					String eventId = path.getElementAt(path.getApplicationIndex() + 3);
					String actionId = path.getElementAt(path.getApplicationIndex() + 4);
					Action action = application.processAction(servletResponse, applyPermissionsOnServiceRef,
							applicationRequest, actionId, eventId, marshallService);
					if (null != action) {
						LOGGER.debug("calling event '{}', action '{}' of application '{}', format: {}", eventId,
								actionId, applicationName, format);
						if (FORMAT_XML.equals(format)) {
							result = marshallService.marshallNonRoot(action);
							contenttype = MediaType.TEXT_XML_VALUE;
						} else if (FORMAT_HTML.equals(format)) {
							result = processPlatform(environment, path, siteToUse, application, action);
							contenttype = MediaType.TEXT_HTML_VALUE;
						} else if (FORMAT_JSON.equals(format)) {
							result = writeJson(new JsonWrapper(action));
							contenttype = MediaType.APPLICATION_JSON_VALUE;
						} else {
							servletResponse.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
						}
					} else {
						LOGGER.debug("event not present or no permission ('{}', action '{}' of application '{}')",
								eventId, actionId, applicationName);
					}
				} else if (SERVICE_TYPE_DATASOURCE.equals(serviceType)) {
					path.checkPathLength(7);
					String format = path.getElementAt(path.getApplicationIndex() + 2);
					String dataSourceId = path.getElementAt(path.getApplicationIndex() + 3);
					Datasource datasource = application.processDataSource(servletResponse, applyPermissionsOnServiceRef,
							applicationRequest, dataSourceId, marshallService);
					if (null != datasource) {
						boolean hasErrors = addMessagesToDatasource(environment, site, application, datasource);
						if (hasErrors) {
							LOGGER.debug(
									"Datasource has been processed an error messages found in session. Set return code to 400");
							servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
						}
						LOGGER.debug("calling datasource '{}' of application '{}', format: {}", dataSourceId,
								applicationName, format);
						if (FORMAT_XML.equals(format)) {
							result = marshallService.marshallNonRoot(datasource);
							contenttype = MediaType.TEXT_XML_VALUE;
						} else if (FORMAT_HTML.equals(format)) {
							result = processPlatform(environment, path, siteToUse, application, datasource);
							contenttype = MediaType.TEXT_HTML_VALUE;
						} else if (FORMAT_JSON.equals(format)) {
							result = writeJson(new JsonWrapper(datasource));
							contenttype = MediaType.APPLICATION_JSON_VALUE;
						} else {
							servletResponse.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
						}
					} else {
						LOGGER.debug("datasource not present or no permission ('{}' in application '{}')", dataSourceId,
								applicationName);
					}
				} else if (SERVICE_TYPE_WEBSERVICE.equals(serviceType)) {
					path.checkPathLength(6);
					String webserviceName = path.getService();
					callWebservice(servletRequest, servletResponse, applicationRequest, environment, siteToUse,
							application, webserviceName);
				} else if (SERVICE_TYPE_SOAP.equals(serviceType)) {
					path.checkPathLength(5);
					handleSoap(siteToUse, application, environment, servletRequest, servletResponse);
				} else if (SERVICE_TYPE_REST.equals(serviceType)) {
					path.checkPathLength(6);
					handleRest(siteToUse, application, environment, servletRequest, servletResponse);
				} else {
					LOGGER.warn("unknown service type: {}", serviceType);
				}
				if (null != result) {
					servletResponse.setContentType(contenttype);
					servletResponse.getOutputStream().write(result.getBytes());
					servletResponse.getOutputStream().close();
					LOGGER.debug("set content_type to {}", contenttype);
				}
			}
		} catch (Exception e) {
			String queryString = servletRequest.getQueryString();
			String pathWithQuery = servletRequest.getServletPath() + (null == queryString ? "" : "?" + queryString);
			LOGGER.error(String.format("error while processing service-request %s", pathWithQuery), e);
			servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			servletResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
			servletResponse.getWriter().write("an error occured");
			servletResponse.getWriter().close();
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	private boolean addMessagesToDatasource(Environment environment, Site site, ApplicationProvider application,
			Datasource datasource) {
		// Messages added to the FieldProcessor during processing of the datasource are normally not added
		// to the Datasource if it is called with the GuiHandler. Those messages are added to the page. When a
		// datasource is called as a service, we have to put them into the datasource and remove them from session.
		ElementHelper elementHelper = new ElementHelper(site, application);
		Messages messages = elementHelper.removeMessages(environment);
		if (null != messages) {
			datasource.setMessages(messages);
			return messages.getMessageList().stream().filter(m -> MessageType.ERROR.equals(m.getClazz())).findAny()
					.isPresent();
		}
		return false;
	}

	protected String processPlatform(Environment environment, Path path, Site siteToUse,
			ApplicationProvider application, Object element) throws InvalidConfigurationException,
			ParserConfigurationException, JAXBException, TransformerException, FileNotFoundException, IOException {
		Properties platformProperties = environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String charsetName = platformProperties.getString(Platform.Property.ENCODING);
		String platformXml = retrievePlatform(environment, path, siteToUse, element, platformProperties);
		return transformer.transform(application, platformProperties, platformXml, charsetName, debugFolder);
	}

	protected String retrievePlatform(Environment environment, Path path, Site siteToUse, Object element,
			Properties platformProperties)
			throws InvalidConfigurationException, ParserConfigurationException, JAXBException, TransformerException {
		transformer.setEnvironment(environment);
		Properties siteProperties = siteToUse.getProperties();
		File templateRepoFolder = TemplateService.getTemplateRepoFolder(platformProperties, siteProperties);

		transformer.setTemplatePath(templateRepoFolder.getAbsolutePath());
		org.appng.xml.platform.Platform platform = transformer.getPlatform(marshallService, path);
		AbstractRequestProcessor.initPlatform(platform, environment, path);
		String format = siteProperties.getString(SiteProperties.SERVICE_OUTPUT_FORMAT);
		String type = siteProperties.getString(SiteProperties.SERVICE_OUTPUT_TYPE);

		Output output = new Output();
		output.setFormat(format);
		output.setType(type);
		platform.getConfig().setOutput(output);

		outer: for (OutputFormat of : platform.getConfig().getOutputFormat()) {
			if (format.equals(of.getId())) {
				for (OutputType ot : of.getOutputType()) {
					if (type.equals(ot.getId())) {
						transformer.setOutputType(ot);
						break outer;
					}
				}
			}
		}

		Content content = new Content();
		platform.setContent(content);
		content.setApplication(new ApplicationReference());
		PagesReference pagesRef = new PagesReference();
		content.getApplication().setPages(pagesRef);
		PageReference pageRef = new PageReference();
		pagesRef.getPage().add(pageRef);
		Structure struct = new Structure();
		pageRef.setStructure(struct);
		Section sect = new Section();
		struct.getSection().add(sect);
		Sectionelement sel = new Sectionelement();
		sect.getElement().add(sel);
		if (element instanceof Datasource) {
			sel.setDatasource((Datasource) element);
		} else if (element instanceof Action) {
			sel.setAction((Action) element);
		}
		return marshallService.marshal(platform);
	}

	protected String writeJson(Object data) throws IOException, JsonGenerationException, JsonMappingException {
		ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(Include.NON_EMPTY);
		StringWriter stringWriter = new StringWriter();
		objectMapper.writer().withDefaultPrettyPrinter().writeValue(stringWriter, data);
		return stringWriter.toString();
	}

	protected void handleSoap(Site site, AccessibleApplication application, Environment environment,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		new SoapService(site, application, environment).handle(servletRequest, servletResponse);
	}

	protected void handleRest(Site site, AccessibleApplication application, Environment environment,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
		new RestService(site, application, environment).handle(servletRequest, servletResponse);
	}

	protected void callWebservice(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			ApplicationRequest applicationRequest, Environment env, Site site, ApplicationProvider application,
			String webserviceName) throws BusinessException, IOException {
		Webservice webservice = application.getBean(webserviceName, Webservice.class);
		if (null == webservice) {
			LOGGER.error("no webservice '{}' for application '{}' in site '{}'", webserviceName, application.getName(),
					site.getName());
			servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		LOGGER.debug("calling  webservice '{}' of application '{}' in site {}", webserviceName, application.getName(),
				site.getName());

		application.setPlatformScope();

		byte[] data = webservice.processRequest(site, application, env, applicationRequest);
		servletResponse.setStatus(webservice.getStatus());
		HttpHeaderUtils.applyHeaders(servletResponse, webservice.getHeaders());

		if (null != data) {
			int contentLength = data.length;
			servletResponse.setContentLength(contentLength);
			if (webservice instanceof AttachmentWebservice) {
				AttachmentWebservice attachmentWebservice = (AttachmentWebservice) webservice;
				String contentType = webservice.getContentType();
				String fileName = attachmentWebservice.getFileName();
				if (null != fileName) {
					if (null == contentType) {
						contentType = servletRequest.getServletContext().getMimeType(fileName);
					}
					servletResponse.setContentType(contentType);
					if (attachmentWebservice.isAttachment()) {
						String attachment = "attachment; filename=\"" + fileName + "\"";
						servletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, attachment);
					}
				} else {
					servletResponse.setContentType(contentType);
				}
				servletResponse.getOutputStream().write(data);
				servletResponse.getOutputStream().close();
			} else {
				String contentType = webservice.getContentType();
				servletResponse.setContentType(contentType);
				String string = new String(data);
				servletResponse.getWriter().write(string);
				servletResponse.getWriter().close();
			}
		}
	}

	class JsonWrapper {
		private Action action;
		private Datasource datasource;

		JsonWrapper(Action action) {
			this.action = action;
		}

		JsonWrapper(Datasource datasource) {
			this.datasource = datasource;
		}

		public Action getAction() {
			return action;
		}

		public Datasource getDatasource() {
			return datasource;
		}

	}

}
