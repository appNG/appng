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
package org.appng.taglib;

import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.appng.api.DataContainer;
import org.appng.api.Environment;
import org.appng.api.GlobalTaglet;
import org.appng.api.GlobalXMLTaglet;
import org.appng.api.PageProcessor;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.Taglet;
import org.appng.api.XMLTaglet;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.MetaData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class TagletProcessorTest {

	@Mock
	private Site site;

	@Mock
	private Application application;

	@Mock
	private Request request;

	@Mock
	private Environment environment;

	@Mock
	private GlobalTaglet globalTaglet;

	@Mock
	private Taglet taglet;

	@Mock
	private XMLTaglet xmlTaglet;

	@Mock
	private GlobalXMLTaglet globalXMLTaglet;

	private TagletProcessor tagletProcessor = new TagletProcessor();

	private Map<String, String> tagletAttributes = new HashMap<String, String>();

	private StringWriter writer = new StringWriter();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(request.getEnvironment()).thenReturn(environment);
		Mockito.when(site.getSiteClassLoader()).thenReturn(new URLClassLoader(new URL[0]));
		Mockito.when(environment.getLocale()).thenReturn(Locale.ENGLISH);
	}

	@Test
	public void testTaglet() throws Exception {
		Mockito.when(taglet.processTaglet(site, application, request, tagletAttributes)).thenReturn("a taglet");
		Mockito.when(application.getBean("taglet", Taglet.class)).thenReturn(taglet);
		boolean processPage = tagletProcessor.perform(site, site, application, tagletAttributes, request, "taglet", "",
				writer);
		Assert.assertTrue(processPage);
		Mockito.verify(application).getBean("taglet", Taglet.class);
		Mockito.verify(taglet).processTaglet(site, application, request, tagletAttributes);
		Assert.assertEquals("a taglet", writer.toString());
	}

	@Test
	public void testGlobalTaglet() throws Exception {
		Mockito.when(globalTaglet.processTaglet(site, site, application, request, tagletAttributes)).thenReturn(
				"a global taglet");
		Mockito.when(application.getBean("taglet", Taglet.class)).thenReturn(globalTaglet);
		boolean processPage = tagletProcessor.perform(site, site, application, tagletAttributes, request, "taglet", "",
				writer);
		Assert.assertTrue(processPage);
		Mockito.verify(application).getBean("taglet", Taglet.class);
		Mockito.verify(globalTaglet).processTaglet(site, site, application, request, tagletAttributes);
		Assert.assertEquals("a global taglet", writer.toString());
	}

	@Test
	public void testXmlTaglet() throws Exception {
		Mockito.when(xmlTaglet.processTaglet(site, application, request, tagletAttributes)).thenReturn(
				getDataContainer());
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.DO_XSL)).thenReturn(false);
		Mockito.when(application.getBean("taglet", XMLTaglet.class)).thenReturn(xmlTaglet);
		tagletProcessor.setMarshallService(MarshallService.getMarshallService());
		boolean processPage = tagletProcessor.perform(site, site, application, tagletAttributes, request, "taglet",
				"xml", writer);
		Assert.assertTrue(processPage);
		Mockito.verify(application).getBean("taglet", XMLTaglet.class);
		Mockito.verify(xmlTaglet).processTaglet(site, application, request, tagletAttributes);
		Assert.assertEquals("<platform xmlns=\"http://www.appng.org/schema/platform\">",
				writer.toString().split("\n")[2]);
	}

	@Test
	public void testXmlTagletPrefixSuffix() throws Exception {
		String prefix = "test prefix";
		String prefixKey = "noXslPrefix";
		String suffix = "test suffix";
		String suffixKey = "noXslSuffix";
		tagletAttributes.put(prefixKey, prefix);
		tagletAttributes.put(suffixKey, suffix);
		Mockito.when(xmlTaglet.processTaglet(site, application, request, tagletAttributes)).thenReturn(
				getDataContainer());
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.DO_XSL)).thenReturn(false);
		Mockito.when(application.getBean("taglet", XMLTaglet.class)).thenReturn(xmlTaglet);
		tagletProcessor.setMarshallService(MarshallService.getMarshallService());
		boolean processPage = tagletProcessor.perform(site, site, application, tagletAttributes, request, "taglet",
				"xml", writer);
		Assert.assertTrue(processPage);
		Mockito.verify(application).getBean("taglet", XMLTaglet.class);
		Mockito.verify(xmlTaglet).processTaglet(site, application, request, tagletAttributes);
		String xmlOutput = writer.toString();
		Assert.assertEquals(prefix, xmlOutput.split("\n")[0]);
		Assert.assertTrue(xmlOutput.endsWith(suffix));
		tagletAttributes.remove(prefixKey);
		tagletAttributes.remove(suffixKey);
	}

	@Test
	public void testXmlTagletPrefixOnly() throws Exception {
		String prefix = "test prefix";
		String prefixKey = "noXslPrefix";
		tagletAttributes.put(prefixKey, prefix);
		Mockito.when(xmlTaglet.processTaglet(site, application, request, tagletAttributes)).thenReturn(
				getDataContainer());
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.DO_XSL)).thenReturn(false);
		Mockito.when(application.getBean("taglet", XMLTaglet.class)).thenReturn(xmlTaglet);
		tagletProcessor.setMarshallService(MarshallService.getMarshallService());
		boolean processPage = tagletProcessor.perform(site, site, application, tagletAttributes, request, "taglet",
				"xml", writer);
		Assert.assertTrue(processPage);
		Mockito.verify(application).getBean("taglet", XMLTaglet.class);
		Mockito.verify(xmlTaglet).processTaglet(site, application, request, tagletAttributes);
		String xmlOutput = writer.toString();
		Assert.assertEquals("<!--", xmlOutput.split("\n")[0]);
		Assert.assertTrue(xmlOutput.endsWith("-->"));
		tagletAttributes.remove(prefixKey);
	}

	@Test
	public void testXmlTagletSuffixOnly() throws Exception {
		String suffix = "test suffix";
		String suffixKey = "noXslSuffix";
		tagletAttributes.put(suffixKey, suffix);
		Mockito.when(xmlTaglet.processTaglet(site, application, request, tagletAttributes)).thenReturn(
				getDataContainer());
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.DO_XSL)).thenReturn(false);
		Mockito.when(application.getBean("taglet", XMLTaglet.class)).thenReturn(xmlTaglet);
		tagletProcessor.setMarshallService(MarshallService.getMarshallService());
		boolean processPage = tagletProcessor.perform(site, site, application, tagletAttributes, request, "taglet",
				"xml", writer);
		Assert.assertTrue(processPage);
		Mockito.verify(application).getBean("taglet", XMLTaglet.class);
		Mockito.verify(xmlTaglet).processTaglet(site, application, request, tagletAttributes);
		String xmlOutput = writer.toString();
		Assert.assertEquals("<!--", xmlOutput.split("\n")[0]);
		Assert.assertTrue(xmlOutput.endsWith("-->"));
		tagletAttributes.remove(suffixKey);
	}

	@Test
	public void testGlobalXmlTaglet() throws Exception {
		Mockito.when(globalXMLTaglet.processTaglet(site, site, application, request, tagletAttributes)).thenReturn(
				getDataContainer());
		Mockito.when(environment.getAttribute(Scope.REQUEST, EnvironmentKeys.DO_XSL)).thenReturn(false);
		Mockito.when(application.getBean("taglet", XMLTaglet.class)).thenReturn(globalXMLTaglet);
		tagletProcessor.setMarshallService(MarshallService.getMarshallService());
		boolean processPage = tagletProcessor.perform(site, site, application, tagletAttributes, request, "taglet",
				"xml", writer);
		Assert.assertTrue(processPage);
		Mockito.verify(application).getBean("taglet", XMLTaglet.class);
		Mockito.verify(globalXMLTaglet).processTaglet(site, site, application, request, tagletAttributes);
		Assert.assertEquals("<platform xmlns=\"http://www.appng.org/schema/platform\">",
				writer.toString().split("\n")[2]);
	}

	@Test
	public void testPageProcessor() throws Exception {
		Taglet myTaglet = Mockito.spy(new MyTaglet());
		Mockito.when(application.getBean("taglet", Taglet.class)).thenReturn(myTaglet);
		boolean processPage = tagletProcessor.perform(site, site, application, tagletAttributes, request, "taglet", "",
				writer);
		Assert.assertFalse(processPage);
		Mockito.verify(application).getBean("taglet", Taglet.class);
		Mockito.verify(myTaglet).processTaglet(site, application, request, tagletAttributes);
		Assert.assertEquals("MyTaglet", writer.toString());
	}

	protected DataContainer getDataContainer() {
		MetaData metaData = new MetaData();
		metaData.setBindClass(MyTaglet.class.getName());
		FieldProcessorImpl fp = new FieldProcessorImpl("foo", metaData);
		DataContainer dataContainer = new DataContainer(fp);
		dataContainer.setItem(new MyTaglet());
		return dataContainer;
	}

	class MyTaglet implements Taglet, PageProcessor {

		public boolean processPage() {
			return false;
		}

		public String processTaglet(Site site, Application application, Request request,
				Map<String, String> tagletAttributes) {
			return "MyTaglet";
		}

	}
}
