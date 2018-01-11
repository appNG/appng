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
package org.appng.api.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.model.Application;
import org.appng.api.model.Resources;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.BeanOption;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Permission;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * TODO insert description
 * 
 * @author Claus Stümke, aiticon GmbH, 2016
 *
 */
public class DatasourceInheritanceTest {

	@Mock
	private Application application;

	@Test
	public void testInheritance() throws URISyntaxException, InvalidConfigurationException, JAXBException, IOException {
		MockitoAnnotations.initMocks(this);
		Mockito.when(application.isFileBased()).thenReturn(true);

		File targetFolder = new File("target/temp");
		ClassLoader classLoader = getClass().getClassLoader();
		File applicationFolder = new File(classLoader.getResource("application").toURI());

		Resources applicationResources = new ApplicationResourceHolder(application,
				MarshallService.getApplicationMarshallService(), applicationFolder, targetFolder);
		MarshallService marshallService = MarshallService.getMarshallService();
		ApplicationConfigProviderImpl applicationConfigProvider = new ApplicationConfigProviderImpl(marshallService,
				"testInheritance", applicationResources, false);

		Datasource datasource = applicationConfigProvider.getDatasource("datasource");
		Assert.assertNotNull(datasource);

		Datasource clone = applicationConfigProvider.getDatasource("cloneDatasource");
		Assert.assertNotNull(clone);

		Datasource cloneClone = applicationConfigProvider.getDatasource("cloneCloneDatasource");
		Assert.assertNotNull(cloneClone);

		Datasource cloneCloneClone = applicationConfigProvider.getDatasource("cloneCloneCloneDatasource");
		Assert.assertNotNull(cloneCloneClone);

		Datasource wrongDoubleClone = applicationConfigProvider.getDatasource("wrongDoubleClone");
		Assert.assertNull(wrongDoubleClone);

		Datasource wrongClone = applicationConfigProvider.getDatasource("wrongClone");
		Assert.assertNull(wrongClone);

		Datasource enhancedClone = applicationConfigProvider.getDatasource("enhancedClone");
		Assert.assertNotNull(enhancedClone);

		String marshalledEnhancedClone = marshallService.marshallNonRoot(enhancedClone);

		// change to true if expected result needs to be updated
		if (false) {
			marshallService.marshallNonRoot(enhancedClone,
					new FileOutputStream("src/test/resources/marshalledEnhancedClone.xml"));
		}

		String expected = IOUtils.toString(new FileInputStream("src/test/resources/marshalledEnhancedClone.xml"));
		Assert.assertEquals(expected, marshalledEnhancedClone);

		Datasource overrideBeanClone = applicationConfigProvider.getDatasource("overrideBeanClone");
		Assert.assertEquals("hansolo", overrideBeanClone.getBean().getId());
		List<BeanOption> options = overrideBeanClone.getBean().getOptions();
		Assert.assertEquals(1, options.size());
		Assert.assertEquals("lucas", options.get(0).getName());
		Assert.assertEquals(1, options.get(0).getOtherAttributes().size());
		Assert.assertEquals("[harrison]", options.get(0).getOtherAttributes().keySet().toString());
		Assert.assertEquals("[han]", options.get(0).getOtherAttributes().values().toString());

	}

	private BeanOption getBeanOption(List<BeanOption> options, String name) {
		for (BeanOption bo : options) {
			if (bo.getName().equals(name)) {
				return bo;
			}
		}
		return null;
	}

	private Permission getPermission(List<Permission> permissionList, String ref) {
		for (Permission p : permissionList) {
			if (p.getRef().equals(ref)) {
				return p;
			}
		}
		return null;
	}

	private Param getParam(List<Param> params, String name) {
		for (Param p : params) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	private FieldDef getField(List<FieldDef> inheritedFields, String name) {
		for (FieldDef f : inheritedFields) {
			if (f.getName().equals(name)) {
				return f;
			}
		}
		return null;
	}

	private Linkpanel getLinkpanel(List<Linkpanel> inheritedLinkPanels, String id) {
		for (Linkpanel lp : inheritedLinkPanels) {
			if (lp.getId().equals(id)) {
				return lp;
			}
		}
		return null;
	}
}
