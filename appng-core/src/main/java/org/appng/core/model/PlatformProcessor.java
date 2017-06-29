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
package org.appng.core.model;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.core.controller.HttpHeaders;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link RequestProcessor}-implementation, using an XSLT based template.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
public class PlatformProcessor extends AbstractRequestProcessor {

	private static Logger log = LoggerFactory.getLogger(PlatformProcessor.class);
	private PlatformTransformer platformTransformer;

	public PlatformProcessor() {

	}

	@Override
	public void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse, PathInfo pathInfo,
			String templateDir) {
		super.init(servletRequest, servletResponse, pathInfo, templateDir);
		this.platformTransformer.setTemplatePath(templateDir);
	}
	
	@Override
	public org.appng.xml.platform.Platform getPlatform(MarshallService marshallService, Path path)
			throws InvalidConfigurationException {
		org.appng.xml.platform.Platform platform = super.getPlatform(marshallService, path);
		platformTransformer.setOutputType(outputType);
		platformTransformer.setOutputFormat(outputFormat);
		return platform;
	}

	public String processWithTemplate(Site applicationSite) throws InvalidConfigurationException {
		String result = "";
		String platformXML = null;
		org.appng.xml.platform.Platform platform = null;
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String charsetName = platformProperties.getString(Platform.Property.ENCODING);
		Charset charset = Charset.forName(charsetName);
		this.contentType = HttpHeaders.getContentType(HttpHeaders.CONTENT_TYPE_TEXT_XML, charsetName);

		try {
			platform = processPlatform(applicationSite);
			if (isRedirect()) {
				log.debug("request is beeing redirected");
				return "redirect";
			}
			platform.setVersion(env.getAttributeAsString(Scope.PLATFORM, Platform.Environment.APPNG_VERSION));
			platformXML = marshallService.marshal(platform);

			if (platformXML != null) {
				result = platformXML;
				Boolean doXsl = env.getAttribute(Scope.REQUEST, EnvironmentKeys.DO_XSL);
				if (doXsl) {
					platformTransformer.setEnvironment(env);
					ApplicationProvider transformerProvider = getApplicationProvider(applicationSite);
					result = platformTransformer.transform(transformerProvider, platformProperties, platformXML,
							charsetName);
					this.contentType = platformTransformer.getContentType();
				}
			}
		} catch (InvalidConfigurationException ice) {
			throw ice;
		} catch (Exception e) {
			String templateName = applicationSite.getProperties().getString(SiteProperties.TEMPLATE);
			result = handleError(platformProperties, platform, templateName, e);
		} finally {
			platform = null;
		}
		this.contentLength = result.getBytes(charset).length;
		return result;
	}

	Logger logger() {
		return log;
	}

	protected String handleError(Properties platformProperties, org.appng.xml.platform.Platform platform,
			String templateName, Exception e) {
		log.error("error while processing", e);
		servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		contentType = HttpHeaders.CONTENT_TYPE_TEXT_HTML;
		StringWriter stringWriter = new StringWriter();
		stringWriter.append("<!DOCTYPE html><html><body>");
		stringWriter.append("<h2>500 - Internal Server Error</h2>");
		if (platformProperties.getBoolean(Platform.Property.DEV_MODE)) {
			stringWriter.append("Site: " + pathInfo.getSiteName());
			stringWriter.append("<br/>");
			stringWriter.append("Application: " + pathInfo.getApplicationName());
			stringWriter.append("<br/>");
			stringWriter.append("Template: " + templateName);
			stringWriter.append("<br/>");
			stringWriter.append("Thread: " + Thread.currentThread().getName());
			stringWriter.append("<br/>");
			String header = "<h3>%s</h3>";
			String openDiv = "<div style=\"width:100%;height:300px;overflow:auto;border:1px solid grey\"><pre>";
			String closeDiv = "</pre></div>";

			stringWriter.append(String.format(header, "XML"));
			stringWriter.append(openDiv);
			if (platform != null) {
				try {
					stringWriter.append(StringEscapeUtils.escapeHtml4(marshallService.marshallNonRoot(platform)));
				} catch (JAXBException e1) {
					stringWriter.append("error while adding xml: " + e1.getClass().getName() + "-" + e1.getMessage());
				}
			}
			stringWriter.append(closeDiv);
			if (platformProperties.getBoolean(Platform.Property.WRITE_DEBUG_FILES)) {
				String prefix = getPlatformTransformer().getPrefix();
				String rootPath = platformProperties.getString(org.appng.api.Platform.Property.PLATFORM_ROOT_PATH);
				stringWriter.append(String.format(header, "XSLT"));
				stringWriter.append(openDiv);
				try {
					String xslt = FileUtils.readFileToString(new File(rootPath, "debug/" + prefix + "template.xsl"),
							Charset.defaultCharset());
					stringWriter.append(StringEscapeUtils.escapeHtml4(xslt));
				} catch (IOException e1) {
					stringWriter.append("error while adding xsl: " + e1.getClass().getName() + "-" + e1.getMessage());
				}
				stringWriter.append(closeDiv);
			}
			stringWriter.append(String.format(header, "Stacktrace"));
			stringWriter.append(openDiv);
			e.printStackTrace(new PrintWriter(stringWriter));
			stringWriter.append(closeDiv);
			stringWriter.append("</body></html>");
		}
		return stringWriter.toString();
	}

	public PlatformTransformer getPlatformTransformer() {
		return platformTransformer;
	}

	public void setPlatformTransformer(PlatformTransformer platformTransformer) {
		this.platformTransformer = platformTransformer;
	}

	protected void addTemplates(List<Template> templates) {
		platformTransformer.addTemplates(templates);
	}

}
