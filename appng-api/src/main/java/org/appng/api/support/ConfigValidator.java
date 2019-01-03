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
package org.appng.api.support;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ParameterSupport;
import org.appng.api.PermissionOwner;
import org.appng.api.XPathProcessor;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.Permission;
import org.appng.xml.application.PermissionRef;
import org.appng.xml.application.Permissions;
import org.appng.xml.application.Properties;
import org.appng.xml.application.Property;
import org.appng.xml.application.Role;
import org.appng.xml.application.Roles;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.BeanOption;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.GetParams;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkmode;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.PageConfig;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.PostParams;
import org.appng.xml.platform.SectionDef;
import org.appng.xml.platform.SectionelementDef;
import org.appng.xml.platform.UrlParams;
import org.appng.xml.platform.UrlSchema;
import org.appng.xml.platform.ValidationGroups.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Validates a {@link ApplicationConfigProvider}, which means it checks that each reference to a <br/>
 * <ul>
 * <li>page</li>
 * <li>datasource</li>
 * <li>event</li>
 * <li>action</li>
 * <li>parameter</li>
 * </ul>
 * is valid.
 * 
 * @author Matthias Müller
 * 
 */
public class ConfigValidator {

	private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

	private ApplicationConfigProvider provider;
	private Set<String> errors;
	private List<ConfigValidationError> detailedErrors;
	private Set<String> warnings;
	private boolean throwException = false;
	private boolean clearErrors = true;
	private boolean withDetailedErrors = false;
	private Set<String> permissionNames = new HashSet<String>();

	public ConfigValidator(ApplicationConfigProvider provider, boolean throwException, boolean clearErrors) {
		this(provider);
		this.throwException = throwException;
		this.clearErrors = clearErrors;
	}

	public ConfigValidator(ApplicationConfigProvider provider) {
		this.provider = provider;
		this.errors = new HashSet<String>();
		this.warnings = new HashSet<String>();
		this.detailedErrors = new ArrayList<ConfigValidationError>();
	}

	public void validate(String applicationName) throws InvalidConfigurationException {
		validate(applicationName, Thread.currentThread().getContextClassLoader());
	}

	public void validate(String applicationName, ClassLoader classLoader) throws InvalidConfigurationException {
		readPermissions();
		validateApplicationInfo();
		validateApplication(applicationName, classLoader);
	}

	public void setWithDetailedErrors(boolean withDetails) {
		this.withDetailedErrors = withDetails;
	}

	private void validateAction(String eventId, Action action) {
		String resourceName = provider.getResourceNameForEvent(eventId);
		String actionOrigin = resourceName + ": action '" + action.getId() + "':";
		Resource resource = getResourceIfPresent(ResourceType.XML, resourceName);
		String xpathBase = "//action[@id='" + action.getId() + "']";
		validateConditionForAction(resource, action.getId(), action.getConfig().getParams(), action.getCondition(),
				actionOrigin, xpathBase + "/condition");
		checkPermissions(new PermissionOwner(action), actionOrigin, resource, xpathBase + "/config");

		DatasourceRef datasourceRef = action.getDatasource();
		if (null != datasourceRef) {
			String dsId = datasourceRef.getId();
			validateConditionForAction(resource, action.getId(), action.getConfig().getParams(),
					datasourceRef.getCondition(), actionOrigin + ", datasource '" + dsId + "'",
					xpathBase + "/datasource/condition");
			Datasource datasource = provider.getDatasource(dsId);
			if (null == datasource) {
				String error = actionOrigin + "  references the unknown datasource '" + dsId + "'.";
				addDetailedError(error, resource, xpathBase + "/datasource");
			} else {
				Params params = action.getConfig().getParams();
				Map<String, String> parameterMap = getParameterMap(params == null ? null : params.getParam());
				validateDataSourceParameters(actionOrigin, datasourceRef, parameterMap, resourceName);
			}
		}
	}

	private void validateApplication(String applicationName, ClassLoader classLoader)
			throws InvalidConfigurationException {
		long start = System.currentTimeMillis();
		validateApplicationRootConfig();
		validateDataSources(classLoader);
		validateActions();
		Map<String, PageDefinition> pageMap = provider.getPages();
		for (String pageId : pageMap.keySet()) {
			Resource resource = getResourceIfPresent(ResourceType.XML, provider.getResourceNameForPage(pageId));
			String origin = getPagePrefix(pageId);
			PageDefinition page = provider.getPage(pageId);
			PageConfig config = page.getConfig();
			checkPermissions(new PermissionOwner(page), origin, resource, "//page[@id='" + pageId + "']/config");
			int i = 1;
			for (SectionDef sectionDef : page.getStructure().getSection()) {
				String hidden = sectionDef.getHidden();
				validateExpressionForPage(resource, config.getUrlSchema(), hidden, pageId,
						origin + " section[" + i + "]", "hidden", "//section[" + i + "]");
				List<SectionelementDef> elements = sectionDef.getElement();
				int j = 1;
				for (SectionelementDef sectionelement : elements) {
					String folded = sectionelement.getFolded();
					validateExpressionForPage(resource, config.getUrlSchema(), folded, pageId,
							origin + " section[" + i + "]/element[" + j + "]", "folded",
							"//section[" + i + "]/element[" + j + "]");
					String passive = sectionelement.getPassive();
					validateExpressionForPage(resource, config.getUrlSchema(), passive, pageId,
							origin + " section[" + i + "]/element[" + j + "]", "passive",
							"//section[" + i + "]/element[" + j + "]");
					ActionRef actionRef = sectionelement.getAction();
					DatasourceRef datasourceRef = sectionelement.getDatasource();
					if (null != actionRef) {
						String xpathBase = "//action[@id='" + actionRef.getId() + "']";
						String actionOrigin = origin + ", action '" + actionRef.getId() + "'";
						validateIncludeConditionForPage(resource, config.getUrlSchema(), actionRef.getCondition(),
								pageId, actionOrigin, xpathBase);
						checkPermissions(new PermissionOwner(actionRef), actionOrigin, resource, xpathBase);
						String eventId = actionRef.getEventId();
						Event event = provider.getEvent(eventId);
						if (null == event) {
							String message = origin + " references the unknown event '" + eventId + "'.";
							addDetailedError(message, resource, xpathBase + "[@eventId='" + eventId + "']");
						} else {
							String actionId = actionRef.getId();
							if (!provider.getActions(eventId).containsKey(actionId)) {
								String message = getPagePrefix(pageId);
								message += " references the unknown action '" + actionId + "' (from event '" + eventId
										+ "').";
								addDetailedError(message, resource, xpathBase + "[@eventId='" + eventId + "']");
							} else {
								validateActionParameters(pageId, actionRef);
								// validateActionRef(pageId, actionRef);
							}
						}
					} else if (null != datasourceRef) {
						String dsId = datasourceRef.getId();
						String xpathBase = "//datasource[@id='" + dsId + "']";
						String dsOrigin = origin + ", datasource '" + dsId + "'";
						validateIncludeConditionForPage(resource, config.getUrlSchema(), datasourceRef.getCondition(),
								pageId, dsOrigin, xpathBase);
						checkPermissions(new PermissionOwner(datasourceRef), dsOrigin, resource, xpathBase);
						Datasource datasource = provider.getDatasource(dsId);
						if (null == datasource) {
							String message = getPagePrefix(pageId);
							message += " references the unknown datasource '" + dsId + "'.";
							addDetailedError(message, resource, xpathBase);
						} else {
							validateDataSourceParameters(origin, datasourceRef, getAllPageParams(page),
									provider.getResourceNameForPage(pageId));
						}
					} else {
						// invalid
					}
					j++;
				}
				i++;
			}
		}
		log.info("validated application '" + applicationName + "' in " + (System.currentTimeMillis() - start) + "ms");
		processErrors(applicationName);
	}

	private Map<String, String> getAllPageParams(PageDefinition page) {
		UrlSchema urlSchema = page.getConfig().getUrlSchema();
		Map<String, String> params = new HashMap<String, String>();
		if (null != urlSchema) {
			PostParams postParams = urlSchema.getPostParams();
			if (null != postParams) {
				params.putAll(getParameterMap(postParams.getParamList()));
			}
			GetParams getParams = urlSchema.getGetParams();
			if (null != getParams) {
				params.putAll(getParameterMap(getParams.getParamList()));
			}
			UrlParams urlParams = urlSchema.getUrlParams();
			if (null != urlParams) {
				params.putAll(getParameterMap(urlParams.getParamList()));
			}
		}
		return params;
	}

	private Map<String, String> getPageGetParams(PageDefinition page) {
		UrlSchema urlSchema = page.getConfig().getUrlSchema();
		Map<String, String> params = new HashMap<String, String>();
		if (null != urlSchema) {
			GetParams getParams = urlSchema.getGetParams();
			if (null != getParams) {
				params.putAll(getParameterMap(getParams.getParamList()));
			}
		}
		return params;
	}

	private void validateApplicationRootConfig() {
		ApplicationRootConfig applicationRootConfig = provider.getApplicationRootConfig();
		if (null == applicationRootConfig) {
			if (!provider.getPages().isEmpty()) {
				this.errors.add("No <applicationRootConfig> found, application will not work!");
				return;
			}
		} else {
			validateLinkPanel(applicationRootConfig.getNavigation(), "application-info.xml:",
					provider.getResourceNameForApplicationRootConfig(), "//navigation/");
		}
	}

	private void validateLinkPanel(Linkpanel linkpanel, String origin, String originResourceName,
			String linkpanelXPath) {
		if (null != linkpanel) {
			Resource resource = getResourceIfPresent(ResourceType.XML, originResourceName);
			checkPermissions(new PermissionOwner(linkpanel), origin + " linkpanel '" + linkpanel.getId() + "'",
					resource, linkpanelXPath);
			for (Link link : linkpanel.getLinks()) {
				String target = link.getTarget();
				String linkOrigin = origin + " linkpanel '" + linkpanel.getId() + "' link '" + target + "'";
				String xpathBase = "/link[@target='" + target + "']";
				Linkmode mode = link.getMode();
				if (Linkmode.INTERN.equals(mode)) {
					String pageName = null;
					if (StringUtils.isNotBlank(target)) {
						if (target.startsWith("/")) {
							int endIdx = target.indexOf("/", 1);
							pageName = target.substring(1, endIdx > 0 ? endIdx : target.length());
							endIdx = pageName.indexOf('?');
							if (endIdx > 0) {
								pageName = pageName.substring(0, endIdx);
							}
						} else if (!target.startsWith("?") && !target.startsWith("$")) {
							String message = linkOrigin
									+ " points to an invalid target, must start with '/', '${<param>}' or '?'!";
							addDetailedError(message, resource, linkpanelXPath + xpathBase);
						}
					}
					if (null != pageName && !pageName.startsWith("$")) {
						PageDefinition page = provider.getPage(pageName);
						if (page == null) {
							String message = linkOrigin + " points to the unknown page '" + pageName + "'";
							addDetailedError(message, resource, linkpanelXPath + xpathBase);

						} else if (StringUtils.isNotBlank(target)) {
							List<String> queryParameters = new ArrayList<String>();
							int idx = target.indexOf('?');
							if (idx > 0) {
								String query = target.substring(idx + 1);
								for (String pair : query.split("&")) {
									int eqIdx = pair.indexOf("=");
									queryParameters.add(pair.substring(0, eqIdx));
								}
							}

							Map<String, String> allGetParams = getPageGetParams(page);
							Set<String> getParamNames = allGetParams.keySet();
							@SuppressWarnings("rawtypes")
							Collection unknownGetParams = CollectionUtils.subtract(queryParameters, getParamNames);
							if (!unknownGetParams.isEmpty()) {
								String message = linkOrigin + " points to page '" + pageName
										+ "' and uses the unknown get-paramter(s) " + unknownGetParams
										+ ". Valid get-parameters are: " + getParamNames;
								addDetailedError(message, resource, linkpanelXPath + xpathBase);
							}
						}
					}
				}
				checkPermissions(new PermissionOwner(link), linkOrigin, resource, linkpanelXPath + xpathBase);
			}
		}
	}

	private void validateApplicationInfo() {
		Set<String> rolesNames = new HashSet<String>();
		Resource resource = getApplicationXml();
		checkApplicationProperties();
		Roles roles = provider.getApplicationInfo().getRoles();
		if (null != roles) {
			for (Role role : roles.getRole()) {
				if (!rolesNames.add(role.getName())) {
					String message = ResourceType.APPLICATION_XML_NAME + ": Duplicate role: " + role.getName();
					addDetailedError(message, resource, "//role/name[text()='" + role.getName() + "']");
				}
				Set<String> rolePermissions = new HashSet<String>();
				for (PermissionRef permissionRef : role.getPermission()) {
					String permissionId = permissionRef.getId();
					String xpath = "//role[name[text()='" + role.getName() + "']]/permission[@id='" + permissionId
							+ "']";
					if (!permissionNames.contains(permissionId)) {
						String message = ResourceType.APPLICATION_XML_NAME + ": The role '" + role.getName()
								+ "' references the unknown permission '" + permissionId + "'.";
						addDetailedError(message, resource, xpath);
					} else if (!rolePermissions.add(permissionId)) {
						String message = ResourceType.APPLICATION_XML_NAME + ": The role '" + role.getName()
								+ "' references the permission '" + permissionId + "' more than once!";
						addDetailedError(message, resource, xpath);
					}
				}
			}
		}
	}

	private Resource getApplicationXml() {
		return getResourceIfPresent(ResourceType.APPLICATION, ResourceType.APPLICATION_XML_NAME);
	}

	private Resource getResourceIfPresent(ResourceType type, String resourceName) {
		Resources resources = provider.getResources();
		if (null != resources) {
			return resources.getResource(type, resourceName);
		}
		return null;
	}

	private void readPermissions() {
		ApplicationInfo applicationInfo = provider.getApplicationInfo();
		if (null != applicationInfo) {
			Permissions permissions = applicationInfo.getPermissions();
			if (null != permissions) {
				List<Permission> permissionList = permissions.getPermission();
				Resource resource = getApplicationXml();
				for (Permission permission : permissionList) {
					if (!this.permissionNames.add(permission.getId())) {
						String message = ResourceType.APPLICATION_XML_NAME + ": Duplicate permission '"
								+ permission.getId() + "'.";
						addDetailedError(message, resource,
								"//permissions/permission[@id='" + permission.getId() + "']");
					}
				}
			}
		}
	}

	private void checkApplicationProperties() {
		ApplicationInfo applicationInfo = provider.getApplicationInfo();
		Resource resource = getApplicationXml();
		if (null != applicationInfo) {
			Properties properties = applicationInfo.getProperties();
			Set<String> propertyIds = new HashSet<String>();
			for (Property p : properties.getProperty()) {
				if (!propertyIds.add(p.getId())) {
					String message = ResourceType.APPLICATION_XML_NAME + ": Duplicate property '" + p.getId() + "'.";
					addDetailedError(message, resource, "//properties/property[@id='" + p.getId() + "']");
				}
			}
		}
	}

	private void checkPermissions(PermissionOwner owner, String origin, Resource resource, String xpathBase) {
		Collection<org.appng.xml.platform.Permission> permissions = owner.getPermissions();
		if (null != permissions) {
			for (org.appng.xml.platform.Permission permission : permissions) {
				String permName = permission.getRef();
				if (!permName.startsWith(DefaultPermissionProcessor.PREFIX_ANONYMOUS)
						&& !permissionNames.contains(permName)) {
					String message = origin + " references the unknown permission '" + permName + "'.";
					addDetailedError(message, resource, xpathBase + "/permissions/permission[@ref='" + permName + "']");
				}
			}
		}
	}

	private void validateConditionForAction(Resource origin, String actionId, Params params, Condition condition,
			String originName, String xpath) {
		if (null != condition && StringUtils.isNotBlank(condition.getExpression())) {
			if (!condition.getExpression().startsWith("${") || !condition.getExpression().endsWith("}")) {
				addDetailedError(originName + " invalid condition '" + condition.getExpression() + "'", origin, xpath);
			} else {
				Map<String, Object> variables = getVariables(params);
				String message = originName + " condition: '" + condition.getExpression() + "' is invalid: ";
				ExpressionEvaluator ee = getExpressionEvaluator("action", actionId, variables);
				validateCondition(origin, ee, condition, xpath, message);
			}
		}
	}

	private Map<String, Object> getVariables(Params params) {
		Map<String, Object> variables = new HashMap<>();
		if (params != null) {
			params.getParam().forEach(p -> variables.put(p.getName(), p.getValue()));
		}
		return variables;
	}

	private void validateIncludeConditionForPage(Resource pageResource, UrlSchema params, Condition condition,
			String pageId, String originName, String xpath) {
		if (null != condition && StringUtils.isNotBlank(condition.getExpression())) {
			if (!condition.getExpression().startsWith("${") || !condition.getExpression().endsWith("}")) {
				addDetailedError(originName + " invalid condition '" + condition.getExpression() + "'", pageResource, xpath);
			} else {
				ExpressionEvaluator ee = getPageExpressionEvaluator(params, pageId);
				String message = originName + " condition: '" + condition.getExpression() + "' is invalid: ";
				validateCondition(pageResource, ee, condition, xpath, message);
			}
		}
	}

	private ExpressionEvaluator getPageExpressionEvaluator(UrlSchema params, String pageId) {
		Map<String, Object> variables = new HashMap<>();
		if (null != params.getGetParams()) {
			addParams(variables, params.getGetParams().getParamList());
		}
		if (null != params.getPostParams()) {
			addParams(variables, params.getPostParams().getParamList());
		}
		if (null != params.getUrlParams()) {
			addParams(variables, params.getUrlParams().getParamList());
		}
		return getExpressionEvaluator("page", pageId, variables);
	}

	private void addParams(Map<String, Object> variables, List<Param> paramList) {
		if (null != paramList) {
			paramList.forEach(p -> variables.put(p.getName(), p.getValue()));
		}
	}

	private void validateExpressionForPage(Resource resource, UrlSchema urlSchema, String expression, String pageId,
			String origin, String attribute, String xpath) {
		if (StringUtils.isNotBlank(expression)
				&& !("true".equalsIgnoreCase(expression) || "false".equalsIgnoreCase(expression))) {
			ExpressionEvaluator ee = getPageExpressionEvaluator(urlSchema, pageId);
			String message = origin + " attribute '" + attribute + "': ";
			validateCondition(resource, ee, expression, xpath, message);
		}

	}

	private ExpressionEvaluator getExpressionEvaluator(String type, String id, Map<String, Object> variables) {
		VariableMapper variableMapper = new VariableMapper() {

			public ValueExpression setVariable(String variable, ValueExpression expression) {
				return null;
			}

			public ValueExpression resolveVariable(String variable) {
				if (!variables.containsKey(variable)) {
					throw new ELException("parameter '" + variable + "' not found on " + type + " '" + id + "'");
				}
				return new ValueExpression() {

					public boolean isLiteralText() {
						return false;
					}

					public int hashCode() {
						return 0;
					}

					public String getExpressionString() {
						return null;
					}

					public boolean equals(Object obj) {
						return false;
					}

					public void setValue(ELContext context, Object value) {

					}

					public boolean isReadOnly(ELContext context) {
						return false;
					}

					public Object getValue(ELContext context) {
						return variables.get(variable);
					}

					public Class<?> getType(ELContext context) {
						return null;
					}

					public Class<?> getExpectedType() {
						return null;
					}
				};
			}
		};
		ExpressionEvaluator ee = new ExpressionEvaluator(variableMapper);
		return ee;
	}

	public final void processErrors(String applicationName) throws InvalidConfigurationException {
		if (getWarnings().size() > 0 || getErrors().size() > 0) {
			if (!getWarnings().isEmpty()) {
				StringBuilder sb = appendAndClear("found warnings:", getWarnings());
				log.warn(sb.toString());
			}
			if (!getErrors().isEmpty()) {
				StringBuilder sb = appendAndClear("found errors:", getErrors());
				if (throwException) {
					throw new InvalidConfigurationException(applicationName, sb.toString());
				} else {
					log.error(sb.toString());
				}
			}
		} else {
			log.info("validation returned no errors and no warnings");
		}
	}

	private StringBuilder appendAndClear(String start, Set<String> messages) {
		List<String> sorted = new ArrayList<String>(messages);
		Collections.sort(sorted);
		StringBuilder sb = new StringBuilder(start);
		sorted.forEach(warning -> {
			sb.append("\r\n");
			sb.append(warning);
		});
		if (clearErrors) {
			messages.clear();
		}
		return sb;
	}

	public void validateDataSources(ClassLoader classLoader) {
		Set<String> keySet = provider.getDataSources().keySet();
		for (String id : keySet) {
			String resourceName = provider.getResourceNameForDataSource(id);
			String messagePrefix = resourceName + ": datasource '" + id + "':";
			Datasource datasource = provider.getDatasource(id);
			String xpathBase = "//datasource[@id='" + id + "']/config/";
			Resource resource = getResourceIfPresent(ResourceType.XML, resourceName);
			checkPermissions(new PermissionOwner(datasource), messagePrefix, resource, xpathBase);
			List<Linkpanel> linkpanels = datasource.getConfig().getLinkpanel();
			for (Linkpanel linkpanel : linkpanels) {
				validateLinkPanel(linkpanel, messagePrefix, resourceName, xpathBase + "linkpanel/");
			}

			MetaData metaData = datasource.getConfig().getMetaData();
			if (StringUtils.isBlank(metaData.getBindClass())) {
				String message = messagePrefix + " no bindclass given!";
				addDetailedError(message, resource, xpathBase + "meta-data");
			} else {
				Map<String, Object> variables = getVariables(datasource.getConfig().getParams());
				Object bindObject = getBindObject(classLoader, metaData.getBindClass());
				if (null != bindObject) {
					variables.put("current", bindObject);
				}
				ExpressionEvaluator ee = getExpressionEvaluator("datasource", id, variables);
				for (FieldDef fieldDef : metaData.getFields()) {
					String fieldXpath = xpathBase + "meta-data/field[@name='" + fieldDef.getName() + "']";
					Condition condition = fieldDef.getCondition();
					if (null != condition) {
						String xpath = fieldXpath + "/condition";
						String message = messagePrefix + " field: " + fieldDef.getName() + ", condition: '"
								+ condition.getExpression() + "' is invalid: ";
						validateCondition(resource, ee, condition, xpath, message);
					}
					String hiddenMessage = messagePrefix + " field: " + fieldDef.getName() + ", hidden: '"
							+ fieldDef.getHidden() + "' is invalid: ";
					validateCondition(resource, ee, fieldDef.getHidden(), fieldXpath, hiddenMessage);

					String readOnlyMessage = messagePrefix + " field: " + fieldDef.getName() + ", readOnly: '"
							+ fieldDef.getReadonly() + "' is invalid: ";
					validateCondition(resource, ee, fieldDef.getReadonly(), fieldXpath, readOnlyMessage);
				}
			}
		}
	}

	private Object getBindObject(ClassLoader classLoader, String bindClassName) {
		try {
			Class<?> bindClass = ClassUtils.forName(bindClassName, classLoader);
			Class<?> enclosingClass = bindClass.getEnclosingClass();
			int modifier = bindClass.getModifiers();
			if (bindClass.isInterface()) {
				return Proxy.newProxyInstance(classLoader, new Class[] { bindClass }, new InvocationHandler() {

					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
				});
			} else if (enclosingClass == null || Modifier.isStatic(modifier)) {
				return bindClass.newInstance();
			} else if (Modifier.isPublic(modifier)) {
				return bindClass.getConstructor(enclosingClass).newInstance(enclosingClass.newInstance());
			}
		} catch (ReflectiveOperationException o) {
			String errorMssg = String.format(
					"could not create instance of '%s', is it an interface or default-constructor missing?",
					bindClassName);
			log.debug(errorMssg);
		}
		return null;
	}

	private void validateCondition(Resource resource, ExpressionEvaluator ee, Condition condition, String xpath,
			String message) {
		validateCondition(resource, ee, condition.getExpression(), xpath, message);
	}

	private void validateCondition(Resource resource, ExpressionEvaluator ee, String expression, String xpath,
			String message) {
		if (StringUtils.isNotBlank(expression)) {
			try {
				ee.evaluate(expression);
			} catch (ELException ele) {
				message += ele.getMessage();
				addDetailedError(message, resource, xpath);
			}
		}
	}

	public void validateActions() {
		Set<String> eventIds = provider.getEventIds();
		for (String eventId : eventIds) {
			Event event = provider.getEvent(eventId);
			String origin = provider.getResourceNameForEvent(eventId);
			Resource resource = getResourceIfPresent(ResourceType.XML, origin);
			checkPermissions(new PermissionOwner(event), origin + ": event '" + eventId + "'", resource,
					"//event[@id='" + eventId + "']/config");
			for (Action action : event.getActions()) {
				validateAction(eventId, action);
			}
		}
	}

	public void validateMetaData(URLClassLoader classLoader) {
		if (null != classLoader) {
			Set<String> keySet = provider.getDataSources().keySet();
			for (String id : keySet) {
				String resourceName = provider.getResourceNameForDataSource(id);
				String messagePrefix = resourceName + ": datasource '" + id + "':";
				Datasource datasource = provider.getDatasource(id);
				MetaData metaData = datasource.getConfig().getMetaData();
				checkBindClass(classLoader, id, resourceName, messagePrefix, metaData);
				checkValidationGroupClass(classLoader, id, resourceName, messagePrefix, metaData);
			}
		}
	}

	private void checkValidationGroupClass(URLClassLoader classLoader, String id, String resourceName,
			String messagePrefix, MetaData metaData) throws LinkageError {
		if (null != metaData.getValidation()) {
			List<Group> validationGroups = metaData.getValidation().getGroups();
			for (Group group : validationGroups) {
				String validationGroupClassName = group.getClazz();
				try {
					// we just try to get the class from the class loader.
					// If it throws an exception we will create an error message and maybe a detailed error
					@SuppressWarnings("unused")
					Class<?> validationGroupClass = ClassUtils.forName(validationGroupClassName, classLoader);
				} catch (ClassNotFoundException e) {
					String message = messagePrefix + " validation group class '" + validationGroupClassName
							+ "' not found!";
					addDetailedError(message, getResourceIfPresent(ResourceType.XML, resourceName), "//datasource[@id='"
							+ id + "']/config/meta-data/validation/group[@class='" + validationGroupClassName + "']");
				}
			}
		}
	}

	private void checkBindClass(URLClassLoader classLoader, String id, String resourceName, String messagePrefix,
			MetaData metaData) throws LinkageError {
		String xpath = "//datasource[@id='" + id + "']/config/meta-data";
		String bindClassName = metaData.getBindClass();
		try {
			Class<?> bindClass = ClassUtils.forName(bindClassName, classLoader);
			ClassWrapper wrapper = new ClassWrapper(bindClass);
			List<FieldDef> fields = metaData.getFields();
			String binding = metaData.getBinding();
			for (FieldDef fieldDef : fields) {
				if (!FieldType.LINKPANEL.equals(fieldDef.getType())) {
					String name = fieldDef.getName();
					String property = fieldDef.getBinding();
					if (property == null) {
						property = (binding == null ? "" : binding + ".") + name;
					}
					if (!wrapper.isReadableProperty(property)) {
						String message = messagePrefix + " property '" + property + "' of class '" + bindClassName
								+ "' is not readable!";
						getWarnings().add(message);
						// addDetailedError(message, getResourceIfPresent(ResourceType.XML, resourceName),
						// xpath + "/field[@name='" + name + "']");
					}
					if (!"true".equals(fieldDef.getReadonly())) {
						boolean isWritable = wrapper.isWritableProperty(property);
						if (!isWritable) {
							String message = messagePrefix + " property '" + property + "' of class '" + bindClassName
									+ "' is not writable!";
							getWarnings().add(message);
						}
					}
				}
			}
		} catch (ClassNotFoundException e) {
			String message = messagePrefix + " bindclass '" + bindClassName + "' not found!";
			addDetailedError(message, getResourceIfPresent(ResourceType.XML, resourceName), xpath);
		}
	}

	private void validateDataSourceParameters(String origin, DatasourceRef datasourceRef,
			Map<String, String> availableParams, String resourceName) {
		String dsId = datasourceRef.getId();
		Datasource datasource = provider.getDatasource(dsId);

		Params refParams = datasourceRef.getParams();
		Params dataSourceParams = datasource.getConfig().getParams();
		String type = "dataSource";
		String dsResource = provider.getResourceNameForDataSource(dsId);
		Map<String, String> paramsFromDatasource = checkReferenceParameters(origin, dsId, refParams, dataSourceParams,
				type, dsResource, availableParams, resourceName);

		ParameterSupport dsParameterSupport = new DollarParameterSupport(paramsFromDatasource);

		Bean bean = datasource.getBean();
		if (null == bean) {
			log.debug("datasource '" + datasource.getId() + "' is static");
			return;
		}
		validateBeanOptions(dsResource + ": datasource '" + dsId + "'", dataSourceParams, dsParameterSupport, bean,
				dsResource, "datasource", dsId);
	}

	private Map<String, String> checkReferenceParameters(String origin, String refId, Params refParams,
			Params execParams, String type, String resourceName, Map<String, String> availableParams,
			String resourceFileName) {
		Map<String, String> paramsFromRef = getParameterMap(refParams == null ? null : refParams.getParam());
		Map<String, String> paramsFromExec = getParameterMap(execParams == null ? null : execParams.getParam());

		Set<String> execParamNames = paramsFromExec.keySet();

		Set<String> paramNames = new HashSet<String>();
		for (String string : paramsFromRef.values()) {
			if (string.matches("\\$\\{(.)*\\}")) {
				String param = string.replace("${", "").replace("}", "");
				if (StringUtils.isNotBlank(param)) {
					paramNames.add(param);
				}
			}
		}

		@SuppressWarnings("unchecked")
		Collection<String> missingRefParameters = CollectionUtils.subtract(paramNames, availableParams.keySet());
		Resource resource = getResourceIfPresent(ResourceType.XML, resourceFileName);
		String xpathBase = "//" + type.toLowerCase() + "[@id='" + refId + "']/params/";
		if (!missingRefParameters.isEmpty()) {
			for (String missing : missingRefParameters) {
				// we need the type and id of the origin
				String message = origin + " the reference to " + type + " '" + refId + "' uses the unknown parameter "
						+ missing + ". Supported parameters are: " + availableParams.keySet();
				addDetailedError(message, resource, xpathBase + "param[text()='${" + missing + "}']");
			}
		}

		@SuppressWarnings("unchecked")
		Collection<String> unsupportedParameters = CollectionUtils.subtract(paramsFromRef.keySet(), execParamNames);
		if (!unsupportedParameters.isEmpty()) {
			for (String unsupported : unsupportedParameters) {
				String message = origin + " the reference to " + type + " '" + refId + "' (from '" + resourceName
						+ "') uses the parameter " + unsupported + " which is unknown in the " + type + " '" + refId
						+ "'! Supported parameters are: " + execParamNames;
				addDetailedError(message, resource, xpathBase + "param[@name='" + unsupported + "']");
			}
		}
		return paramsFromExec;
	}

	private void validateBeanOptions(String origin, Params params, ParameterSupport parameterSupport, Bean bean,
			String resourceName, String originType, String originId) {
		List<BeanOption> options = bean.getOptions();
		if (null != options) {
			for (BeanOption option : options) {
				Map<QName, String> otherAttributes = option.getOtherAttributes();
				for (QName key : otherAttributes.keySet()) {
					String value = otherAttributes.get(key);
					List<String> paramNames = parameterSupport.getParameters(value);
					for (String paramName : paramNames) {
						boolean found = findParameter(params, paramName);
						if (!found) {
							String message = origin + ", option '" + option.getName()
									+ "' references the unknown parameter '" + paramName + "'. Valid parameters are: "
									+ parameterSupport.getParameterNames();
							addDetailedError(message, getResourceIfPresent(ResourceType.XML, resourceName),
									"//" + originType.toLowerCase() + "[@id='" + originId + "']/bean/option[@" + key
											+ "='" + value + "']");
						}
					}
				}
			}
		}
	}

	private Map<String, String> getParameterMap(List<Param> params) {
		Map<String, String> parameters = new HashMap<String, String>();
		if (null != params) {
			for (Param param : params) {
				String value = StringUtils.isNotBlank(param.getValue()) ? param.getValue()
						: StringUtils.isNotBlank(param.getDefault()) ? param.getDefault() : "value";
				parameters.put(param.getName(), value);
			}
		}
		return parameters;
	}

	private void validateActionParameters(String pageId, ActionRef actionRef) {
		String eventId = actionRef.getEventId();
		String actionId = actionRef.getId();
		Action action = provider.getAction(eventId, actionId);
		String origin = getPagePrefix(pageId);
		Params params = action.getConfig().getParams();
		String resourceName = provider.getResourceNameForEvent(eventId);
		PageDefinition page = provider.getPage(pageId);
		Map<String, String> paramsFromDatasource = checkReferenceParameters(origin, actionId, actionRef.getParams(),
				params, "action", resourceName, getAllPageParams(page), provider.getResourceNameForPage(pageId));

		Bean bean = action.getBean();
		if (null != bean && null != bean.getOptions()) {
			ParameterSupport actionParamMapSupport = new DollarParameterSupport(paramsFromDatasource);
			String actionOrigin = resourceName + ": action '" + actionId + "'";
			validateBeanOptions(actionOrigin, params, actionParamMapSupport, bean, resourceName, "action", actionId);
		}
	}

	private String getPagePrefix(String pageId) {
		return provider.getResourceNameForPage(pageId) + ": page '" + pageId + "'";
	}

	private void addDetailedError(String error, Resource resource, String xpath) {
		this.errors.add(error);
		if (withDetailedErrors) {
			NodeList nodes = getNodesWithPositionForXpath(resource, xpath);
			if (null != nodes && nodes.getLength() > 0) {
				for (int x = 0; x < nodes.getLength(); x++) {
					ConfigValidationError detailError = new ConfigValidationError();
					detailError.setMessage(error);
					detailError.setLine((Integer) nodes.item(x).getUserData(PositionalXMLReader.LINE_NUMBER_KEY_NAME));
					detailError.setResourceName(resource.getName());
					detailedErrors.add(detailError);
				}
			} else {
				log.error("no node found for xpath: " + xpath + " in resource " + resource.getName());
			}
		}
	}

	private NodeList getNodesWithPositionForXpath(Resource resource, String xpath) {
		try {
			InputStream is = null;
			if (null != resource.getBytes()) {
				is = new ByteArrayInputStream(resource.getBytes());
			} else if (null != resource.getCachedFile()) {
				is = new FileInputStream(resource.getCachedFile());
			}
			if (null != is) {
				Document doc = PositionalXMLReader.readXML(is);
				XPathProcessor process = new XPathProcessor(doc);
				NodeList nodes = process.getNodes(xpath);
				is.close();
				return nodes;
			} else {
				log.error("Neither bytes nor cached file is available for resource " + resource.getName());
				return null;
			}
		} catch (IOException | SAXException e) {
			log.error("cannot get node with xpath " + xpath, e);
		}
		return null;
	}

	private boolean findParameter(Params params, String paramName) {
		return null != params && findParameter(params.getParam(), paramName);
	}

	private boolean findParameter(List<? extends Param> paramList, String paramName) {
		if (null == paramList) {
			return false;
		}
		for (Param param : paramList) {
			if (param.getName().equals(paramName)) {
				return true;
			}
		}
		return false;
	}

	public Set<String> getErrors() {
		return errors;
	}

	public List<ConfigValidationError> getDetaildErrors() {
		return detailedErrors;
	}

	public Set<String> getWarnings() {
		return warnings;
	}

}
