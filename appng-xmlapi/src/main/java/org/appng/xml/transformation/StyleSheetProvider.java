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
package org.appng.xml.transformation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@code StyleSheetProvider} assembles a XSL-stylesheet from one master XSL-file and a various number of other
 * XSL-files to include. The insertion-point for the additional files must be set via {@link #setInsertBefore(String)}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class StyleSheetProvider {

	private static final String XSL_INCLUDE = "xsl:include";
	private InputStream masterSource;
	private DocumentBuilderFactory documentBuilderFactory;
	private TransformerFactory transformerFactory;
	private DocumentBuilder documentBuilder;
	private Transformer transformer;
	private String insertBefore;
	private Map<String, InputStream> styleReferences = new TreeMap<>();
	private String name;
	private String templateRoot;

	public StyleSheetProvider() {
	}

	/**
	 * Initializes this {@code StyleSheetProvider} by setting the {@link DocumentBuilder} and {@link Transformer} to
	 * use.
	 */
	public void init() {
		try {
			documentBuilder = getDocumentBuilderFactory().newDocumentBuilder();
			transformer = getTransformerFactory().newTransformer();
		} catch (Exception e) {
			LOGGER.error(String.format("[%s] error setting up StyleSheetProvider, instance will not work!", name), e);
		}
	}

	/**
	 * Sets the master XSL source for this {@code StyleSheetProvider}.
	 * 
	 * @param masterXsl
	 *                     the master source
	 * @param templateRoot
	 *                     the absolute path to the directory where template resides
	 */
	public void setMasterSource(InputStream masterXsl, String templateRoot) {
		try {
			this.masterSource = masterXsl;
			this.templateRoot = templateRoot;
		} catch (Exception e) {
			LOGGER.error(String.format("[%s] error setting up StyleSheetProvider, instance will not work!", name), e);
		}
	}

	/**
	 * Adds an additional XSL-source to be included into the master source
	 * 
	 * @param styleSheet
	 *                   the additional source
	 * @param reference
	 *                   the name of the source, used for XSL-comments and logging
	 */
	public void addStyleSheet(InputStream styleSheet, String reference) {
		try {
			if (styleReferences.containsKey(reference)) {
				LOGGER.warn("[{}] stylesheet '{}' is already defined, contents will be overridden!", name, reference);
			}
			styleReferences.put(reference, styleSheet);
			LOGGER.trace("[{}] adding stylesheet with reference '{}'", name, reference);
		} catch (Exception e) {
			LOGGER.error(String.format("[%s] error parsing stylesheet '%s'", name, reference), e);
		}
	}

	/**
	 * @see #getStyleSheet(boolean, OutputStream)
	 */
	public byte[] getStyleSheet(boolean deleteIncludes) {
		return getStyleSheet(deleteIncludes, null);
	}

	/**
	 * Assembles the complete XSL stylesheet by including the sources added via
	 * {@link #addStyleSheet(InputStream, String)} and embedding them into the master source at the defined
	 * insertion-point.
	 * 
	 * @param deleteIncludes
	 *                       If set to {@code true}, all {@code <xsl:include>}s eventually contained in the master
	 *                       source will be removed before assembling the stylesheet. If set to {@code false}, the
	 *                       {@code <xsl:include>}s are being kept and processed.
	 * @param additionalOut
	 *                       an additional {@link OutputStream} to write the stylesheet to (optional)
	 * 
	 * @return the complete stylesheet as an array of bytes
	 */
	public byte[] getStyleSheet(boolean deleteIncludes, OutputStream additionalOut) {
		try {
			Document masterDoc = documentBuilder.parse(masterSource);
			Node rootNode = masterDoc.getFirstChild();
			Node insertionPoint = null;
			NodeList nodes = rootNode.getChildNodes();
			List<Node> includes = new ArrayList<>();
			int hits = nodes.getLength();
			for (int i = 0; i < hits; i++) {
				Node node = nodes.item(i);
				String nodeName = node.getNodeName();
				if (XSL_INCLUDE.equals(nodeName)) {
					includes.add(node);
				} else if (insertBefore.equals(nodeName)) {
					insertionPoint = node;
				}
			}

			for (Node node : includes) {
				Node href = node.getAttributes().getNamedItem("href");
				String reference = href.getTextContent();
				rootNode.removeChild(node);
				if (deleteIncludes) {
					LOGGER.trace("[{}] removing reference to '{}'", name, reference);
				} else {
					File file = new File(templateRoot, reference);
					styleReferences.put(reference, new FileInputStream(file));
				}
			}

			for (String reference : styleReferences.keySet()) {
				includeStyleSheet(rootNode, insertionPoint, reference);
			}

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				DOMSource domSource = new DOMSource(masterDoc);
				if (additionalOut != null) {
					transformer.transform(domSource, new StreamResult(additionalOut));
				}
				transformer.transform(domSource, new StreamResult(outputStream));
				LOGGER.debug("stylesheet complete");
				return outputStream.toByteArray();
			}
		} catch (Exception e) {
			LOGGER.error(String.format("[%s] error writing stylesheet", name), e);
		} finally {
			close(masterSource);
		}
		return null;
	}

	protected void close(InputStream is) {
		try {
			is.close();
		} catch (IOException e) {
			LOGGER.error("error closing stream", e);
		} finally {
			is = null;
		}
	}

	private void includeStyleSheet(Node rootNode, Node insertionPoint, String reference)
			throws SAXException, IOException {
		InputStream inputStream = styleReferences.get(reference);
		Document styleSheetDoc = getDocumentBuilder().parse(inputStream);
		if (null == styleSheetDoc) {
			LOGGER.warn("[{}] referenced stylesheet '{}' could not be found, inclusion skipped!", name, reference);
		} else {
			LOGGER.trace("[{}] including referenced stylesheet '{}'", name, reference);
			NodeList childNodes = styleSheetDoc.getFirstChild().getChildNodes();
			Document ownerDocument = rootNode.getOwnerDocument();
			Comment beginComment = ownerDocument.createComment("[BEGIN] embed '" + reference + "'");
			Comment endComment = ownerDocument.createComment("[END] embed '" + reference + "'");
			Node inserted = rootNode.insertBefore(endComment, insertionPoint);
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node sourceNode = childNodes.item(i);
				Node adoptedNode = ownerDocument.importNode(sourceNode, true);
				inserted = rootNode.insertBefore(adoptedNode, inserted);
			}
			inserted = rootNode.insertBefore(beginComment, inserted);
		}
	}

	public DocumentBuilderFactory getDocumentBuilderFactory() {
		return documentBuilderFactory;
	}

	public void setDocumentBuilderFactory(DocumentBuilderFactory documentBuilderFactory) {
		this.documentBuilderFactory = documentBuilderFactory;
	}

	public TransformerFactory getTransformerFactory() {
		return transformerFactory;
	}

	public void setTransformerFactory(TransformerFactory transformerFactory) {
		this.transformerFactory = transformerFactory;
	}

	public DocumentBuilder getDocumentBuilder() {
		return documentBuilder;
	}

	public void setDocumentBuilder(DocumentBuilder documentBuilder) {
		this.documentBuilder = documentBuilder;
	}

	public Transformer getTransformer() {
		return transformer;
	}

	public void setTransformer(Transformer transformer) {
		this.transformer = transformer;
	}

	/**
	 * Returns the actual insertion-point
	 * 
	 * @return the actual insertion-point
	 */
	public String getInsertBefore() {
		return insertBefore;
	}

	/**
	 * Sets the insertion point for the master source, at which additional stylesheets should be embedded.
	 * 
	 * @param insertBefore
	 *                     the insertion-point to use
	 */
	public void setInsertBefore(String insertBefore) {
		this.insertBefore = insertBefore;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns a unique ID for this {@code StyleSheetProvider}, which is build from the sorted set of reference names of
	 * the additional stylesheets added via {@link #addStyleSheet(InputStream, String)}.
	 * 
	 * @return the ID
	 * 
	 * @see #addStyleSheet(InputStream, String)
	 */
	public String getId() {
		return StringUtils.join(new TreeSet<>(styleReferences.keySet()), ",");
	}

	/**
	 * Closes all the previously used {@link InputStream}s.
	 * 
	 * @see #setMasterSource(InputStream, String)
	 * @see #addStyleSheet(InputStream, String)
	 */
	public void cleanup() {
		new ArrayList<>(styleReferences.keySet()).forEach(k -> close(styleReferences.remove(k)));
	}

	/**
	 * Checks if this {@code StyleSheetProvider} is valid, i.e. the master source is not {@code null}.
	 * 
	 * @return {@code true} the master source is not {@code null}, {@code false} otherwise
	 */
	public boolean isValid() {
		return null != masterSource;
	}

}
