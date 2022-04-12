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
package org.appng.api.support.field;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Icon;
import org.appng.xml.platform.Icontype;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkable;
import org.appng.xml.platform.Linkmode;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.PanelLocation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LinkPanelFieldHandlerTest extends AbstractFieldConverterTest {

	private static final String LABEL_WITH_PARAM = "label.with.param";

	@Before
	public void setup() throws Exception {
		Map<String, Object> params = new HashMap<>();
		params.put("param1", 5);
		super.setup(FieldType.LINKPANEL, params);
		fieldWrapper.setLinkpanel(getLinkPanel());
		Mockito.when(messageSource.getMessage(LABEL_WITH_PARAM, new Object[] { "5", "7" }, LABEL_WITH_PARAM,
				environment.getLocale())).thenReturn("label-with-param-5-7");
		Mockito.when(messageSource.getMessage(LABEL_WITH_PARAM, new Object[] { "5", "foobar" }, LABEL_WITH_PARAM,
				environment.getLocale())).thenReturn("5 - foobar");
	}

	@Test
	public void testParamWithDot() {
		Linkpanel linkPanel = getLinkPanel();
		Linkable link = linkPanel.getLinks().get(0);
		link.getConfirmation().setParams("${param1},#{field.with.dot}");

		DatafieldOwner datafieldOwner = getDatafieldOwner();

		Datafield dotField = new Datafield();
		dotField.setName("field.with.dot");
		dotField.setType(FieldType.TEXT);
		dotField.setValue("foobar");
		datafieldOwner.getFields().add(dotField);

		fieldWrapper.setLinkpanel(linkPanel);
		fieldConverter.addField(datafieldOwner, fieldWrapper);

		List<Linkpanel> linkpanels = datafieldOwner.getLinkpanels();
		Label confirmation = linkpanels.get(0).getLinks().get(0).getConfirmation();
		Assert.assertEquals("5 - foobar", confirmation.getValue());
		Label label3 = linkpanels.get(0).getLinks().get(1).getLabel();
		Assert.assertEquals("label3", label3.getId());
	}

	private Linkpanel getLinkPanel() {
		Linkpanel linkpanel = new Linkpanel();
		linkpanel.setLocation(PanelLocation.INLINE);
		linkpanel.setId("links");

		Link link1 = new Link();
		link1.setLabel(new Label());
		link1.getLabel().setParams("${param1},#{id}");
		link1.getLabel().setId(LABEL_WITH_PARAM);
		link1.setTarget("/foo/${param1}/#{id}");
		link1.setMode(Linkmode.INTERN);
		Condition c1 = new Condition();
		c1.setExpression("${5 eq param1}");
		link1.setCondition(c1);
		Icon icon = new Icon();
		icon.setType(Icontype.CLASS);
		icon.setContent("edit");
		link1.setIcon(icon);
		link1.setDefault("${param1 > 3}");
		Label confirmation = new Label();
		confirmation.setId(LABEL_WITH_PARAM);
		confirmation.setParams("${param1},#{id}");
		link1.setConfirmation(confirmation);
		linkpanel.getLinks().add(link1);

		Link link2 = new Link();
		link2.setTarget("/bar");
		link2.setMode(Linkmode.INTERN);
		Condition c2 = new Condition();
		c2.setExpression("${4 eq param1}");
		link2.setCondition(c2);
		linkpanel.getLinks().add(link2);

		Link link3 = new Link();
		link3.setTarget("disabled");
		link3.setMode(Linkmode.INTERN);
		Condition c3 = new Condition();
		c3.setExpression("${3 eq 4}");
		link3.setShowDisabled(true);
		link3.setCondition(c3);
		link3.setLabel(new Label());
		link3.getLabel().setId("label3");
		linkpanel.getLinks().add(link3);
		return linkpanel;
	}

	@Override
	@Test
	public void testAddField() throws Exception {
		DatafieldOwner dataFieldOwner = getDatafieldOwner();

		Datafield datafield = new Datafield();
		datafield.setName("links");
		datafield.setType(FieldType.LINKPANEL);
		dataFieldOwner.getFields().add(datafield);

		Datafield idField = new Datafield();
		idField.setName("id");
		idField.setType(FieldType.INT);
		idField.setValue("7");
		dataFieldOwner.getFields().add(idField);

		fieldConverter.addField(dataFieldOwner, fieldWrapper);

		List<Linkpanel> linkpanels = dataFieldOwner.getLinkpanels();
		Assert.assertEquals(1, linkpanels.size());
		Linkpanel linkpanel = linkpanels.get(0);
		Linkpanel original = getLinkPanel();
		Assert.assertEquals(original.getId(), linkpanel.getId());
		Assert.assertEquals(2, linkpanel.getLinks().size());

		Linkable link1 = linkpanel.getLinks().get(0);
		Assert.assertEquals("/foo/5/7", link1.getTarget());
		Assert.assertEquals("label-with-param-5-7", link1.getConfirmation().getValue());
		Assert.assertEquals("true", link1.getDefault());
		Assert.assertEquals(LABEL_WITH_PARAM, link1.getConfirmation().getId());
		Assert.assertEquals(Icontype.CLASS, link1.getIcon().getType());
		Assert.assertEquals("edit", link1.getIcon().getContent());
		Assert.assertEquals("label-with-param-5-7", link1.getLabel().getValue());

		Linkable link3 = linkpanel.getLinks().get(1);
		Assert.assertEquals("", link3.getTarget());
		Assert.assertTrue(link3.isDisabled());
	}

	public Container<?> getContainer() {
		return new Container<Linkpanel>() {
		};
	}

	public void testSetObject() throws Exception {
		// nothing to do
	}

	public void testSetObjectEmptyValue() throws Exception {
		// nothing to do
	}

	public void testSetObjectInvalidValue() throws Exception {
		// nothing to do
	}

	public void testSetObjectNull() throws Exception {
		// nothing to do
	}

	public void testSetString() throws Exception {
		// nothing to do
	}

	public void testSetStringNullObject() throws Exception {
		// nothing to do
	}

	public void testSetStringInvalidType() throws Exception {
		// nothing to do
	}

}
