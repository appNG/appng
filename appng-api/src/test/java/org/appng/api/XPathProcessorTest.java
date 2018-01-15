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
package org.appng.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.appng.xml.MarshallService;
import org.appng.xml.MarshallService.AppNGSchema;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class XPathProcessorTest {

	static XPathProcessor processor;

	@BeforeClass
	public static void setup() throws IOException {
		ClassLoader classLoader = XPathProcessorTest.class.getClassLoader();
		processor = new XPathProcessor(classLoader.getResourceAsStream("xml/xpathtest.xml"));
	}

	@Test
	public void testNamespacePrefix() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		MarshallService marshallService = MarshallService.getMarshallService();
		InputStream resource = getClass().getClassLoader().getResourceAsStream("application/conf/datasource.xml");
		AppNGSchema.PLATFORM.getContext().createMarshaller().marshal(marshallService.unmarshall(resource), doc);
		XPathProcessor xpath = new XPathProcessor(doc);
		xpath.setNamespace("appng", AppNGSchema.PLATFORM.getNamespace());
		Assert.assertEquals("datasource", xpath.getString("//appng:datasource[2]/@id"));
		Assert.assertEquals("doesNotExist", xpath.getString("//appng:datasource[@id=\"datasource\"]//appng:meta-data/@bindClass"));
	}

	@Test
	public void testGetString() throws IOException {
		Assert.assertEquals("abcd", processor.getString("/root/a/string"));
		Assert.assertEquals("abcd", processor.getString(processor.getNode("/root/a"), "string"));
	}

	@Test
	public void testGetBoolean() throws IOException {
		Assert.assertEquals(Boolean.TRUE, processor.getBoolean("/root/a/boolean"));
		Assert.assertEquals(Boolean.TRUE, processor.getBoolean(processor.getNode("/root/a"), "boolean"));
	}

	@Test
	public void testGetIntNumber() throws IOException {
		Assert.assertEquals(1.0d, processor.getNumber("/root/a/integer"));
		Assert.assertEquals(1.0d, processor.getNumber(processor.getNode("/root/a"), "integer"));
	}

	@Test
	public void testGetNumber() throws IOException {
		Assert.assertEquals(3.45d, processor.getNumber("/root/a/double"));
		Assert.assertEquals(3.45d, processor.getNumber(processor.getNode("/root/a"), "double"));
	}

	@Test
	public void testGetNodeAndElement() throws IOException {
		Element root = processor.getElement("/root");
		Element el = processor.getElement("/root/a");
		Node node = processor.getNode("/root/a");
		Assert.assertEquals(el, node);
		Assert.assertEquals(el, processor.getElement(root, "./a"));
	}

	@Test
	public void testGetNodeList() throws IOException {
		NodeList nodes = processor.getNodes("/root/*");
		NodeList nodesFromRoot = processor.getNodes(processor.getNode("root"), "./*");
		Assert.assertEquals(processor.getNode("/root/a"), nodes.item(0));
		Assert.assertEquals(processor.getNode("/root/b"), nodes.item(1));
		Assert.assertEquals(nodesFromRoot.item(0), nodes.item(0));
		Assert.assertEquals(nodesFromRoot.item(1), nodes.item(1));
		Node rootNode = processor.getNode("root");
		Assert.assertEquals(processor.getNode(rootNode, "./a"), nodes.item(0));
		Assert.assertEquals(processor.getNode(rootNode, "b"), nodes.item(1));
	}

	@Test
	public void testGetXml() {
		Node node = processor.getNode("/root/b");
		String xml = processor.getXml(node);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		processor.getXml(node, out);
		Assert.assertEquals(xml, out.toString());
	}

	@Test
	public void testNewText() {
		Text text = processor.newText("data");
		Assert.assertEquals("data", text.getData());
	}

	@Test
	public void testNewCDATA() {
		Text text = processor.newCDATA("data");
		Assert.assertEquals("data", text.getData());
	}

	@Test
	public void testNewElement() {
		Element el = processor.newElement("data");
		Assert.assertEquals("data", el.getTagName());
	}

	@Test
	public void testNewAttribute() {
		Attr attr = processor.newAttribute("name", "value");
		Assert.assertEquals("name", attr.getName());
		Assert.assertEquals("value", attr.getValue());
	}

	@Test
	public void testModify() throws IOException {
		Element textEl = processor.newElement("text");
		textEl.appendChild(processor.newCDATA("data"));
		Element root = (Element) processor.getDocument().getElementsByTagName("root").item(0);
		root.setAttributeNode(processor.newAttribute("foo", "bar"));
		root.appendChild(textEl);
		ClassLoader classLoader = XPathProcessorTest.class.getClassLoader();
		InputStream resource = classLoader.getResourceAsStream("xml/xpathtest-modified.xml");
		Assert.assertEquals(processor.getXml(), new XPathProcessor(resource).getXml());
	}
}
