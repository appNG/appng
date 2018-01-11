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
package org.appng.taglib;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.GlobalTaglet;
import org.appng.api.GlobalXMLTaglet;
import org.appng.api.PageProcessor;
import org.appng.api.Scope;
import org.appng.api.Taglet;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.XMLTaglet;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.ElementHelper;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.ApplicationReference;
import org.appng.xml.platform.Content;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.PageReference;
import org.appng.xml.platform.PagesReference;
import org.appng.xml.platform.Platform;
import org.appng.xml.platform.PlatformConfig;
import org.appng.xml.platform.Section;
import org.appng.xml.platform.Sectionelement;
import org.appng.xml.platform.Structure;
import org.appng.xml.transformation.StyleSheetProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TagletProcessor} is responsible for handling taglet-calls from a JSP
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 * 
 * @see Taglet
 * @see GlobalTaglet
 * @see XMLTaglet
 * @see GlobalXMLTaglet
 */
public class TagletProcessor {

	private static final String XSL = "xsl";
	private static final String NO_XSL_PREFIX = "noXslPrefix";
	private static final String NO_XSL_SUFFIX = "noXslSuffix";
	private static final String DEFAULT_NO_XSL_PREFIX = "<!--";
	private static final String DEFAULT_NO_XSL_SUFFIX = "-->";
	private static final String XML = "xml";

	private static Logger log = LoggerFactory.getLogger(TagletProcessor.class);
	private MarshallService marshallService;
	private StyleSheetProvider styleSheetProvider;

	/**
	 * Performs the actual taglet-call.
	 * 
	 * @param callingSite
	 *            the {@link Site} where the taglet-call happened
	 * @param executingSite
	 *            the {@link Site} that actually executes the taglet
	 * @param application
	 *            the {@link Application} from where the taglet comes from
	 * @param tagletAttributes
	 *            the attributes of the taglet-call
	 * @param applicationRequest
	 *            the current {@link Request}
	 * @param methodName
	 *            the name of the taglet to be called
	 * @param type
	 *            the type of the taglet, may be {@code text} or {@code xml}
	 * @param out
	 *            a {@link Writer} used to write the output of the taglet
	 * @return {@code true} if the JSP should be further processed, {@code false} otherwise
	 * @throws JAXBException
	 * @throws TransformerConfigurationException
	 * @throws FileNotFoundException
	 * @throws BusinessException
	 */
	public boolean perform(Site callingSite, Site executingSite, Application application,
			Map<String, String> tagletAttributes, org.appng.api.Request applicationRequest, String methodName,
			String type, Writer out) throws JAXBException, TransformerConfigurationException, FileNotFoundException,
			BusinessException {
		boolean processPage;

		if (XML.equalsIgnoreCase(type)) {
			processPage = processXmlTaglet(callingSite, executingSite, application, tagletAttributes,
					applicationRequest, methodName, out);
		} else {
			String result;
			Taglet taglet = application.getBean(methodName, Taglet.class);
			log.debug("calling taglet '{}' of type '{}' with attributes: {}", methodName, taglet.getClass().getName(),
					tagletAttributes);
			if (taglet instanceof GlobalTaglet) {
				result = ((GlobalTaglet) taglet).processTaglet(callingSite, executingSite, application,
						applicationRequest, tagletAttributes);
			} else {
				if (!callingSite.equals(executingSite)) {
					logNotGlobalWarning(callingSite, executingSite, taglet.getClass().getName(),
							GlobalTaglet.class.getName());
				}
				result = taglet.processTaglet(callingSite, application, applicationRequest, tagletAttributes);
			}
			doWrite(out, result, application, methodName);
			processPage = doProcessPage(taglet);
		}
		return processPage;
	}

	protected void doWrite(Writer out, String result, Application application, String methodName) {
		try {
			out.write(result);
		} catch (IOException ex) {
			log.error("Error writing result of Taglet '" + methodName + "' in application '" + application + "' ", ex);
		}
	}

	private boolean doProcessPage(Object taglet) {
		if (taglet instanceof PageProcessor) {
			return ((PageProcessor) taglet).processPage();
		}
		return true;
	}

	private boolean processXmlTaglet(Site callingSite, Site executingSite, Application application,
			Map<String, String> tagletAttributes, org.appng.api.Request applicationRequest, String methodName,
			Writer out) throws BusinessException, JAXBException, FileNotFoundException,
			TransformerConfigurationException {
		XMLTaglet xmltaglet = application.getBean(methodName, XMLTaglet.class);
		log.debug("calling taglet '{}' of type '{}' width attributes: {}", methodName, xmltaglet.getClass().getName(),
				tagletAttributes);
		DataContainer container = null;
		if (xmltaglet instanceof GlobalXMLTaglet) {
			container = ((GlobalXMLTaglet) xmltaglet).processTaglet(callingSite, executingSite, application,
					applicationRequest, tagletAttributes);
		} else {
			if (!callingSite.equals(executingSite)) {
				logNotGlobalWarning(callingSite, executingSite, xmltaglet.getClass().getName(),
						GlobalXMLTaglet.class.getName());
			}
			container = xmltaglet.processTaglet(callingSite, application, applicationRequest, tagletAttributes);
		}
		boolean processPage = doProcessPage(xmltaglet);
		try {
			ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(applicationRequest.getParameters());
			ElementHelper elementHelper = new ElementHelper(executingSite, application, expressionEvaluator);
			elementHelper.processDataContainer(applicationRequest, container, xmltaglet.getClass().getName());
		} catch (ClassNotFoundException | ProcessingException e) {
			log.error("error while processing " + methodName, e);
			throw new BusinessException(e);
		}

		Platform platform = getPlatform(container.getWrappedData(), callingSite.getDomain());
		StringWriter writer = new StringWriter();
		marshallService.marshalNoValidation(platform, writer);
		String xmlResult = writer.toString();
		Boolean doXsl = applicationRequest.getEnvironment().getAttribute(Scope.REQUEST, EnvironmentKeys.DO_XSL);
		if (doXsl) {
			String xsl = tagletAttributes.get(XSL);
			if (StringUtils.isNotBlank(xsl)) {
				File xslFile = callingSite.readFile(xsl);
				if (xslFile.exists()) {
					FileInputStream xslSource = new FileInputStream(xslFile);
					styleSheetProvider.setMasterSource(xslSource, xslFile.getParent());
					styleSheetProvider.setName(xsl);
					styleSheetProvider.setInsertBefore("xsl:variables");
					transform(xmlResult, out);
				} else {
					log.error("The xsl file " + xslFile.getAbsolutePath() + " does not exist or is invalid! xsl name: "
							+ xsl);
				}
			} else {
				log.error("parameter 'xsl' not set, can not transform data");
			}
		} else {
			String prefix = null;
			String suffix = null;
			if (tagletAttributes.containsKey(NO_XSL_PREFIX) && tagletAttributes.containsKey(NO_XSL_SUFFIX)) {
				prefix = tagletAttributes.get(NO_XSL_PREFIX);
				suffix = tagletAttributes.get(NO_XSL_SUFFIX);
			} else {
				log.debug("No prefix and suffix defined for not transformed output. Using default values");
				prefix = DEFAULT_NO_XSL_PREFIX;
				suffix = DEFAULT_NO_XSL_SUFFIX;
			}
			log.debug("Using prefix {} and suffix {} for not transformed output.", prefix, suffix);
			doWrite(out, prefix + "\n" + xmlResult + "\n" + suffix, application, methodName);
		}
		return processPage;
	}

	private void logNotGlobalWarning(Site callingSite, Site executingSite, String tagletClass, String globalName) {
		String message = "the taglet {} does not implement {}, and the calling site {} is not the same as the executing site {}. This may result in unexpected behavior!";
		log.warn(message, tagletClass, globalName, callingSite.getName(), executingSite.getName());
	}

	private Platform getPlatform(Data data, String baseUrl) {
		Platform platform = new Platform();
		PlatformConfig masterConfig = new PlatformConfig();
		platform.setConfig(masterConfig);
		masterConfig.setBaseUrl(baseUrl);
		Content content = new Content();
		platform.setContent(content);
		ApplicationReference applicationRef = new ApplicationReference();
		content.setApplication(applicationRef);
		PagesReference pagesRef = new PagesReference();
		applicationRef.setPages(pagesRef);
		PageReference pageRef = new PageReference();
		pagesRef.getPage().add(pageRef);
		Structure structure = new Structure();
		pageRef.setStructure(structure);
		Section section = new Section();
		structure.getSection().add(section);
		Sectionelement sectionElement = new Sectionelement();
		section.getElement().add(sectionElement);
		Datasource datasource = new Datasource();
		sectionElement.setDatasource(datasource);
		datasource.setData(data);
		return platform;
	}

	private void transform(String xmlResult, Writer out) throws TransformerConfigurationException {
		try {
			InputStream xmlSourceIn = new ByteArrayInputStream(xmlResult.getBytes());
			StreamSource xmlSource = new StreamSource(xmlSourceIn);

			byte[] xslData = styleSheetProvider.getStyleSheet(false, null);
			Source xslSource = new StreamSource(new ByteArrayInputStream(xslData));
			Templates templates = styleSheetProvider.getTransformerFactory().newTemplates(xslSource);

			templates.newTransformer().transform(xmlSource, new StreamResult(out));
		} catch (TransformerException te) {
			log.error("Error during XSL Transformation: ", te);
		}
	}

	public MarshallService getMarshallService() {
		return marshallService;
	}

	public void setMarshallService(MarshallService marshallService) {
		this.marshallService = marshallService;
	}

	public StyleSheetProvider getStyleSheetProvider() {
		return styleSheetProvider;
	}

	public void setStyleSheetProvider(StyleSheetProvider styleSheetProvider) {
		this.styleSheetProvider = styleSheetProvider;
	}

}
