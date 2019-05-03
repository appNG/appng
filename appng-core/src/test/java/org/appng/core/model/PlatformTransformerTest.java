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
package org.appng.core.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Path;
import org.appng.api.model.Properties;
import org.appng.core.controller.HttpHeaders;
import org.appng.core.service.PlatformTestConfig;
import org.appng.core.model.PlatformTransformer.PlatformTransformerException;
import org.appng.core.service.TestInitializer;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.OutputFormat;
import org.appng.xml.platform.OutputType;
import org.appng.xml.platform.Platform;
import org.appng.xml.platform.Template;
import org.appng.xml.transformation.StyleSheetProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = PlatformTestConfig.class, initializers = TestInitializer.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class PlatformTransformerTest {

	public static final String TEMPLATE_PATH = "src/test/resources/template/appng";

	@Autowired
	private PlatformTransformer platformTransformer;

	@Autowired
	private MarshallService marshallService;

	private ApplicationProvider applicationProvider;

	@Mock
	private Properties platformProperties;

	@Mock
	private Path path;

	@Mock
	private Environment environment;

	private String platformXML;
	private Platform platform;

	static final File DEBUG_FOLDER = new File("target/debug");

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		Mockito.when(platformProperties.getBoolean(org.appng.api.Platform.Property.DEV_MODE)).thenReturn(Boolean.FALSE);
		setFormatAndType(platformTransformer, true);
	}

	protected void setFormatAndType(PlatformTransformer platformTransformer, boolean addUtils) {
		OutputFormat outputFormat = new OutputFormat();
		outputFormat.setId("html");
		platformTransformer.setOutputFormat(outputFormat);
		OutputType outputType = new OutputType();
		outputType.setId("webgui");
		Template master = new Template();
		master.setType("master");
		master.setDeleteIncludes(true);
		master.setPath("platform.xsl");
		outputType.getTemplates().add(master);
		if (addUtils) {
			Template utils = new Template();
			utils.setPath("utils.xsl");
			outputType.getTemplates().add(utils);
		}
		platformTransformer.setOutputType(outputType);
	}

	protected void init(PlatformTransformer platformTransformer, String templatePath)
			throws InvalidConfigurationException, ParserConfigurationException, JAXBException, TransformerException {
		platformTransformer.setTemplatePath(templatePath);
		platformTransformer.setEnvironment(environment);
		platform = platformTransformer.getPlatform(marshallService, path);
		platformXML = marshallService.marshal(platform);
	}

	@Test
	public void test() throws Exception {
		init(platformTransformer, TEMPLATE_PATH);
		transform();
	}

	@Test
	public void testCompileError() throws Exception {
		runErrornousTest("src/test/resources/template/error-compile", PlatformTransformerException.class);
	}

	@Test
	public void testRuntimeError() throws Exception {
		runErrornousTest("src/test/resources/template/error-runtime", PlatformTransformerException.class);
	}

	private void runErrornousTest(String template, Class<? extends TransformerException> exceptionType)
			throws Exception {
		PlatformTransformer.clearCache();
		PlatformTransformer errorTransformer = new PlatformTransformer() {
			@Override
			protected String getDebugFilePrefix(Date now) {
				return StringUtils.EMPTY;
			}
		};
		setFormatAndType(errorTransformer, false);
		StyleSheetProvider styleSheetProvider = platformTransformer.getStyleSheetProvider();
		styleSheetProvider.cleanup();
		errorTransformer.setStyleSheetProvider(styleSheetProvider);
		init(errorTransformer, template);
		Mockito.when(platformProperties.getString(org.appng.api.Platform.Property.PLATFORM_ROOT_PATH))
				.thenReturn(DEBUG_FOLDER.getParent());
		Mockito.when(platformProperties.getBoolean(org.appng.api.Platform.Property.WRITE_DEBUG_FILES)).thenReturn(true);
		try {
			errorTransformer.transform(applicationProvider, platformProperties, platformXML, HttpHeaders.CHARSET_UTF8,
					DEBUG_FOLDER);
			Assert.fail("TransformerException should be thrown");
		} catch (TransformerException e) {
			Assert.assertEquals(exceptionType, e.getClass());
		}
		assertFolderContains(DEBUG_FOLDER, AbstractRequestProcessor.PLATFORM_XML,
				AbstractRequestProcessor.STACKTRACE_TXT, PlatformTransformer.TEMPLATE_XSL);
	}

	static void assertFolderContains(File folder, String... files) {
		List<String> fileList = Arrays.asList(folder.list());
		for (String file : files) {
			Assert.assertTrue(file + " does not exist in folder " + folder.getAbsolutePath() + "!",
					fileList.contains(file));
		}
	}

	@Test
	public void testDevMode() throws Exception {
		init(platformTransformer, TEMPLATE_PATH);
		transform();
	}

	private void transform() throws IOException, TransformerConfigurationException, InvalidConfigurationException,
			JAXBException, ParserConfigurationException, TransformerException {
		String transform = platformTransformer.transform(applicationProvider, platformProperties, platformXML,
				HttpHeaders.CHARSET_UTF8, DEBUG_FOLDER);
		Platform transformedplatform = marshallService.unmarshall(transform, Platform.class);
		Assert.assertEquals(platformXML, marshallService.marshal(transformedplatform));
	}

}
