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
package org.appng.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * An {@code XPathProcessor} is used the create a {@link Document} from an {@link URL} or {@link InputStream} and then
 * to extract {@link NodeList}s, {@link Node}s, {@link Element}s, {@link Attr}ibutes etc. from this {@link Document}.<br/>
 * It also allows to create new {@link Element}s, {@link Attr}ibutes, {@link CDATASection}s and {@link Text}s.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class XPathProcessor {

	private final Document document;
	private final XPath xpath;
	private Transformer transformer;

	/**
	 * Create a new {@code XPathProcessor} from the given URL.
	 * 
	 * @param url
	 *            the URL
	 * @throws IOException
	 *             if an error occurs while reading the XML-document from the URL
	 */
	public XPathProcessor(String url) throws IOException {
		this(new URL(url));
	}

	/**
	 * Create a new {@code XPathProcessor} from the given URL.
	 * 
	 * @param url
	 *            the URL
	 * @throws IOException
	 *             if an error occurs while reading the XML-document from the {@link URL}
	 */
	public XPathProcessor(URL url) throws IOException {
		this(url.openConnection().getInputStream());
	}

	/**
	 * Create a new {@code XPathProcessor} from the given {@link InputStream}.
	 * 
	 * @param is
	 *            the {@link InputStream}
	 * @throws IOException
	 *             if an error occurs while reading the XML-document from the {@link InputStream}
	 */
	public XPathProcessor(InputStream is) throws IOException {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			this.document = builder.parse(is);
			this.xpath = XPathFactory.newInstance().newXPath();
			this.transformer = TransformerFactory.newInstance().newTransformer();
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		} catch (TransformerConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new IOException(e);
		}
	}

	public XPathProcessor(Document document) throws IOException {
		this.document = document;
		try {
			this.xpath = XPathFactory.newInstance().newXPath();
			this.transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new IOException(e);
		}
	}

	/**
	 * Sets the namespace and the prefix to use for that namespace.<br/>
	 * Example:
	 * 
	 * <pre>
	 * XPathProcessor xpath = ...;
	 * xpath.setNamespace("appng", "http://www.appng.org/schema/platform");
	 * xpath.getNode("/appng:platform//appng:action[@id=\"foo\"]")
	 * </pre>
	 * 
	 * @param prefix
	 *            the prefix
	 * @param namespace
	 *            the namespace
	 */
	public void setNamespace(String prefix, String namespace) {
		xpath.setNamespaceContext(new NamespaceContext() {

			public Iterator<String> getPrefixes(String namespaceURI) {
				return Arrays.asList(prefix).iterator();
			}

			public String getPrefix(String namespaceURI) {
				return prefix;
			}

			public String getNamespaceURI(String prefix) {
				return namespace;
			}
		});
	}
	
	/**
	 * Returns the XML-fragment represented by the given {@link Node}.
	 * 
	 * @param node
	 *            the {@link Node}
	 * @return the XML-fragment
	 */
	public String getXml(Node node) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		getXml(node, outputStream);
		return new String(outputStream.toByteArray());
	}

	/**
	 * Writes XML-fragment represented by the given {@link Node} into the given {@link OutputStream}.
	 * 
	 * @param node
	 *            the {@link Node}
	 * @param outputStream
	 *            the {@link OutputStream}
	 */
	public void getXml(Node node, OutputStream outputStream) {
		try {
			transformer.transform(new DOMSource(node), new StreamResult(outputStream));
		} catch (TransformerException e) {
			LOGGER.error("error during transformation", e);
		}
	}

	/**
	 * Returns the XML for the {@link Document} parsed from the {@link URL}/{@link InputStream}.
	 * 
	 * @return the XML-{@link String}
	 */
	public String getXml() {
		return getXml(document);
	}

	/**
	 * Writes the XML for the {@link Document} parsed from the {@link URL}/{@link InputStream} into the given
	 * {@link OutputStream}.
	 * 
	 * @param outputStream
	 *            the {@link OutputStream}.
	 */
	public void getXml(OutputStream outputStream) {
		getXml(document, outputStream);
	}

	/**
	 * Returns the XML-fragment represented by the given {@link NodeList}.
	 * 
	 * @param nodes
	 *            the {@link NodeList}
	 * @return the XML-fragment
	 */
	public String getXml(NodeList nodes) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			getXml(node, outputStream);
		}
		return new String(outputStream.toByteArray());
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link String}.
	 * 
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link String} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public String getString(String xpathExpression) {
		return (String) evaluate(document, xpathExpression, XPathConstants.STRING);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link String}.
	 * 
	 * @param node
	 *            the {@link Node} to apply the expression to
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link String} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public String getString(Node node, String xpathExpression) {
		return (String) evaluate(node, xpathExpression, XPathConstants.STRING);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link Boolean}.
	 * 
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link Boolean} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public Boolean getBoolean(String xpathExpression) {
		return (Boolean) evaluate(document, xpathExpression, XPathConstants.BOOLEAN);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link Boolean}.
	 * 
	 * @param node
	 *            the {@link Node} to apply the expression to
	 * 
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link Boolean} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public Boolean getBoolean(Node node, String xpathExpression) {
		return (Boolean) evaluate(node, xpathExpression, XPathConstants.BOOLEAN);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link Number}.
	 * 
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link Number} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public Number getNumber(String xpathExpression) {
		return (Number) evaluate(document, xpathExpression, XPathConstants.NUMBER);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link Number}.
	 * 
	 * @param node
	 *            the {@link Node} to apply the expression to
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link Number} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public Number getNumber(Node node, String xpathExpression) {
		return (Number) evaluate(node, xpathExpression, XPathConstants.NUMBER);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link Node}.
	 * 
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link Node} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if the
	 *         expression could not be parsed.
	 * 
	 */
	public Node getNode(String xpathExpression) {
		return (Node) evaluate(document, xpathExpression, XPathConstants.NODE);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link Node}.
	 * 
	 * @param node
	 *            the {@link Node} to apply the expression to
	 * 
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link Node} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if the
	 *         expression could not be parsed.
	 * 
	 */
	public Node getNode(Node node, String xpathExpression) {
		return (Node) evaluate(node, xpathExpression, XPathConstants.NODE);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve an {@link Element}.
	 * 
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link Element} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public Element getElement(String xpathExpression) {
		return (Element) evaluate(document, xpathExpression, XPathConstants.NODE);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve an {@link Element}.
	 * 
	 * @param node
	 *            the {@link Node} to apply the expression to
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link Element} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public Element getElement(Node node, String xpathExpression) {
		return (Element) evaluate(node, xpathExpression, XPathConstants.NODE);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link NodeList}.
	 * 
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link NodeList} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public NodeList getNodes(String xpathExpression) {
		return (NodeList) evaluate(document, xpathExpression, XPathConstants.NODESET);
	}

	/**
	 * Parses the given {@code xpathExpression} to retrieve a {@link NodeList}.
	 * 
	 * @param node
	 *            the {@link Node} to apply the expression to
	 * @param xpathExpression
	 *            the xpath-expression
	 * 
	 * @return the {@link NodeList} retrieved from the {@code xpathExpression} (may be {@code null}), or {@code null} if
	 *         the expression could not be parsed.
	 * 
	 */
	public NodeList getNodes(Node node, String xpathExpression) {
		return (NodeList) evaluate(node, xpathExpression, XPathConstants.NODESET);
	}

	/**
	 * Creates a new {@link Attr}ibute.
	 * 
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 * @return the new {@link Attr}ibute.
	 */
	public Attr newAttribute(String name, String value) {
		Attr attribute = document.createAttribute(name);
		attribute.setNodeValue(value);
		return attribute;
	}

	/**
	 * Creates a new {@link Element}.
	 * 
	 * @param tagName
	 *            the tag-name for the element
	 * @return the new {@link Element}
	 */
	public Element newElement(String tagName) {
		Element element = document.createElement(tagName);
		return element;
	}

	/**
	 * Creates a new {@link CDATASection}.
	 * 
	 * @param tagName
	 *            the tag-name for the CDATA-section
	 * @return the new {@link CDATASection}
	 */
	public CDATASection newCDATA(String tagName) {
		CDATASection cdata = document.createCDATASection(tagName);
		return cdata;
	}

	/**
	 * Creates a new {@link Text}.
	 * 
	 * @param tagName
	 *            the tag-name for the text
	 * @return the new {@link Text}
	 */
	public Text newText(String tagName) {
		Text text = document.createTextNode(tagName);
		return text;
	}

	/**
	 * Adds a new {@link Attr}ibute to the given {@link Node}.
	 * 
	 * @param node
	 *            the Node to add the {@link Attr}ibute to
	 * @param name
	 *            the name of the {@link Attr}ibute to add
	 * @param value
	 *            the value of the {@link Attr}ibute to add
	 * 
	 * @return the {@code node} with the added {@link Attr}ibute
	 * 
	 * @see #newAttribute(String, String)
	 */
	public Node addAttribute(Node node, String name, String value) {
		Attr attribute = newAttribute(name, value);
		node.getAttributes().setNamedItem(attribute);
		return node;
	}

	/**
	 * Returns the {@link Document} parsed from the {@link URL}/{@link InputStream}.
	 * 
	 * @return the {@link Document}
	 */
	public Document getDocument() {
		return document;
	}

	private Object evaluate(Node node, String xpathExpression, QName returnType) {
		try {
			return this.xpath.evaluate(xpathExpression, node, returnType);
		} catch (XPathExpressionException e) { // ignore
		}
		return null;
	}
}
