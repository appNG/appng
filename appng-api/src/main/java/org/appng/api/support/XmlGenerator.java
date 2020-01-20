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

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ActionProvider;
import org.appng.api.DataProvider;
import org.appng.api.model.Identifiable;
import org.appng.api.model.Versionable;
import org.appng.tools.locator.Coordinate;
import org.appng.xml.BuilderFactory;
import org.appng.xml.MarshallService;
import org.appng.xml.MarshallService.AppNGSchema;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.ApplicationConfig;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.BeanOption;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Datasources;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.GetParams;
import org.appng.xml.platform.Icon;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkmode;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.PageConfig;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.Pages;
import org.appng.xml.platform.PanelLocation;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Permission;
import org.appng.xml.platform.PermissionMode;
import org.appng.xml.platform.Permissions;
import org.appng.xml.platform.PostParams;
import org.appng.xml.platform.SectionDef;
import org.appng.xml.platform.SectionelementDef;
import org.appng.xml.platform.Session;
import org.appng.xml.platform.SessionParams;
import org.appng.xml.platform.StructureDef;
import org.appng.xml.platform.UrlParams;
import org.appng.xml.platform.UrlSchema;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Utility class for generating source XML (for {@link Action}s and {@link Datasource}) from a given list of
 * {@link Entity}s. The generated XML will contain a basic master-detail navigation which can be used as a starting
 * point to implement CRUD functionality. Anyhow, no Java-sourcecode ( {@link ActionProvider}s or {@link DataProvider}s
 * ) will be generated. This can be done for example by using custom freemarker templating.
 * <p>
 * Example usage:
 * 
 * <pre>
 * XmlGenerator generator = new XmlGenerator();
 * generator.setDatePattern(&quot;yyyy-MM-dd&quot;);
 * Entity entity = new Entity(Person.class, new String[] { &quot;name&quot;, &quot;firstName&quot;, &quot;street&quot;, &quot;birthDate&quot; });
 * generator.generate(&quot;target/generated&quot;, &quot;My generated person app&quot;, &quot;messages-persons&quot;, entity);
 * </pre>
 * 
 * See also {@link #generate(String, String, String, Entity...)}
 * </p>
 * 
 * @author Matthias Müller
 *
 */
public class XmlGenerator {

	private static final String UTF_8 = "UTF-8";
	private static final String ACTION_PARAM = "${action}";
	private static final String ACTION = "action";
	private static final String ACTIONS = "actions";
	private static final String CONFIRM = "confirm";
	private static final String CREATE = "create";
	private static final String CURRENT_ID = "${current.id}";
	private static final String DELETE = "delete";
	private static final String EDIT = "edit";
	private static final String EVENT = "Event";
	private static final String FORM_ACTION = "form_action";
	private static final String FORM_ACTION_PARAM = "${form_action}";
	private static final String ID = "id";
	private static final String ID_PARAM = "${id}";
	private static final String LIST = "list";
	private static final String NAME = "name";
	private static final String SLASH = "/";
	private static final String TRUE = "true";
	private static final String UPDATE = "update";

	private MarshallService marshallService;
	private boolean addPermissions;
	private SortedMap<String, String> dictionary = new TreeMap<>();
	private SortedMap<String, String> permissionNames = new TreeMap<>();
	private String datePattern = "yyyy-MM-dd HH:mm:ss";

	/**
	 * Represents a domain object of a certain type with a number of properties
	 */
	public static class Entity {
		private final Class<?> type;
		private final String[] properties;

		/**
		 * Creates a new entity
		 * 
		 * @param type
		 *            the type of the entity to create
		 * @param properties
		 *            the properties to map when generating XML
		 */
		public Entity(Class<?> type, String[] properties) {
			this.type = type;
			this.properties = properties;
		}

		public String getName() {
			return type.getName();
		}

		public String getSimpleName() {
			return type.getSimpleName();
		}

		public String getSimpleNameLower() {
			return StringUtils.uncapitalize(getSimpleName());
		}

		public String getPlural() {
			String simpleName = getSimpleName();
			if (simpleName.endsWith("y")) {
				return simpleName.substring(0, simpleName.length() - 1) + "ies";
			}
			return simpleName + "s";
		}

		public String getPluralLower() {
			return StringUtils.uncapitalize(getPlural());
		}

		public Class<?> getType() {
			return type;
		}

		public String[] getProperties() {
			return properties;
		}

		public String getIdType() {
			return new BeanWrapperImpl(type).getPropertyType("id").getSimpleName();
		}
	}

	/**
	 * Generates the source XML for the given {@link Entity}s. The following files will be created for each
	 * {@link Entity}, assuming the {@code Person.class} as the entities type:
	 * <ul>
	 * <li>conf/datasource/ds-persons.xml</li>
	 * <li>conf/action/ev-persons.xml</li>
	 * <li>conf/page/pg-persons.xml</li>
	 * </ul>
	 * The following files are created once:
	 * <ul>
	 * <li>conf/config.xml - contains the {@link ApplicationRootConfig}</li>
	 * <li>dictionary/&lt;dictionaryName>.properties</li>
	 * <li>application.xml</li>
	 * </ul>
	 * 
	 * @param outPath
	 *            the target folder for the files to be generated
	 * @param name
	 *            the name for the {@link ApplicationRootConfig}-file to be created
	 * @param dictionaryName
	 *            the name for the dictionary file to be created (&lt;dictionaryName>.properties)
	 * @param entities
	 *            the list of {@link Entity}s to generate the source XML for
	 * @throws Exception
	 *             if something goes really wrong ;-)
	 */
	public void generate(String outPath, String name, String dictionaryName, Entity... entities) throws Exception {
		ApplicationRootConfig rootConfig = generateRootConfig(name);
		File conf = new File(outPath, "conf");
		for (Entity entity : entities) {
			String nameTpl = entity.getPluralLower();
			File dsFile = new File(conf, "datasource/ds-" + nameTpl + ".xml");
			dsFile.getParentFile().mkdirs();
			generateDatasources(entity, new FileOutputStream(dsFile), entity.getProperties());
			File actionFile = new File(conf, "action/ev-" + nameTpl + ".xml");
			actionFile.getParentFile().mkdirs();
			generateActions(new org.appng.xml.application.Permissions(), entity, new FileOutputStream(actionFile));
			File pageFile = new File(conf, "page/pg-" + nameTpl + ".xml");
			pageFile.getParentFile().mkdirs();
			generatePages(entity, rootConfig.getNavigation(), new FileOutputStream(pageFile));
		}
		File cfgFile = new File(conf, "config.xml");
		cfgFile.getParentFile().mkdirs();
		writeToOutputStream(rootConfig, new FileOutputStream(cfgFile));
		File dictFile = new File(outPath, "dictionary/" + dictionaryName + ".properties");
		dictFile.getParentFile().mkdirs();
		writeDictionary(new FileOutputStream(dictFile));
		writeApplicationXml(new FileOutputStream(new File(outPath, "application.xml")));
	}

	/**
	 * Creates a new XmlGenerator
	 * 
	 * @param addPermissions
	 *            if {@link Permissions} should be generated and used when referencing {@link Action}s
	 * @throws JAXBException
	 *             if creating a {@link MarshallService} fails
	 * @throws TransformerConfigurationException
	 *             if an error occurs while configuring the {@link TransformerFactory}
	 */
	public XmlGenerator(boolean addPermissions) throws JAXBException, TransformerConfigurationException {
		this.addPermissions = addPermissions;
		this.marshallService = new MarshallService();
		marshallService.setSchema(AppNGSchema.PLATFORM);
		marshallService.setPrettyPrint(true);
		marshallService.setSchemaLocation("http://www.appng.org/schema/platform/appng-platform.xsd");
		marshallService.setUseSchema(true);
		marshallService.setCdataElements(new ArrayList<>());
		marshallService.setDocumentBuilderFactory(BuilderFactory.documentBuilderFactory());
		marshallService.setTransformerFactory(BuilderFactory.transformerFactory());
		marshallService.init();
	}

	public XmlGenerator() throws JAXBException, TransformerConfigurationException {
		this(true);
	}

	public Pages generatePages(Entity entity, Linkpanel navigation, OutputStream out) throws Exception {
		PageDefinition page = new PageDefinition();
		String typeName = entity.getType().getSimpleName();
		String singular = StringUtils.uncapitalize(typeName);
		String plural = entity.getPluralLower();
		page.setId(plural);

		PageConfig config = new PageConfig();
		config.setTitle(getLabel(plural));
		UrlSchema urlSchema = new UrlSchema();
		UrlParams urlParams = new UrlParams();
		urlParams.getParamList().add(getParam(ACTION, null));
		urlParams.getParamList().add(getParam(ID, null));
		urlSchema.setUrlParams(urlParams);
		PostParams postParams = new PostParams();
		postParams.getParamList().add(getParam(FORM_ACTION, null));
		urlSchema.setPostParams(postParams);
		GetParams getParams = new GetParams();
		getParams.getParamList().add(getParam(ACTION, null));
		urlSchema.setGetParams(getParams);
		config.setUrlSchema(urlSchema);
		page.setConfig(config);

		String eventId = singular + EVENT;
		StructureDef structure = new StructureDef();
		SectionDef section = new SectionDef();
		section.setTitle(getLabel(plural));
		SectionelementDef create = new SectionelementDef();
		String onSuccess = SLASH + plural;
		ActionRef createRef = geActionRef(eventId, CREATE + typeName, onSuccess, "${empty id and 'create' eq action}",
				getParam(FORM_ACTION, FORM_ACTION_PARAM));
		create.setAction(createRef);
		section.getElement().add(create);

		SectionelementDef update = new SectionelementDef();
		ActionRef updateRef = geActionRef(eventId, UPDATE + typeName, onSuccess,
				"${not empty id and 'update' eq action}", getParam(FORM_ACTION, FORM_ACTION_PARAM),
				getParam(ID, ID_PARAM));
		update.setAction(updateRef);
		section.getElement().add(update);

		SectionelementDef list = new SectionelementDef();
		DatasourceRef listRef = new DatasourceRef();
		listRef.setId(plural);
		list.setDatasource(listRef);
		listRef.setParams(getParams(getParam("selectedId", ID_PARAM)));
		section.getElement().add(list);

		structure.getSection().add(section);

		SectionDef hiddenSection = new SectionDef();
		hiddenSection.setHidden(TRUE);
		SectionelementDef hiddenElement = new SectionelementDef();
		ActionRef deleteRef = geActionRef(eventId, DELETE + typeName, onSuccess, null, getParam(ACTION, ACTION_PARAM),
				getParam(ID, ID_PARAM));
		hiddenElement.setAction(deleteRef);
		hiddenSection.getElement().add(hiddenElement);
		structure.getSection().add(hiddenSection);

		page.setStructure(structure);

		Pages pages = new Pages();
		pages.getPageList().add(page);
		if (null != out) {
			marshallService.marshallNonRoot(pages, out);
		}

		Link pageLink = new Link();
		pageLink.setTarget("/" + page.getId());
		pageLink.setMode(Linkmode.INTERN);
		pageLink.setLabel(getLabel(page.getId()));
		navigation.getLinks().add(pageLink);

		return pages;
	}

	private ActionRef geActionRef(String eventId, String id, String onSuccess, String condition, Param... params) {
		ActionRef actionRef = new ActionRef();
		actionRef.setEventId(eventId);
		actionRef.setId(id);
		actionRef.setOnSuccess(onSuccess);
		if (params.length > 0) {
			actionRef.setParams(new Params());
			for (Param param : params) {
				actionRef.getParams().getParam().add(param);
			}
		}

		if (null != condition) {
			Condition cond = new Condition();
			cond.setExpression(condition);
			actionRef.setCondition(cond);
		}
		return actionRef;
	}

	private Param getParam(String name, String value) {
		Param param = new Param();
		param.setName(name);
		param.setValue(value);
		return param;
	}

	private Params getParams(Param... parameters) {
		Params params = new Params();
		for (Param param : parameters) {
			params.getParam().add(param);
		}
		return params;
	}

	public Event generateActions(org.appng.xml.application.Permissions permissions, Entity entity, OutputStream out)
			throws Exception {
		Class<?> bindClass = entity.getType();
		String entityName = bindClass.getSimpleName();
		String singular = StringUtils.uncapitalize(entityName);
		String plural = entity.getPluralLower();
		Event event = new Event();
		event.setId(singular + EVENT);
		event.setConfig(new Config());

		Action create = getCreateAction(bindClass, entityName, singular, plural);
		event.getActions().add(create);
		addPermissions(permissions, create, CREATE, singular);

		Action delete = getDeleteAction(bindClass, entityName, singular, plural);
		event.getActions().add(delete);
		addPermissions(permissions, delete, DELETE, singular);

		Action update = getUpdateAction(bindClass, entityName, singular, plural);
		event.getActions().add(update);
		addPermissions(permissions, update, UPDATE, singular);

		writeToOutputStream(event, out);

		return event;
	}

	public Event generateActions(org.appng.xml.application.Permissions permissions, Entity entity) throws Exception {
		return generateActions(permissions, entity, null);
	}

	public Datasources generateDatasources(Entity entity, OutputStream out) throws Exception {
		return generateDatasources(entity, out, new String[0]);
	}

	public Datasources generateDatasources(Entity entity, OutputStream out, String... properties) throws Exception {
		Datasources datasources = new Datasources();
		Datasource all = getListDatasource(entity, null, properties);
		datasources.getDatasourceList().add(all);
		Datasource single = getSingleItemDatasource(entity, null, properties);
		datasources.getDatasourceList().add(single);
		writeToOutputStream(datasources, out);
		return datasources;
	}

	public void writeToOutputStream(Object object, OutputStream out)
			throws ParserConfigurationException, JAXBException, TransformerException {
		if (null != out) {
			marshallService.marshallNonRoot(object, out);
		}
	}

	public Datasource getSingleItemDatasource(Entity entity, OutputStream out, String... properties) throws Exception {
		Datasource single = new Datasource();
		Class<?> bindClass = entity.getType();
		String singular = bindClass.getSimpleName().toLowerCase();
		single.setId(singular);
		DataConfig singleConfig = new DataConfig();
		singleConfig.setParams(getParams(getParam(ID, null)));
		single.setConfig(singleConfig);
		MetaData singleMetaData = new MetaData();
		singleConfig.setMetaData(singleMetaData);

		addFields(bindClass, singleMetaData, true, properties);

		Bean singleBean = new Bean();
		singleBean.setId(entity.getPluralLower());
		BeanOption singleOption = new BeanOption();
		singleOption.setName(ID);
		singleOption.getOtherAttributes().put(new QName(singular + "Id"), ID_PARAM);
		singleBean.getOptions().add(singleOption);
		single.setBean(singleBean);
		writeToOutputStream(single, out);
		return single;
	}

	private Datasource getListDatasource(Entity entity, OutputStream out, String... properties) throws Exception {
		Class<?> bindClass = entity.getType();
		String plural = entity.getPluralLower();
		Datasource all = new Datasource();
		all.setId(plural);
		DataConfig config = new DataConfig();

		config.setParams(new Params());
		config.getParams().getParam().add(getParam("selectedId", null));
		Permissions dsPermissions = getPermissions(bindClass, LIST);
		config.setPermissions(dsPermissions);
		config.setTitle(getLabel(plural));
		MetaData metaData = new MetaData();
		metaData.setBindClass(bindClass.getName());

		boolean hasName = false;
		boolean checkWriteable = false;

		hasName = addFields(bindClass, metaData, checkWriteable, properties);
		FieldDef linkField = new FieldDef();
		linkField.setName(ACTIONS);
		linkField.setType(FieldType.LINKPANEL);
		linkField.setLabel(getLabel(ACTIONS));
		metaData.setResultSelector("${current.id eq selectedId}");
		metaData.getFields().add(linkField);
		config.setMetaData(metaData);
		all.setConfig(config);

		Linkpanel actions = new Linkpanel();
		actions.setId(ACTIONS);
		actions.setLocation(PanelLocation.INLINE);

		String editTarget = SLASH + plural + SLASH + UPDATE + SLASH + CURRENT_ID;
		Link editLink = createLink(bindClass, Linkmode.INTERN, editTarget, EDIT, EDIT, UPDATE);
		editLink.setDefault(TRUE);
		actions.getLinks().add(editLink);

		String deleteTarget = SLASH + plural + "?action=" + DELETE + bindClass.getSimpleName() + "&id=" + CURRENT_ID;
		Link deleteLink = createLink(bindClass, Linkmode.INTERN, deleteTarget, DELETE, DELETE, DELETE);
		Label confirmation = getLabel(getPermissionName(bindClass, DELETE) + "." + CONFIRM);
		deleteLink.setConfirmation(confirmation);
		if (hasName) {
			confirmation.setParams("#{name}");
		}
		actions.getLinks().add(deleteLink);

		config.getLinkpanel().add(actions);

		Linkpanel create = new Linkpanel();
		create.setId("create");
		create.setLocation(PanelLocation.BOTH);

		Link createLink = createLink(bindClass, Linkmode.INTERN, SLASH + plural + SLASH + CREATE, CREATE, "new",
				CREATE);
		create.getLinks().add(createLink);
		config.getLinkpanel().add(create);

		Bean bean = new Bean();
		bean.setId(plural);
		all.setBean(bean);
		writeToOutputStream(all, out);
		return all;
	}

	private Permissions getPermissions(Class<?> bindClass, String action) {
		Permissions permissions = null;
		if (addPermissions) {
			permissions = new Permissions();
			Permission permission = new Permission();
			permission.setMode(PermissionMode.SET);
			permission.setRef(getPermissionName(bindClass, action));
			permissions.getPermissionList().add(permission);
			permissionNames.put(permission.getRef(),
					StringUtils.capitalize(action) + " a " + bindClass.getSimpleName());
		}
		return permissions;
	}

	private String getPermissionName(Class<?> bindClass, String action) {
		return bindClass.getSimpleName().toLowerCase() + "." + action;
	}

	private Link createLink(Class<?> bindClass, Linkmode mode, String target, String labelId, String iconValue,
			String action) {
		Link link = new Link();
		Icon icon = new Icon();
		icon.setContent(iconValue);
		link.setIcon(icon);
		link.setMode(mode);
		link.setTarget(target);
		Label label = getLabel(labelId);
		link.setLabel(label);
		if (null != action) {
			Permissions permissions = getPermissions(bindClass, action);
			link.setPermissions(permissions);
		}
		return link;
	}

	private boolean addFields(Class<?> bindClass, MetaData metaData, boolean checkWriteable, String... properties) {
		boolean versionable = Versionable.class.isAssignableFrom(bindClass);
		boolean identifiable = Identifiable.class.isAssignableFrom(bindClass);
		metaData.setBindClass(bindClass.getName());
		BeanWrapper beanWrapper = new BeanWrapperImpl(bindClass);
		boolean hasName = beanWrapper.isReadableProperty(NAME);
		boolean skipId = true;
		boolean skipVersion = true;
		List<String> propertyNames;
		if (null == properties || properties.length == 0) {
			propertyNames = new ArrayList<>();
			PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				propertyNames.add(propertyDescriptor.getName());
			}
		} else {
			propertyNames = Arrays.asList(properties);
			skipId = false;
			skipVersion = false;
		}

		for (String propertyName : propertyNames) {
			if (beanWrapper.isReadableProperty(propertyName)) {
				FieldDef fieldDef = new FieldDef();
				fieldDef.setName(propertyName);
				Class<?> type = beanWrapper.getPropertyType(propertyName);
				if (type.isAssignableFrom(Integer.class)) {
					fieldDef.setType(FieldType.INT);
				} else if (type.isAssignableFrom(Long.class)) {
					fieldDef.setType(FieldType.LONG);
				} else if (type.isAssignableFrom(Float.class) || type.isAssignableFrom(Double.class)) {
					fieldDef.setType(FieldType.DECIMAL);
				} else if (type.isAssignableFrom(Date.class)) {
					fieldDef.setType(FieldType.DATE);
					fieldDef.setFormat(datePattern);
				} else if (type.isAssignableFrom(Coordinate.class)) {
					fieldDef.setType(FieldType.COORDINATE);
				} else {
					fieldDef.setType(FieldType.TEXT);
				}

				if (checkWriteable && !beanWrapper.isWritableProperty(propertyName)) {
					fieldDef.setReadonly(TRUE);
				}
				boolean isVersion = skipVersion && versionable && "version".equals(propertyName);
				boolean isId = skipId && identifiable && "id".equals(propertyName);
				boolean skip = checkWriteable && (isVersion || isId);
				if (null != fieldDef.getType() && !skip) {
					fieldDef.setLabel(getLabel(propertyName));
					metaData.getFields().add(fieldDef);
				}
			}
		}
		return hasName;
	}

	private Action getUpdateAction(Class<?> bindClass, String entityName, String singular, String plural) {
		Action update = new Action();
		update.setId(UPDATE + entityName);
		DataConfig config = new DataConfig();
		config.setPermissions(getPermissions(bindClass, UPDATE));

		Condition condition = new Condition();
		condition.setExpression("${not empty id and '" + update.getId() + "' eq form_action}");
		update.setCondition(condition);
		config.setParams(getParams(getParam(ID, null), getParam(FORM_ACTION, null)));
		config.setTitle(getLabel(singular + "." + UPDATE));
		update.setConfig(config);

		DatasourceRef datasourceRef = new DatasourceRef();
		datasourceRef.setId(singular);
		datasourceRef.setParams(new Params());
		datasourceRef.getParams().getParam().add(getParam(ID, ID_PARAM));
		update.setDatasource(datasourceRef);

		Bean bean = new Bean();
		bean.setId(plural);
		BeanOption idOption = new BeanOption();
		idOption.setName(ACTION);
		idOption.getOtherAttributes().put(new QName(ID), UPDATE);
		idOption.getOtherAttributes().put(new QName(singular + "Id"), ID_PARAM);
		bean.getOptions().add(idOption);
		update.setBean(bean);

		return update;
	}

	private Action getCreateAction(Class<?> bindClass, String entityName, String singular, String plural) {
		Action update = new Action();
		update.setId(CREATE + entityName);
		DataConfig config = new DataConfig();
		config.setPermissions(getPermissions(bindClass, CREATE));

		Condition condition = new Condition();
		condition.setExpression("${'" + update.getId() + "' eq form_action}");
		update.setCondition(condition);
		config.setParams(getParams(getParam(FORM_ACTION, null)));
		config.setTitle(getLabel(singular + "." + CREATE));
		update.setConfig(config);

		DatasourceRef datasourceRef = new DatasourceRef();
		datasourceRef.setId(singular);
		update.setDatasource(datasourceRef);

		Bean bean = new Bean();
		bean.setId(plural);
		BeanOption idOption = new BeanOption();
		idOption.setName(ACTION);
		idOption.getOtherAttributes().put(new QName(ID), CREATE);
		bean.getOptions().add(idOption);
		update.setBean(bean);

		return update;
	}

	private Action getDeleteAction(Class<?> bindClass, String entityName, String singular, String plural) {
		Action delete = new Action();
		delete.setId(DELETE + entityName);
		DataConfig config = new DataConfig();
		config.setPermissions(getPermissions(bindClass, DELETE));

		Condition condition = new Condition();
		condition.setExpression("${not empty id and '" + delete.getId() + "' eq action}");
		delete.setCondition(condition);
		config.setParams(getParams(getParam(ID, null), getParam(ACTION, null)));
		config.setTitle(getLabel(singular + "." + DELETE));
		delete.setConfig(config);

		Bean bean = new Bean();
		bean.setId(plural);
		BeanOption idOption = new BeanOption();
		idOption.setName(ACTION);
		idOption.getOtherAttributes().put(new QName(ID), DELETE);
		idOption.getOtherAttributes().put(new QName(singular + "Id"), ID_PARAM);
		bean.getOptions().add(idOption);
		delete.setBean(bean);

		return delete;
	}

	private void addPermissions(org.appng.xml.application.Permissions permissions, Action action, String actionName,
			String entityName) {
		Permissions actionPermissions = action.getConfig().getPermissions();
		if (null != actionPermissions) {
			List<Permission> permissionList = actionPermissions.getPermissionList();
			for (Permission permission : permissionList) {
				org.appng.xml.application.Permission p = new org.appng.xml.application.Permission();
				p.setId(permission.getRef());
				p.setValue(actionName + " a " + entityName);
				permissions.getPermission().add(p);
			}

		}
	}

	private Label getLabel(String id) {
		Label label = new Label();
		label.setId(id);
		dictionary.put(id, id);
		return label;
	}

	public void writeDictionary(OutputStream out) throws IOException {
		char previous = ' ';
		for (String key : dictionary.keySet()) {
			if (key.charAt(0) != previous) {
				IOUtils.write(("#" + key.charAt(0)).toUpperCase() + "\n", out, UTF_8);
			}
			previous = key.charAt(0);
			IOUtils.write(key + "=" + dictionary.get(key) + "\n", out, UTF_8);
		}
	}

	private void writeApplicationXml(OutputStream out) throws IOException {
		IOUtils.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n", out, UTF_8);
		IOUtils.write("<application>\r\n", out, UTF_8);

		IOUtils.write("\t<roles>\r\n", out, UTF_8);
		IOUtils.write("\t\t<role>\r\n", out, UTF_8);
		IOUtils.write("\t\t\t<name>Admin</name>\r\n", out, UTF_8);
		IOUtils.write("\t\t\t<description>Admin with all permissions</description>\r\n", out, UTF_8);
		for (String p : permissionNames.keySet()) {
			IOUtils.write("\t\t\t<permission id=\"" + p + "\" />\r\n", out, UTF_8);
		}
		IOUtils.write("\t\t</role>\r\n", out, UTF_8);
		IOUtils.write("\t</roles>\r\n", out, UTF_8);

		IOUtils.write("\t<permissions>\r\n", out, UTF_8);
		for (String p : permissionNames.keySet()) {
			IOUtils.write("\t\t<permission id=\"" + p + "\">" + permissionNames.get(p) + "</permission>\r\n", out,
					UTF_8);
		}
		IOUtils.write("\t</permissions>\r\n", out, UTF_8);
		IOUtils.write("</application>", out, UTF_8);
	}

	public ApplicationRootConfig generateRootConfig(String name) {
		ApplicationRootConfig rootConfig = new ApplicationRootConfig();
		rootConfig.setName(name);
		ApplicationConfig appCfg = new ApplicationConfig();
		appCfg.setSession(new Session());
		appCfg.getSession().setSessionParams(new SessionParams());
		rootConfig.setConfig(appCfg);
		Linkpanel navigation = new Linkpanel();
		navigation.setLocation(PanelLocation.TOP);
		navigation.setId("nav");
		rootConfig.setNavigation(navigation);
		return rootConfig;
	}

	public String getDatePattern() {
		return datePattern;
	}

	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}
}
