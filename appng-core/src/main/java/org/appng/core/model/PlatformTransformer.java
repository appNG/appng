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
package org.appng.core.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.model.Properties;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.api.model.Site;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.service.TemplateService;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.OutputFormat;
import org.appng.xml.platform.OutputType;
import org.appng.xml.platform.Platform;
import org.appng.xml.platform.Template;
import org.appng.xml.transformation.StyleSheetProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for transforming a XML document (retrieved from a {@link Platform}-object) to XHTML.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class PlatformTransformer {

	static final String TEMPLATE_XSL = "template.xsl";
	private static final String INSERTION_NODE = "xsl:variables";
	private static final String NO = "no";
	private static final String YES = "yes";
	private static final String MASTER_TYPE = "master";
	private StyleSheetProvider styleSheetProvider;
	private Set<Template> templates;
	private Environment environment;
	private String templatePath;
	private String contentType;
	private OutputFormat outputFormat;
	private OutputType outputType;
	private String prefix;

	@SuppressWarnings("unchecked")
	private static final Map<String, SourceAwareTemplate> STYLESHEETS = Collections.synchronizedMap(new LRUMap(20));

	public PlatformTransformer() {
		this.templates = new HashSet<>();
	}

	/**
	 * Performs the transformation. Therefore a composite XSL-template is used, that is build from the {@link Template}s
	 * of the used {@link OutputType} and those previously added via {@link #addTemplates(List)} (assumed the
	 * {@link OutputType} matches).
	 * 
	 * @param applicationProvider
	 *                            the current {@link ApplicationProvider}
	 * @param platformProperties
	 *                            the platform-{@link Properties}
	 * @param platformXML
	 *                            an XML-string retrieved from a {@link Platform}-object
	 * @param charSet
	 *                            the character-set to used in the returned content-type (see {@link #getContentType()})
	 * @param debugFolder
	 *                            the folder to write debug files to
	 * 
	 * @return the result of the transformation
	 * 
	 * @throws FileNotFoundException
	 *                               if a template XSL-file could not be found
	 * @throws TransformerException
	 *                               when parsing or applying the XSLT template fails
	 */
	public String transform(ApplicationProvider applicationProvider, Properties platformProperties, String platformXML,
			String charSet, File debugFolder) throws IOException, TransformerException {
		InputStream xmlSourceIn = new ByteArrayInputStream(platformXML.getBytes());
		StreamSource xmlSource = new StreamSource(xmlSourceIn);
		boolean deleteIncludes = false;
		// fails if master is not on first position
		for (Template template : outputType.getTemplates()) {
			if (outputTypeMatches(template)) {
				String reference = template.getPath();
				String xslPath = new File(templatePath, "xsl").getAbsolutePath();
				InputStream xslSource = new FileInputStream(new File(xslPath, reference));
				if (MASTER_TYPE.equals(template.getType())) {
					deleteIncludes = Boolean.TRUE.equals(template.isDeleteIncludes());
					getStyleSheetProvider().setMasterSource(xslSource, xslPath);
					getStyleSheetProvider().setName(reference);
					getStyleSheetProvider().setInsertBefore(INSERTION_NODE);
				} else {
					getStyleSheetProvider().addStyleSheet(xslSource, templatePath + ":" + reference);
				}
			}
		}

		Boolean devMode = platformProperties.getBoolean(org.appng.api.Platform.Property.DEV_MODE);

		for (Template template : templates) {
			if (outputTypeMatches(template)) {
				String fileName = template.getPath();
				Resources applicationResourceHolder = applicationProvider.getResources();
				Resource resource = applicationResourceHolder.getResource(ResourceType.XSL, fileName);
				if (null == resource) {
					LOGGER.warn("missing resource: no resource named '{}' is assigned to application '{}'", fileName,
							applicationProvider.getName());
				} else {
					if (devMode) {
						File cachedFile = resource.getCachedFile();
						LOGGER.debug("devMode is active, reading from cached file {}", cachedFile.getAbsolutePath());
						styleSheetProvider.addStyleSheet(new FileInputStream(cachedFile),
								applicationProvider.getName() + ":" + fileName);
					} else {
						styleSheetProvider.addStyleSheet(new ByteArrayInputStream(resource.getBytes()),
								applicationProvider.getName() + ":" + fileName);
					}
				}
			}
		}
		SourceAwareTemplate sourceAwareTemplate = null;
		String styleId = styleSheetProvider.getId();
		String result = null;
		TransformerException transformerException = null;
		Boolean writeDebugFiles = platformProperties.getBoolean(org.appng.api.Platform.Property.WRITE_DEBUG_FILES);
		try {
			ErrorCollector errorCollector = new ErrorCollector();
			if (!devMode && STYLESHEETS.containsKey(styleId)) {
				sourceAwareTemplate = STYLESHEETS.get(styleId);
				styleSheetProvider.cleanup();
				LOGGER.debug("reading templates from cache (id: {})", styleId);
			} else {
				byte[] xslData = styleSheetProvider.getStyleSheet(deleteIncludes, null);
				ByteArrayInputStream templateInputStream = new ByteArrayInputStream(xslData);
				Source xslSource = new StreamSource(templateInputStream);
				TransformerFactory transformerFactory = styleSheetProvider.getTransformerFactory();
				transformerFactory.setErrorListener(errorCollector);
				try {
					Templates templates = transformerFactory.newTemplates(xslSource);
					sourceAwareTemplate = new SourceAwareTemplate(templates, templateInputStream);
				} catch (TransformerConfigurationException tce) {
					sourceAwareTemplate = new SourceAwareTemplate(null, templateInputStream);
					sourceAwareTemplate.errorCollector = errorCollector;
					for (TransformerException t : errorCollector.exceptions) {
						LOGGER.error(t.getMessage(), t);
					}
					if (!devMode) {
						STYLESHEETS.put(styleId, sourceAwareTemplate);
					}
					LOGGER.debug("writing templates to cache (id: {})", styleId);
				}
			}
			if (!errorCollector.hasErrors()) {
				Boolean formatOutput = platformProperties.getBoolean(org.appng.api.Platform.Property.FORMAT_OUTPUT);
				result = transform(xmlSource, sourceAwareTemplate, formatOutput);
				this.contentType = HttpHeaders.getContentType(HttpHeaders.CONTENT_TYPE_TEXT_HTML, charSet);
				if (writeDebugFiles) {
					writeDebugFile(AbstractRequestProcessor.INDEX_HTML, result, debugFolder);
				}
			} else {
				throw errorCollector.exceptions.get(0);
			}
		} catch (TransformerException te) {
			transformerException = new PlatformTransformerException(te, sourceAwareTemplate);
			throw transformerException;
		} finally {
			if (null != transformerException || writeDebugFiles) {
				writeDebugFiles(debugFolder, platformXML, sourceAwareTemplate, transformerException);
			}
		}
		return result;
	}

	class PlatformTransformerException extends TransformerException {
		private SourceAwareTemplate template;

		PlatformTransformerException(TransformerException te, SourceAwareTemplate sat) {
			super(te.getMessage(), te.getLocator(), te.getCause());
			this.template = sat;
		}

		public SourceAwareTemplate getTemplate() {
			return template;
		}
	}

	private void writeDebugFile(String name, String content, File outFolder) throws IOException {
		AbstractRequestProcessor.writeDebugFile(LOGGER, outFolder, name, content);
	}

	protected void writeDebugFiles(File outFolder, String platformXML, SourceAwareTemplate sourceAwareTemplate,
			TransformerException te) {
		try {
			if (null == sourceAwareTemplate || null == sourceAwareTemplate.source) {
				LOGGER.warn("can not write debug files, set 'platform.writeDebugFiles' to 'true' to make this work!");
				return;
			}

			sourceAwareTemplate.source.reset();
			LOGGER.info("writing debug files to {} ", outFolder);

			writeDebugFile(TEMPLATE_XSL, IOUtils.toString(sourceAwareTemplate.source, StandardCharsets.UTF_8),
					outFolder);
			writeDebugFile(AbstractRequestProcessor.PLATFORM_XML, platformXML, outFolder);

			try (StringWriter debugWriter = new StringWriter();
					PrintWriter debugPrintWriter = new PrintWriter(debugWriter)) {
				if (null != te) {
					te.printStackTrace(debugPrintWriter);
				}
				if (null != sourceAwareTemplate.errorCollector) {
					for (TransformerException transformerException : sourceAwareTemplate.errorCollector.exceptions) {
						debugWriter.write("--------------------");
						debugWriter.write(System.lineSeparator());
						transformerException.printStackTrace(debugPrintWriter);
					}
				}
			}
			writeDebugFile(AbstractRequestProcessor.STACKTRACE_TXT, platformXML, outFolder);
		} catch (IOException e) {
			LOGGER.error("error while writing exception details", e);
		}
	}

	protected String getDebugFilePrefix(Date now) {
		return AbstractRequestProcessor.getDebugFilePrefix(now);
	}

	class SourceAwareTemplate implements Templates {

		private final Templates inner;
		final InputStream source;
		ErrorCollector errorCollector;

		SourceAwareTemplate(Templates inner, InputStream source) {
			this.inner = inner;
			this.source = source;
		}

		public java.util.Properties getOutputProperties() {
			return inner.getOutputProperties();
		}

		public Transformer newTransformer() throws TransformerConfigurationException {
			return inner.newTransformer();
		}

	}

	public StyleSheetProvider getStyleSheetProvider() {
		return styleSheetProvider;
	}

	public void setStyleSheetProvider(StyleSheetProvider styleSheetProvider) {
		this.styleSheetProvider = styleSheetProvider;
	}

	/**
	 * Adds the given {@link Template}s to the upcoming transformation.
	 * 
	 * @param templates
	 *                  a list of {@link Template}s
	 */
	public void addTemplates(List<Template> templates) {
		this.templates.addAll(templates);
	}

	private String transform(Source xmlSource, Templates templates, Boolean formatOutput) throws TransformerException {
		StringWriter output = new StringWriter();
		Transformer transformer = templates.newTransformer();
		ErrorCollector errorCollector = new ErrorCollector();
		transformer.setErrorListener(errorCollector);
		// Override the value configured in the XSL
		transformer.setOutputProperty(OutputKeys.INDENT, formatOutput ? YES : NO);
		try {
			transformer.transform(xmlSource, new StreamResult(output));
		} catch (TransformerException transformerException) {
			for (TransformerException te : errorCollector.exceptions) {
				LOGGER.error(te.getMessage(), te);
			}
			throw transformerException;
		}
		return output.toString();
	}

	static class ErrorCollector implements ErrorListener {

		List<TransformerException> exceptions = new ArrayList<>();

		public void warning(TransformerException exception) throws TransformerException {
			exceptions.add(exception);
		}

		public void fatalError(TransformerException exception) throws TransformerException {
			exceptions.add(exception);
		}

		public void error(TransformerException exception) throws TransformerException {
			exceptions.add(exception);
		}

		public boolean hasErrors() {
			return !exceptions.isEmpty();
		}

	}

	private boolean outputTypeMatches(Template template) {
		return template.getOutputType() == null || (template.getOutputType().equals(outputType.getId()));
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Returns the {@link Platform}-object unmarshalled from the template's
	 * {@value org.appng.core.service.TemplateService#PLATFORM_XML}-file. Also determines the {@link OutputType} and
	 * {@link OutputFormat} for the upcoming transformation.
	 * 
	 * @param marshallService
	 *                        the {@link MarshallService} to use for unmarshalling
	 * @param path
	 *                        the current {@link Path}-object
	 * 
	 * @return the {@link Platform}-object
	 * 
	 * @throws InvalidConfigurationException
	 *                                       if the {@value org.appng.core.service.TemplateService#PLATFORM_XML}-file
	 *                                       could net be found or unmarshalled.
	 * 
	 * @see #getOutputFormat()
	 * @see #getOutputType()
	 */
	public Platform getPlatform(MarshallService marshallService, Path path) throws InvalidConfigurationException {
		File platformXML = new File(templatePath, TemplateService.PLATFORM_XML);
		try {
			return marshallService.unmarshall(platformXML, Platform.class);
		} catch (Exception e) {
			throw new InvalidConfigurationException(path.getApplicationName(), "error while reading " + platformXML, e);
		}
	}

	/**
	 * Returns the content-type of the transformation result.
	 * 
	 * @return the content-type
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Sets the path to the active template of the current {@link Site}.
	 * 
	 * @param templatePath
	 *                     the absolute path to the directory where template of the {@link Site} resides
	 */
	public void setTemplatePath(String templatePath) {
		this.templatePath = templatePath;
	}

	/**
	 * Clears the internal template-cache, which must be done if a {@link Site} is being reloaded.
	 */
	public static synchronized void clearCache() {
		STYLESHEETS.clear();
	}

	/**
	 * Returns the {@link OutputFormat} used during transformation
	 * 
	 * @return the {@link OutputFormat}
	 */
	public OutputFormat getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = outputFormat;
	}

	/**
	 * Returns the {@link OutputType} used during transformation
	 * 
	 * @return the {@link OutputType}
	 */
	public OutputType getOutputType() {
		return outputType;
	}

	String getPrefix() {
		return prefix;
	}

	public void setOutputType(OutputType outputType) {
		this.outputType = outputType;
	}

}
