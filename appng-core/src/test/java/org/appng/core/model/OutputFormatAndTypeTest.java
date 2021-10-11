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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBException;

import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.Scope;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.core.service.TemplateService;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.OutputFormat;
import org.appng.xml.platform.OutputType;
import org.appng.xml.platform.Platform;
import org.appng.xml.platform.PlatformConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

public class OutputFormatAndTypeTest {

	@Mock
	private Path path;

	@Mock
	private Properties properties;

	private PlatformProcessor processor;

	private MarshallService marshallService;

	private String tplOne = "src/test/resources/template/tpl-one";

	@Before
	public void setup() throws InvalidConfigurationException, JAXBException {
		MockitoAnnotations.initMocks(this);
		marshallService = MarshallService.getMarshallService();
		processor = new PlatformProcessor();
		MockServletContext servletContext = new MockServletContext();
		Map<String, Object> platformScope = new ConcurrentHashMap<>();
		platformScope.put(org.appng.api.Platform.Environment.PLATFORM_CONFIG, properties);
		platformScope.put(org.appng.api.Platform.Environment.SITES, new HashMap<>());
		Mockito.when(properties.getString(org.appng.api.Platform.Property.VHOST_MODE))
				.thenReturn(VHostMode.NAME_BASED.name());
		servletContext.setAttribute(Scope.PLATFORM.name(), platformScope);
		processor.setPlatformTransformer(new PlatformTransformer());
		processor.setTemplatePath(PlatformTransformerTest.TEMPLATE_PATH);
		processor.init(new MockHttpServletRequest(servletContext), new MockHttpServletResponse(), null,
				PlatformTransformerTest.TEMPLATE_PATH);
	}

	@Test
	public void testDefaultFormatAndType() throws InvalidConfigurationException, JAXBException {
		processor.getPlatform(marshallService, path);
		validateHtmlWebgui();
	}

	@Test
	public void testFormatAndDefaultType() throws InvalidConfigurationException {
		Mockito.when(path.hasOutputFormat()).thenReturn(true);
		Mockito.when(path.hasOutputType()).thenReturn(false);
		Mockito.when(path.getOutputFormat()).thenReturn("xml");
		processor.getPlatform(marshallService, path);
		validateXmlRaw();
	}

	@Test
	public void testFormatAndType() throws InvalidConfigurationException {
		Mockito.when(path.hasOutputFormat()).thenReturn(true);
		Mockito.when(path.hasOutputType()).thenReturn(true);
		Mockito.when(path.getOutputFormat()).thenReturn("xml");
		Mockito.when(path.getOutputType()).thenReturn("raw");
		processor.getPlatform(marshallService, path);
		validateXmlRaw();
	}

	@Test
	public void testNoDefault() throws InvalidConfigurationException {
		processor.setTemplatePath(tplOne);
		processor.getPlatform(marshallService, path);
		validateHtmlWebgui();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoDefaultNoFormat() throws InvalidConfigurationException, JAXBException {
		File masterXML = new File(tplOne, TemplateService.PLATFORM_XML);
		Platform master = marshallService.unmarshall(masterXML, Platform.class);
		PlatformConfig config = master.getConfig();
		config.getOutputFormat().get(0).getOutputType().clear();
		processor.determineFormatAndType(config, path);
	}

	@Test
	public void testDefaultNoType() throws InvalidConfigurationException, JAXBException {
		Mockito.when(path.hasOutputFormat()).thenReturn(true);
		Mockito.when(path.getOutputFormat()).thenReturn("notype");
		File masterXML = new File(tplOne, TemplateService.PLATFORM_XML);
		Platform master = marshallService.unmarshall(masterXML, Platform.class);
		PlatformConfig config = master.getConfig();
		OutputFormat defaultFormat = config.getOutputFormat().get(0);
		defaultFormat.setDefault(true);
		config.getOutputFormat().get(1).getOutputType().clear();
		processor.determineFormatAndType(config, path);
		validateHtmlWebgui();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoFormat() throws InvalidConfigurationException, JAXBException {
		File masterXML = new File(tplOne, TemplateService.PLATFORM_XML);
		Platform master = marshallService.unmarshall(masterXML, Platform.class);
		PlatformConfig config = master.getConfig();
		config.getOutputFormat().clear();
		processor.determineFormatAndType(config, path);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoType() throws InvalidConfigurationException, JAXBException {
		Mockito.when(path.hasOutputFormat()).thenReturn(true);
		Mockito.when(path.getOutputFormat()).thenReturn("notype");
		File masterXML = new File(tplOne, TemplateService.PLATFORM_XML);
		Platform master = marshallService.unmarshall(masterXML, Platform.class);
		PlatformConfig config = master.getConfig();
		config.getOutputFormat().get(1).getOutputType().clear();
		processor.determineFormatAndType(config, path);
	}

	private void validateHtmlWebgui() {
		OutputFormat outputFormat = processor.getOutputFormat();
		OutputType outputType = processor.getOutputType();
		Assert.assertEquals("html", outputFormat.getId());
		Assert.assertEquals("webgui", outputType.getId());
	}

	private void validateXmlRaw() {
		OutputFormat outputFormat = processor.getOutputFormat();
		OutputType outputType = processor.getOutputType();
		Assert.assertEquals("xml", outputFormat.getId());
		Assert.assertEquals("raw", outputType.getId());
	}
}
