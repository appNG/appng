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
package org.appng.core.model;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.appng.xml.MarshallService;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.PageReference;
import org.appng.xml.platform.Platform;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Section;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.ValidationRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ThymeleafProcessorTest {

	private static ThymeleafProcessor.AppNG appNG;
	private static Platform platform;

	@BeforeClass
	public static void setup() throws Exception {
		MarshallService ms = MarshallService.getMarshallService();
		ClassLoader classLoader = ThymeleafProcessorTest.class.getClassLoader();
		InputStream is = classLoader.getResourceAsStream("xml/ThymeleafProcessorTest-platform.xml");
		platform = ms.unmarshall(is, org.appng.xml.platform.Platform.class);
		appNG = new ThymeleafProcessor.AppNG(platform, null);
	}

	@Test
	public void testSectionTitle() {
		PageReference page = appNG.page("sites");
		List<Section> sections = page.getStructure().getSection();
		Assert.assertEquals("Site properties", sections.get(1).getTitle().getValue());
	}

	@Test
	public void testAction() {
		Assert.assertTrue(appNG.hasAction("siteEvent", "update"));
		Action action = appNG.action("siteEvent", "update");
		Assert.assertEquals("update", action.getId());
		Assert.assertEquals("siteEvent", action.getEventId());
	}

	@Test
	public void testDatasource() {
		Assert.assertTrue(appNG.hasDatasource("site-properties"));
		Datasource datasource = appNG.datasource("site-properties");
		Assert.assertEquals("site-properties", datasource.getId());
	}

	@Test
	public void testFieldFromAction() {
		Action action = appNG.action("siteEvent", "update");
		FieldDef field = appNG.field(action, "name");
		Assert.assertEquals("name", field.getName());
		Assert.assertEquals("site.name", field.getBinding());

		FieldDef field2 = appNG.actionField(action.getEventId(), action.getId(), field.getName());
		Assert.assertEquals(field, field2);
	}

	@Test
	public void testFieldFromDataSource() {
		Datasource datasource = appNG.datasource("site-properties");
		FieldDef field = appNG.field(datasource, "shortName");
		Assert.assertEquals("shortName", field.getName());
		Assert.assertEquals("shortName", field.getBinding());

		FieldDef field2 = appNG.dataSourceField(datasource.getId(), field.getName());
		Assert.assertEquals(field, field2);

		Assert.assertTrue(appNG.hasSort(datasource.getConfig()));
	}

	@Test
	public void testDataFromResult() {
		Result result = appNG.result(appNG.datasource("site-properties"), 1);
		Assert.assertNotNull(result);
		Datafield data = appNG.data(result, "shortName");
		Assert.assertEquals("shortName", data.getName());
		Assert.assertEquals("assetsDir", data.getValue());
	}

	@Test
	public void testSelection() {
		Action action = appNG.action("siteEvent", "update");
		Selection selection = appNG.selection(action.getData(), "template");
		Assert.assertNotNull(selection);
		Assert.assertEquals("template", selection.getId());
	}

	@Test
	public void testPage() {
		PageReference page = appNG.page("sites");
		Assert.assertNotNull(page);
		Assert.assertEquals("sites", page.getId());
	}

	@Test
	public void testPages() {
		Datasource datasource = appNG.datasource("site-properties");
		List<Integer> pages = appNG.pages(datasource.getData().getResultset());
		Assert.assertEquals(Arrays.asList(0, 1, 2), pages);
	}

	@Test
	public void testSessionParam() {
		String param = appNG.sessionParam("selectedappid");
		Assert.assertEquals("", param);
	}

	@Test
	public void testGetParam() {
		String param = appNG.getParam("sites", "selectedappid");
		Assert.assertEquals("", param);
	}

	@Test
	public void testUrlParam() {
		String param = appNG.urlParam("sites", "action");
		Assert.assertEquals("update", param);
	}

	@Test
	public void testLinkPanel() {
		Result result = appNG.result(appNG.datasource("site-properties"), 1);
		Linkpanel linkpanel = appNG.linkpanel(result, "actions");
		Assert.assertNotNull(linkpanel);
		Assert.assertEquals("actions", linkpanel.getId());
	}

	@Test
	public void testDefaultLink() {
		Result result = appNG.result(appNG.datasource("site-properties"), 1);
		Linkpanel linkpanel = appNG.linkpanel(result, "actions");
		Link defaultLink = appNG.defaultLink(linkpanel);
		Assert.assertNotNull(defaultLink);
		Assert.assertEquals("actions[1]", defaultLink.getId());
	}

	@Test
	public void testActionParam() {
		Action action = appNG.action("siteEvent", "update");
		String param = appNG.param(action, "siteid");
		Assert.assertEquals("1", param);
	}

	@Test
	public void testDataSourceParam() {
		Datasource datasource = appNG.datasource("site-properties");
		String param = appNG.param(datasource, "siteid");
		Assert.assertEquals("1", param);
	}

	@Test
	public void testRules() {
		Action action = appNG.action("siteEvent", "update");
		FieldDef field = appNG.field(action, "host");
		List<ValidationRule> rules = appNG.rules(field);
		Assert.assertEquals(2, rules.size());
	}

	@Test
	public void testChildField() {
		Action action = appNG.action("siteEvent", "update");
		FieldDef location = appNG.field(action, "location");
		Assert.assertNotNull(location);
		FieldDef latitude = appNG.childField(location, "latitude");
		Assert.assertNotNull(latitude);

		Datafield locationData = appNG.data(action, location.getName());
		Assert.assertNotNull(locationData);

		Datafield latData = appNG.childData(locationData, latitude.getBinding());
		Assert.assertNotNull(latData);
		Assert.assertEquals("8.0000", latData.getValue());
	}

	@Test
	public void testLabel() {
		String label = appNG.label(appNG.page("sites").getConfig(), "foo");
		Assert.assertEquals("bar", label);
	}
}
