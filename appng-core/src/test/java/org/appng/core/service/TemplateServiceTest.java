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
package org.appng.core.service;

import java.io.File;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.appng.core.controller.TestSupport;
import org.appng.core.domain.PackageArchiveImpl;
import org.appng.core.model.ZipFileProcessor;
import org.appng.xml.application.Template;
import org.appng.xml.application.TemplateType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:platformContext.xml", initializers = TemplateServiceTest.class)
@DirtiesContext
public class TemplateServiceTest extends TestSupport
		implements ApplicationContextInitializer<GenericApplicationContext> {

	private File templateZip = new File("target/test-classes/template/appng-template-0.8.0.zip");

	@Autowired
	private TemplateService service;

	@Test
	public void testGetTemplate() throws Exception {
		Template template = service.getTemplate("target/test-classes/template/appng");
		Assert.assertEquals(TemplateType.XSL, template.getType());
		Assert.assertEquals("appng-template", template.getName());
		Assert.assertEquals("0.8.0", template.getVersion());
		Assert.assertEquals("appNG Template", template.getDisplayName());
		Assert.assertEquals("0.13.0", template.getAppngVersion());
	}

	@Test
	public void testTemplateExtractor() throws Exception {
		ZipFileProcessor<org.appng.core.domain.Template> templateExtractor = service.getTemplateExtractor();
		org.appng.core.domain.Template processed = templateExtractor.process(new ZipFile(templateZip));
		verifyTemplate(processed);
	}

	@Test
	public void testInstall() throws Exception {
		org.appng.core.domain.Template installed = service.installTemplate(new PackageArchiveImpl(templateZip, true));
		verifyTemplate(installed);
		Assert.assertNotNull(installed.getVersion());
	}

	protected void verifyTemplate(org.appng.core.domain.Template processed) {
		Assert.assertEquals(TemplateType.XSL, processed.getType());
		Assert.assertEquals("appng-template", processed.getName());
		Assert.assertEquals("0.8.0", processed.getPackageVersion());
		Assert.assertEquals("appNG Template", processed.getDisplayName());
		Assert.assertEquals("0.13.0", processed.getAppNGVersion());
		Assert.assertEquals(6, processed.getResources().size());
	}

	@Override
	public void initialize(GenericApplicationContext applicationContext) {
		new TestInitializer() {

			@Override
			protected java.util.Properties getProperties() {
				java.util.Properties properties = super.getProperties();
				properties.put("entityPackage", "org.appng.testapplication");
				properties.put("hsqlPort", "9010");
				properties.put("hsqlPath", "file:target/hsql/" + InitializerServiceTest.class.getSimpleName());
				properties.put("repositoryBase", "org.appng.testapplication");
				return properties;
			}
		}.initialize(applicationContext);
	}

}
