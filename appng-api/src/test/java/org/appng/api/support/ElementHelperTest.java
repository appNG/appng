/*
 * Copyright 2011-2020 the original author or authors.
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

import java.io.Closeable;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.Environment;
import org.appng.api.Options;
import org.appng.api.ParameterSupport;
import org.appng.api.Path;
import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ApplicationConfig;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.BeanOption;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkmode;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.OptionGroup;
import org.appng.xml.platform.PageConfig;
import org.appng.xml.platform.PanelLocation;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Permission;
import org.appng.xml.platform.Permissions;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.Template;
import org.appng.xml.platform.ValidationGroups;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ElementHelperTest {

	private static final String DATASOURCE_TEST = "datasource 'test'";

	@Mock
	private ApplicationRequest applicationRequest;

	@Mock
	private ApplicationConfigProvider configProvider;

	@Mock
	private ApplicationConfigProvider pcp;

	@Mock
	private PermissionProcessor permissionProcessor;

	@Mock
	private Site site;

	@Mock
	private Application application;

	@Mock
	private Properties properties;

	@Mock
	private Path path;

	private MetaData metaData;

	private ElementHelper elementHelper;

	private ParameterSupport parameterSupport;

	private ApplicationRootConfig rootCfg;

	@Before
	public void setup() throws JAXBException, ProcessingException {
		MockitoAnnotations.initMocks(this);
		Datasource ds = new Datasource();
		DataConfig config = new DataConfig();
		ds.setConfig(config);
		metaData = new MetaData();
		config.setMetaData(metaData);

		Mockito.when(site.getProperties()).thenReturn(properties);
		Mockito.when(site.getName()).thenReturn("localhost");
		Mockito.when(site.getSiteClassLoader()).thenReturn(new URLClassLoader(new URL[0], getClass().getClassLoader()));
		Mockito.when(application.getName()).thenReturn("application");
		Mockito.when(properties.getString(SiteProperties.SERVICE_PATH)).thenReturn("/services");
		Mockito.when(properties.getString(SiteProperties.MANAGER_PATH)).thenReturn("/manager");
		Mockito.when(applicationRequest.getApplicationConfig()).thenReturn(configProvider);
		Mockito.when(applicationRequest.getLocale()).thenReturn(Locale.getDefault());

		Mockito.doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Object arg0 = invocation.getArguments()[0];
				if (arg0 instanceof Label) {
					Label label = (Label) arg0;
					if (null == label.getValue()) {
						label.setValue(label.getId());
					}
				}
				return null;
			}
		}).when(applicationRequest).setLabel(Mockito.any(Label.class));

		rootCfg = new ApplicationRootConfig();
		Mockito.when(configProvider.getApplicationRootConfig()).thenReturn(rootCfg);

		Mockito.when(pcp.getDatasource("dsId")).thenReturn(ds);
		Mockito.when(applicationRequest.getPermissionProcessor()).thenReturn(permissionProcessor);
		parameterSupport = new DollarParameterSupport(new HashMap<>());
		Mockito.when(applicationRequest.getParameterSupportDollar()).thenReturn(parameterSupport);
		elementHelper = new ElementHelper(site, application);
		elementHelper.initializeParameters(DATASOURCE_TEST, applicationRequest, parameterSupport, new Params(),
				new Params());

		XMLUnit.setIgnoreWhitespace(true);
	}

	@Test
	public void testNoData() {
		MetaData metaData2 = elementHelper.getFilteredMetaData(applicationRequest, metaData, false);
		Assert.assertTrue(metaData2.getFields().isEmpty());
		metaData2 = elementHelper.getFilteredMetaData(applicationRequest, metaData, true);
		Assert.assertTrue(metaData2.getFields().isEmpty());
	}

	@Test
	public void testInitNavigation() {
		Linkpanel linkpanel = new Linkpanel();
		linkpanel.setId("panel");
		linkpanel.setLocation(PanelLocation.TOP);
		addLink(linkpanel, "link1", "target", "${1 eq 1}");
		addLink(linkpanel, "link2", "target", "${1 eq 2}");
		rootCfg.setNavigation(linkpanel);
		PageConfig pageConfig = new PageConfig();
		pageConfig.setLinkpanel(new Linkpanel());
		Mockito.when(permissionProcessor.hasPermissions(Mockito.any(PermissionOwner.class))).thenReturn(true);
		elementHelper.initNavigation(applicationRequest, path, pageConfig);
		XmlValidator.validate(pageConfig.getLinkpanel());
	}

	@Test
	public void testInitNavigationNoPermission() {
		Linkpanel linkpanel = new Linkpanel();
		Permissions permissions = new Permissions();
		Permission p1 = new Permission();
		p1.setRef("foo");
		permissions.getPermissionList().add(p1);
		linkpanel.setPermissions(permissions);
		linkpanel.setId("panel");
		linkpanel.setLocation(PanelLocation.TOP);
		addLink(linkpanel, "link1", "target", "${1 eq 1}");
		addLink(linkpanel, "link2", "target", "${1 eq 2}");
		rootCfg.setNavigation(linkpanel);
		PageConfig pageConfig = new PageConfig();
		Linkpanel pageLinks = new Linkpanel();
		pageLinks.setPermissions(new Permissions());
		Link page = new Link();
		page.setMode(Linkmode.INTERN);
		page.setLabel(new Label());
		pageLinks.getLinks().add(page);
		pageConfig.setLinkpanel(pageLinks);
		Mockito.when(permissionProcessor.hasPermissions(Mockito.any(PermissionOwner.class))).thenReturn(true, false);
		elementHelper.initNavigation(applicationRequest, path, pageConfig);
		Assert.assertNull(pageConfig.getLinkpanel());
	}

	@Test
	public void testLinkPanel() {
		DataConfig config = new DataConfig();
		Linkpanel linkpanel1 = new Linkpanel();
		linkpanel1.setId("linkpanel1");
		linkpanel1.setLocation(PanelLocation.INLINE);

		Link link1 = addLink(linkpanel1, "link1", "foo", "${1 eq 1}");
		Label confirmation = new Label();
		confirmation.setId("some.label");
		confirmation.setParams("#{name}");
		link1.setConfirmation(confirmation);
		addLink(linkpanel1, "link2", "bar", "${1 eq 2}");
		addLink(linkpanel1, "link3", "bar", null);

		Link link4 = addLink(linkpanel1, "link4", "wslink", null);
		link4.setMode(Linkmode.WEBSERVICE);

		Link disabledLink = addLink(linkpanel1, "link5", "disabled", "${3 eq 4}");
		disabledLink.setShowDisabled(true);

		Link withConditionAndPermission = addLink(linkpanel1, "withCurrentCondition", "foobar", "${current.id=5}");
		withConditionAndPermission.setShowDisabled(true);
		withConditionAndPermission.setPermissions(new Permissions());
		Permission permission = new Permission();
		withConditionAndPermission.getPermissions().getPermissionList().add(permission);
		permission.setRef("link:withCurrentCondition");

		config.getLinkpanel().add(linkpanel1);
		Linkpanel linkpanel2 = new Linkpanel();
		linkpanel2.setId("linkpanel2");
		config.getLinkpanel().add(linkpanel2);

		Mockito.when(path.isPathSelected("/ws/localhost/applicationfoo")).thenReturn(true);

		Mockito.when(permissionProcessor.hasPermissions(Mockito.any(PermissionOwner.class)))
				.thenAnswer(new Answer<Boolean>() {
					public Boolean answer(InvocationOnMock invocation) throws Throwable {
						PermissionOwner owner = (PermissionOwner) invocation.getArguments()[0];
						String name = owner.getName();
						if ("linkpanel:linkpanel2".equals(name) || "link:link3".equals(name)
								|| "link:withCurrentCondition".equals(name)) {
							return false;
						}
						return true;
					}
				});
		Mockito.when(path.getGuiPath()).thenReturn("/ws");
		Mockito.when(path.getServicePath()).thenReturn("/services");
		Mockito.when(path.getOutputPrefix()).thenReturn(StringUtils.EMPTY);
		XmlValidator.validate(config, "-before");
		elementHelper.initLinkpanel(applicationRequest, path, config, parameterSupport);
		XmlValidator.validate(config);
	}

	private Link addLink(Linkpanel linkpanel, String labelText, String target, String condition) {
		Link link = new Link();
		Label label = new Label();
		label.setValue(labelText);
		link.setLabel(label);
		link.setMode(Linkmode.EXTERN);
		link.setTarget(target);
		linkpanel.getLinks().add(link);
		if (null != condition) {
			Condition c = new Condition();
			c.setExpression(condition);
			link.setCondition(c);
		}
		return link;
	}

	@Test
	public void testRead() {
		FieldDef dateField = addField(metaData, "dateField");
		dateField.setType(FieldType.DATE);
		dateField.setFormat("${i18n.message('dateFormat')}");
		Mockito.when(applicationRequest.getMessage("dateFormat")).thenReturn("yyyy-MM-dd");

		FieldDef field = addField(metaData, "readableField");
		Label tooltip = new Label();
		tooltip.setId("tooltip");
		field.setTooltip(tooltip);
		FieldDef fieldNoPermission = addField(metaData, "fieldNoRead");
		FieldDef conditionFalse = addField(metaData, "conditionFalse", "${1 eq 2}");
		FieldDef conditionTrue = addField(metaData, "conditionTrue", "${1 eq 1}");
		FieldDef conditionCurrent = addField(metaData, "conditionCurrent", "${current.id gt 5}");

		Mockito.when(permissionProcessor.hasReadPermission(dateField)).thenReturn(true);
		Mockito.when(permissionProcessor.hasReadPermission(field)).thenReturn(true);
		Mockito.when(permissionProcessor.hasReadPermission(conditionFalse)).thenReturn(true);
		Mockito.when(permissionProcessor.hasReadPermission(conditionTrue)).thenReturn(true);
		Mockito.when(permissionProcessor.hasReadPermission(conditionCurrent)).thenReturn(true);
		Mockito.when(permissionProcessor.hasReadPermission(fieldNoPermission)).thenReturn(false);

		MetaData readableFields = elementHelper.getFilteredMetaData(applicationRequest, metaData, false);
		XmlValidator.validate(readableFields);
	}

	@Test
	public void testWrite() {

		FieldDef field = addField(metaData, "writeableField");
		FieldDef fieldNoPermission = addField(metaData, "fieldNoWrite");

		Mockito.when(permissionProcessor.hasWritePermission(field)).thenReturn(true);
		Mockito.when(permissionProcessor.hasWritePermission(fieldNoPermission)).thenReturn(false);

		Action action = new Action();
		DatasourceRef dsRef = new DatasourceRef();
		dsRef.setId("dsId");
		action.setDatasource(dsRef);
		Mockito.when(pcp.getAction("eventId", "actionId")).thenReturn(action);

		MetaData writeableFields = elementHelper.getFilteredMetaData(applicationRequest, metaData, true);
		XmlValidator.validate(writeableFields);
	}

	@Test
	public void testGetOptions() {
		List<BeanOption> beanOptions = getOptions();
		Options options = elementHelper.getOptions(beanOptions);
		BeanOption option = beanOptions.get(0);
		Assert.assertEquals("foobar", option.getOtherAttributes().get(new QName("id")));
		Assert.assertEquals("${foo}", option.getOtherAttributes().get(new QName("id2")));
		Assert.assertEquals("foobar", options.getOptionValue("action", "id"));
		Assert.assertEquals("${foo}", options.getOptionValue("action", "id2"));
	}

	@Test
	public void testInitOptions() throws ProcessingException {
		Params referenceParams = new Params();
		addParam(referenceParams, "foo", null, null);

		Params executionParams = new Params();
		addParam(executionParams, "foo", "foobar", null);
		elementHelper.initializeParameters(DATASOURCE_TEST, applicationRequest, parameterSupport, referenceParams,
				executionParams);

		List<BeanOption> beanOptions = getOptions();
		elementHelper.initOptions(beanOptions);
		BeanOption option = beanOptions.get(0);
		Assert.assertEquals("foobar", option.getOtherAttributes().get(new QName("id")));
		Assert.assertEquals("foobar", option.getOtherAttributes().get(new QName("id2")));
	}

	@Test
	public void testConditionMatches() {
		Assert.assertTrue(elementHelper.conditionMatches(null));
		Condition condition = new Condition();
		Assert.assertTrue(elementHelper.conditionMatches(condition));
		condition.setExpression("${1<2}");
		Assert.assertTrue(elementHelper.conditionMatches(condition));
		condition.setExpression("${1>2}");
		Assert.assertFalse(elementHelper.conditionMatches(condition));
	}

	@Test
	public void testAddMessages() {
		Messages messages = new Messages();
		Message firstMessage = new Message();
		messages.getMessageList().add(firstMessage);
		Environment env = Mockito.mock(Environment.class);
		Messages sessionMessages = new Messages();
		Message sessionMessage = new Message();
		sessionMessages.getMessageList().add(sessionMessage);
		Mockito.when(env.getAttribute(Scope.SESSION, Session.Environment.MESSAGES)).thenReturn(sessionMessages);
		Messages addMessages = ElementHelper.addMessages(env, messages);
		Assert.assertEquals(sessionMessages, addMessages);
		Assert.assertTrue(addMessages.getMessageList().contains(firstMessage));
		Assert.assertTrue(addMessages.getMessageList().contains(sessionMessage));
		Mockito.verify(env).setAttribute(Scope.SESSION, Session.Environment.MESSAGES, sessionMessages);
	}

	private List<BeanOption> getOptions() {
		List<BeanOption> beanOptions = new ArrayList<>();
		BeanOption option = new BeanOption();
		option.setName("action");
		option.getOtherAttributes().put(new QName("id"), "foobar");
		option.getOtherAttributes().put(new QName("id2"), "${foo}");
		beanOptions.add(option);
		return beanOptions;
	}

	private FieldDef addField(MetaData metaData, String name) {
		return addField(metaData, name, null);
	}

	private FieldDef addField(MetaData metaData, String name, String condition) {
		FieldDef field = new FieldDef();
		field.setName(name);
		Label label = new Label();
		label.setValue(name);
		field.setLabel(label);
		metaData.getFields().add(field);
		if (null != condition) {
			Condition c = new Condition();
			c.setExpression(condition);
			field.setCondition(c);
		}
		return field;
	}

	@Test
	public void testSetSelectionTitles() {
		Data data = new Data();
		Selection selection1 = new Selection();
		Selection selection2 = new Selection();
		data.getSelections().add(selection1);
		Label l2 = new Label();
		Label l1 = new Label();
		Label l3 = new Label();
		l1.setId("id1");
		l3.setId("id3");
		l2.setId("id2");
		selection1.setTitle(l1);
		selection2.setTitle(l2);
		OptionGroup optionGroup = new OptionGroup();
		optionGroup.setLabel(l3);
		selection2.getOptionGroups().add(optionGroup);
		SelectionGroup selectionGroup = new SelectionGroup();
		selectionGroup.getSelections().add(selection2);
		data.getSelectionGroups().add(selectionGroup);
		elementHelper.setSelectionTitles(data, applicationRequest);
		Assert.assertEquals("id1", l1.getValue());
		Assert.assertEquals("id2", l2.getValue());
		Assert.assertEquals("id3", l3.getValue());
	}

	@Test
	public void testAddTemplates() {
		Config config = new Config();
		Template t1 = new Template();
		t1.setOutputType("html");
		t1.setPath("t1.xsl");
		Template t2 = new Template();
		t2.setOutputType("html");
		t2.setPath("t2.xsl");
		config.getTemplates().add(t1);
		config.getTemplates().add(t2);
		rootCfg.setConfig(new ApplicationConfig());
		elementHelper.addTemplates(configProvider, config);
		Assert.assertEquals(config.getTemplates(), rootCfg.getConfig().getTemplates());
	}

	@Test
	public void testDefaultParameters() throws ProcessingException {

		Params referenceParams = new Params();
		addParam(referenceParams, "p1", null, null);
		addParam(referenceParams, "p2", null, null);
		addParam(referenceParams, "p3", null, "bar");
		addParam(referenceParams, "p4", null, "bar");
		addParam(referenceParams, "p5", "foo", null);
		addParam(referenceParams, "p6", "foo", null);
		addParam(referenceParams, "p7", "foo", "bar");
		addParam(referenceParams, "p8", "foo", "bar");
		addParam(referenceParams, "p9", "foo", "bar");

		Params executionParams = new Params();
		addParam(executionParams, "p1", null, null);
		addParam(executionParams, "p2", "jin", null);
		addParam(executionParams, "p3", null, null);
		addParam(executionParams, "p4", "jin", null);
		addParam(executionParams, "p5", null, null);
		addParam(executionParams, "p6", "jin", null);
		addParam(executionParams, "p7", null, null);
		addParam(executionParams, "p8", "jin", null);
		addParam(executionParams, "p9", null, "fizz");

		DollarParameterSupport parameterSupport = new DollarParameterSupport(new HashMap<>());
		Map<String, String> actual = elementHelper.initializeParameters(DATASOURCE_TEST, applicationRequest,
				parameterSupport, referenceParams, executionParams);

		Assert.assertNull(actual.get("p1"));
		Assert.assertEquals("jin", actual.get("p2"));
		Assert.assertEquals("bar", actual.get("p3"));
		Assert.assertEquals("bar", actual.get("p4"));
		Assert.assertEquals("foo", actual.get("p5"));
		Assert.assertEquals("foo", actual.get("p6"));
		Assert.assertEquals("bar", actual.get("p7"));
		Assert.assertEquals("bar", actual.get("p8"));
		Assert.assertEquals("fizz", actual.get("p9"));
	}

	@Test
	@Ignore("APPNG-442")
	public void testOverlappingParams() {
		Map<String, List<String>> postParameters = new HashMap<>();
		postParameters.put("p5", Arrays.asList("a"));

		Mockito.when(applicationRequest.getParametersList()).thenReturn(postParameters);
		Mockito.when(applicationRequest.isPost()).thenReturn(true);

		Params referenceParams = new Params();
		addParam(referenceParams, "p5", null, "foo");

		Params executionParams = new Params();
		addParam(executionParams, "p5", null, "b");

		DollarParameterSupport parameterSupport = new DollarParameterSupport(new HashMap<>());
		try {
			elementHelper.initializeParameters(DATASOURCE_TEST, applicationRequest, parameterSupport, referenceParams,
					executionParams);
			Assert.fail("should throw ProcessingException");
		} catch (ProcessingException e) {
			Assert.assertEquals(
					"the parameter 'p5' is ambiguous, since it's a execution parameter for datasource 'test' (value: 'b') and also"
							+ " POST-parameter (value: 'a'). Avoid such overlapping parameters!",
					e.getMessage());
		}
	}

	@Test
	public void testInitializeParameters() throws ProcessingException {
		Map<String, List<String>> postParameters = new HashMap<>();
		postParameters.put("postParam1", Arrays.asList("a"));
		postParameters.put("postParam2", Arrays.asList("b"));
		postParameters.put("postParam3", Arrays.asList("x", "y", "z"));
		Mockito.when(applicationRequest.getParametersList()).thenReturn(postParameters);
		Mockito.when(applicationRequest.isPost()).thenReturn(true);
		Params referenceParams = new Params();
		addParam(referenceParams, "p0", null, null);
		addParam(referenceParams, "p1", null, "${req_1}");
		addParam(referenceParams, "p2", "2", "${req_2}");
		addParam(referenceParams, "p3", "3", "7");
		addParam(referenceParams, "p4", "6", null);

		Params executionParams = new Params();
		addParam(executionParams, "p0", "7", null);
		addParam(executionParams, "p1", null, null);
		addParam(executionParams, "p2", "4", null);
		addParam(executionParams, "p3", "9", "18");
		addParam(executionParams, "p4", null, null);

		Map<String, String> parameters = new HashMap<>();
		parameters.put("req_1", "42");

		DollarParameterSupport parameterSupport = new DollarParameterSupport(parameters);
		Map<String, String> result = elementHelper.initializeParameters(DATASOURCE_TEST, applicationRequest,
				parameterSupport, referenceParams, executionParams);
		Assert.assertEquals("7", result.get("p0"));
		Assert.assertEquals("42", result.get("p1"));
		Assert.assertEquals("2", result.get("p2"));
		Assert.assertEquals("18", result.get("p3"));
		Assert.assertEquals("6", result.get("p4"));
		Assert.assertEquals("a", result.get("postParam1"));
		Assert.assertEquals("b", result.get("postParam2"));
		Assert.assertEquals("x|y|z", result.get("postParam3"));

	}

	@Test
	public void testGetOutputPrefix() {
		Environment env = Mockito.mock(Environment.class);
		Mockito.when(env.removeAttribute(Scope.REQUEST, EnvironmentKeys.EXPLICIT_FORMAT)).thenReturn(true);
		Path pathMock = Mockito.mock(Path.class);
		Mockito.when(pathMock.getGuiPath()).thenReturn("/manager");
		Mockito.when(pathMock.getOutputPrefix()).thenReturn("/_html/_nonav");
		Mockito.when(pathMock.getSiteName()).thenReturn("site");
		Mockito.when(env.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO)).thenReturn(pathMock);
		String outputPrefix = elementHelper.getOutputPrefix(env);
		Assert.assertEquals("/manager/_html/_nonav/site/", outputPrefix);
	}

	@Test
	public void testGetValidationGroups() {
		ValidationGroups groups = new ValidationGroups();

		ValidationGroups.Group groupA = new ValidationGroups.Group();
		groupA.setClazz(Serializable.class.getName());
		groups.getGroups().add(groupA);

		ValidationGroups.Group groupB = new ValidationGroups.Group();
		groupB.setClazz(Closeable.class.getName());
		String condition = "${current eq 'foo'}";
		groupB.setCondition(condition);
		groups.getGroups().add(groupB);

		metaData.setValidation(groups);

		Class<?>[] validationGroups = elementHelper.getValidationGroups(metaData, "foo");
		Assert.assertArrayEquals(new Class[] { Serializable.class, Closeable.class }, validationGroups);
		Assert.assertEquals(condition, groupB.getCondition());
	}

	private void addParam(Params params, String name, String defaultVal, String value) {
		Param param = new Param();
		param.setName(name);
		param.setValue(value);
		param.setDefault(defaultVal);
		params.getParam().add(param);
	}

}
