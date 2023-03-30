/*
 * Copyright 2011-2023 the original author or authors.
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
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.appng.core.model.PlatformTransformer.PlatformTransformerException;
import org.appng.core.model.PlatformTransformer.SourceAwareTemplate;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Template;
import org.slf4j.Logger;

import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link RequestProcessor}-implementation, using an XSLT based template.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Slf4j
public class PlatformProcessor extends AbstractRequestProcessor {

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

	public String processWithTemplate(Site applicationSite, File debugRootFolder) throws InvalidConfigurationException {
		String result = "";
		String platformXML = null;
		org.appng.xml.platform.Platform platform = null;
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String charsetName = platformProperties.getString(Platform.Property.ENCODING);
		Charset charset = Charset.forName(charsetName);
		this.contentType = HttpHeaders.getContentType(HttpHeaders.CONTENT_TYPE_TEXT_XML, charsetName);
		File debugFolder = new File(debugRootFolder, getDebugFilePrefix(new Date()));

		try {
			platform = processPlatform(applicationSite);
			if (isRedirect()) {
				LOGGER.debug("request is beeing redirected");
				return "redirect";
			}
			platform.setVersion(env.getAttributeAsString(Scope.PLATFORM, Platform.Environment.APPNG_VERSION));
			platformXML = marshallService.marshal(platform);

			if (platformXML != null) {
				result = platformXML;
				Boolean render = env.getAttribute(Scope.REQUEST, EnvironmentKeys.RENDER);
				if (render || !applicationSite.getProperties().getBoolean(SiteProperties.ALLOW_SKIP_RENDER)) {
					platformTransformer.setEnvironment(env);
					ApplicationProvider transformerProvider = getApplicationProvider(applicationSite);
					result = platformTransformer.transform(transformerProvider, platformProperties, platformXML,
							charsetName, debugFolder);
					this.contentType = platformTransformer.getContentType();
				}
			}
		} catch (InvalidConfigurationException ice) {
			throw ice;
		} catch (Exception e) {
			String templateName = applicationSite.getProperties().getString(SiteProperties.TEMPLATE);
			result = writeErrorPage(platformProperties, debugFolder, platformXML, templateName, e, platformTransformer);
		} finally {
			platform = null;
		}
		this.contentLength = result.getBytes(charset).length;
		return result;
	}

	Logger logger() {
		return LOGGER;
	}

	protected void writeTemplateToErrorPage(Properties platformProperties, File debugFolder,
			Exception templateException, Object executionContext, StringWriter errorPage) {
		errorPage.append("<h3>XSLT</h3>");
		errorPage.append("<button onclick=\"copy('xslt')\">Copy to clipboard</button>");
		errorPage.append("<div><pre id=\"xslt\">");
		try {
			if (templateException instanceof PlatformTransformerException) {
				SourceAwareTemplate template = PlatformTransformerException.class.cast(templateException).getTemplate();
				template.source.reset();
				String xsl = IOUtils.toString(template.source, StandardCharsets.UTF_8);
				errorPage.append(StringEscapeUtils.escapeHtml4(xsl));
			} else {
				String prefix = getPlatformTransformer().getPrefix();
				File templateFile = new File(debugFolder, prefix + PlatformTransformer.TEMPLATE_XSL);
				if (templateFile.exists()) {
					String xslt = FileUtils.readFileToString(templateFile, StandardCharsets.UTF_8);
					errorPage.append(StringEscapeUtils.escapeHtml4(xslt));
				}
			}
		} catch (IOException e) {
			errorPage.append("error while adding xsl: " + e.getClass().getName() + " - " + e.getMessage());
		}
		errorPage.append("</pre></div>");
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
