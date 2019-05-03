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
package org.appng.xml.transformation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Transformer {

	private static final String PLATFORM_XSL = "platform.xsl";
	private static final String XSL_PLATFORM_XSL = "xsl/" + PLATFORM_XSL;
	private static final String EXT_XML = ".xml";
	private static final String EXT_XSL = ".xsl";
	private static final String YES = "yes";
	private static final String NO = "no";

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("usage: Transformer <template-source> <source-file> <output-file>");
			return;
		}
		String templateSource = args[0];
		File templateFolder = new File(templateSource);
		if (!templateFolder.exists()) {
			System.err.println("No such folder: " + templateSource);
			return;
		}

		String xmlSource = args[1];
		File xmlFile = new File(xmlSource);
		if (!xmlFile.exists()) {
			System.err.println("No such file: " + xmlSource);
			return;
		}

		File outFile = new File(args[2]);
		if (outFile.isDirectory()) {
			System.err.println("<output-file> needs to be a file, not a directory!");
			return;
		}
		File masterFile = new File(templateSource, XSL_PLATFORM_XSL);
		if (!masterFile.exists()) {
			System.err.println("platform.xsl not found at " + templateSource);
			return;
		}

		new Transformer().run(templateFolder, xmlFile, outFile);

	}

	public void run(File templateFolder, File xmlSource, File outputFile) throws Exception {
		outputFile = outputFile.getAbsoluteFile();
		templateFolder = templateFolder.getAbsoluteFile();
		xmlSource = xmlSource.getAbsoluteFile();

		LOGGER.info("using template at {}", templateFolder);
		// System.setProperty("jaxp.debug", "true");
		StyleSheetProvider styleSheetProvider = new StyleSheetProvider();
		File platformXsl = new File(templateFolder, XSL_PLATFORM_XSL);
		styleSheetProvider.setMasterSource(new FileInputStream(platformXsl), templateFolder.getAbsolutePath());
		LOGGER.info("using {}", platformXsl);
		String[] xslFiles = new File(templateFolder, "xsl").list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(EXT_XSL) && !name.equals(PLATFORM_XSL);
			}
		});

		if (null != xslFiles) {
			for (String file : xslFiles) {
				String name = file.substring(0, file.length() - 4);
				File xslFile = new File(templateFolder + "/xsl", file);
				styleSheetProvider.addStyleSheet(new FileInputStream(xslFile), name);
			}
		}
		styleSheetProvider.setName("transformer");
		styleSheetProvider.setInsertBefore("xsl:variables");
		styleSheetProvider.setDocumentBuilderFactory(DocumentBuilderFactory.newInstance());
		TransformerFactory tf = TransformerFactory.newInstance();
		tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		styleSheetProvider.setTransformerFactory(tf);
		styleSheetProvider.init();

		outputFile.getParentFile().mkdirs();
		File styleSheet = new File(outputFile.getParent(), xmlSource.getName().replace(EXT_XML, EXT_XSL));
		FileOutputStream xslOut = new FileOutputStream(styleSheet);
		LOGGER.info("Writing styleSheet to {}", styleSheet);
		byte[] xslData = styleSheetProvider.getStyleSheet(true, xslOut);
		Source xslSource = new StreamSource(new ByteArrayInputStream(xslData));
		Templates templates = styleSheetProvider.getTransformerFactory().newTemplates(xslSource);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StreamResult streamResult = new StreamResult(out);
		LOGGER.info("Using XML-source {}", xmlSource);
		javax.xml.transform.Transformer transformer = templates.newTransformer();
		String indent = System.getProperty(OutputKeys.INDENT);
		if (!(NO.equals(indent) || YES.equals(indent))) {
			indent = NO;
		}
		transformer.setOutputProperty(OutputKeys.INDENT, indent);
		LOGGER.info("indent: {}", indent);
		transformer.transform(new StreamSource(new FileInputStream(xmlSource)), streamResult);
		String html = new String(out.toByteArray());
		URL url = templateFolder.toURI().toURL();
		html = html.replaceAll("/template/", url.toExternalForm());

		LOGGER.info("Writing result to {}", outputFile);
		html = html.replaceAll("charset=\"", "charset=UTF-8\"");
		PrintWriter printWriter = new PrintWriter(outputFile);
		printWriter.write(html);
		printWriter.close();
	}
}
