/*
 * Copyright 2011-2017 the original author or authors.
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

import static org.appng.api.Scope.REQUEST;
import static org.appng.api.Scope.SESSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.DataContainer;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.MessageParam;
import org.appng.api.Options;
import org.appng.api.ParameterSupport;
import org.appng.api.Path;
import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.Platform;
import org.appng.api.ProcessingException;
import org.appng.api.Session;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.BeanOption;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkmode;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Navigation;
import org.appng.xml.platform.NavigationItem;
import org.appng.xml.platform.OptionGroup;
import org.appng.xml.platform.PageConfig;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.Template;
import org.appng.xml.platform.ValidationGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.util.ClassUtils;

/**
 * 
 * Utility class offering methods for proper initialization of {@link Linkpanel}s, {@link Link}s, {@link Navigation}/
 * {@link NavigationItem}s, {@link BeanOption}s, {@link Param}s etc.
 * 
 * @author Matthias Müller
 * 
 */
public class ElementHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElementHelper.class);

	private static final String SLASH = "/";

	public static final String INTERNAL_ERROR = "internal.error";

	private Application application;

	private Site site;

	private ExpressionEvaluator expressionEvaluator;

	public ElementHelper(Site site, Application application) {
		this.application = application;
		this.site = site;
	}

	public ElementHelper(Site site, Application application, ExpressionEvaluator expressionEvaluator) {
		this.application = application;
		this.site = site;
		this.expressionEvaluator = expressionEvaluator;
	}

	void initLinkpanel(ApplicationRequest applicationRequest, Path pathInfo, DataConfig dsConfig,
			ParameterSupport parameterSupport) {
		List<Linkpanel> linkpanel = dsConfig.getLinkpanel();
		if (null != linkpanel) {
			List<Linkpanel> out = null;
			if (null != linkpanel) {
				out = new ArrayList<Linkpanel>();
				for (Linkpanel panel : linkpanel) {
					Linkpanel outPanel = initLinkpanel(applicationRequest, pathInfo, panel, parameterSupport);
					if (null != outPanel) {
						out.add(outPanel);
					}
				}
				linkpanel.clear();
				linkpanel.addAll(out);
			}
		}
	}

	private Linkpanel initLinkpanel(ApplicationRequest request, Path pathInfo, Linkpanel panel,
			ParameterSupport parameterSupport) {
		Linkpanel outPanel = null;
		PermissionProcessor permissionProcessor = request.getPermissionProcessor();
		if (null != panel && permissionProcessor.hasPermissions(new PermissionOwner(panel))) {
			outPanel = new Linkpanel();
			String panelId = panel.getId();
			outPanel.setId(panelId);
			outPanel.setLocation(panel.getLocation());
			List<Link> links = panel.getLinks();
			int linkCount = 1;
			String servicePath = pathInfo.getServicePath();
			String guiPath = pathInfo.getGuiPath();
			for (Link link : links) {
				boolean hasPermission = request.getPermissionProcessor().hasPermissions(new PermissionOwner(link));
				if (hasPermission) {
					Condition condition = link.getCondition();
					ExpressionEvaluator linkExpressionEvaluator = parameterSupport.getExpressionEvaluator();
					boolean doInclude = expressionMatchesOrContainsCurrent(condition, linkExpressionEvaluator);
					boolean showDisabled = Boolean.TRUE.equals(link.isShowDisabled());

					if (doInclude || showDisabled) {
						link.setCondition(condition);
						if (link.getId() == null) {
							link.setId(panelId + "[" + linkCount + "]");
						}
						request.setLabel(link.getLabel());
						request.setLabel(link.getConfirmation());
						outPanel.getLinks().add(link);
						String currentTarget = link.getTarget();
						String newTarget = parameterSupport.replaceParameters(currentTarget);
						if (link.getMode().equals(Linkmode.WEBSERVICE)) {
							newTarget = servicePath + SLASH + site.getName() + SLASH + application.getName() + SLASH
									+ Platform.SERVICE_TYPE_WEBSERVICE + SLASH + newTarget;
						}
						String proposedPath = guiPath + SLASH + site.getName() + SLASH + application.getName()
								+ newTarget;
						if (null != pathInfo && pathInfo.isPathSelected(proposedPath)) {
							link.setActive(Boolean.TRUE.toString());
						}
						link.setTarget(newTarget);
					}
				}
				linkCount++;
			}
		}
		return outPanel;
	}

	private boolean expressionMatchesOrContainsCurrent(Condition condition,
			ExpressionEvaluator conditionExpressionEvaluator) {
		if (null != condition) {
			String expression = condition.getExpression();
			if (StringUtils.isNotBlank(expression) && !expression.contains(AdapterBase.CURRENT)) {
				return conditionExpressionEvaluator.evaluate(expression);
			}
		}
		return true;
	}

	public void initNavigation(ApplicationRequest applicationRequest, Path pathInfo, PageConfig pageConfig) {
		ParameterSupport parameterSupport = applicationRequest.getParameterSupportDollar();
		Linkpanel pageLinks = initLinkpanel(applicationRequest, pathInfo, pageConfig.getLinkpanel(), parameterSupport);

		Linkpanel navigation = applicationRequest.getApplicationConfig().getApplicationRootConfig().getNavigation();
		if (null != navigation) {
			navigation = initLinkpanel(applicationRequest, pathInfo, navigation, parameterSupport);
			if (null != pageLinks) {
				List<Link> links = navigation.getLinks();
				for (Link link : links) {
					pageLinks.getLinks().add(link);
				}
				pageConfig.setLinkpanel(pageLinks);
			} else {
				pageConfig.setLinkpanel(navigation);
			}
		}
	}

	/**
	 * Builds {@link Options} from the given list of {@link BeanOption}s, without evaluation of parameter placeholders.
	 * 
	 * @param beanOptions
	 *            some {@link BeanOption}s
	 * @return the {@link Options}
	 * @see #initOptions(List)
	 */
	Options getOptions(List<BeanOption> beanOptions) {
		OptionsImpl options = new OptionsImpl();
		if (null != beanOptions) {
			for (BeanOption beanOption : beanOptions) {
				OptionImpl opt = new OptionImpl(beanOption.getName());
				Map<QName, String> attributes = beanOption.getOtherAttributes();
				for (Entry<QName, String> entry : attributes.entrySet()) {
					String optionName = entry.getKey().getLocalPart();
					opt.addAttribute(optionName, entry.getValue());
				}
				options.addOption(opt);
			}
		}
		return options;
	}

	/**
	 * Performs parameter-replacement for the given {@link BeanOption}s
	 * 
	 * @param beanOptions
	 *            some {@link BeanOption}s
	 */
	void initOptions(List<BeanOption> beanOptions) {
		if (null != beanOptions) {
			for (BeanOption beanOption : beanOptions) {
				Map<QName, String> attributes = beanOption.getOtherAttributes();
				for (Entry<QName, String> entry : attributes.entrySet()) {
					String value = expressionEvaluator.evaluate(entry.getValue(), String.class);
					entry.setValue(value);
				}
			}
		}
	}

	void setSelectionTitles(Data data, ApplicationRequest applicationRequest) {
		setSelectionTitles(data.getSelections(), applicationRequest);
		for (SelectionGroup group : data.getSelectionGroups()) {
			setSelectionTitles(group.getSelections(), applicationRequest);
		}
	}

	private void setSelectionTitles(List<Selection> selections, ApplicationRequest applicationRequest) {
		for (Selection selection : selections) {
			applicationRequest.setLabel(selection.getTitle());
			for (OptionGroup optionGroup : selection.getOptionGroups()) {
				applicationRequest.setLabel(optionGroup.getLabel());
			}
		}
	}

	void processConfig(ApplicationConfigProvider applicationConfigProvider, ApplicationRequest applicationRequest,
			DataConfig config, Map<String, String> parameters) {
		MetaData metaData = getFilteredMetaData(applicationRequest, config.getMetaData(), false);
		config.setMetaData(metaData);
		// DO NOT evaluate hidden and readOnly here!!
		Path path = applicationRequest.getEnvironment().getAttribute(REQUEST, EnvironmentKeys.PATH_INFO);
		initLinkpanel(applicationRequest, path, config, new DollarParameterSupport(parameters));
		addTemplates(applicationConfigProvider, config);
	}

	MetaData getFilteredMetaData(ApplicationRequest request, MetaData metaData, boolean write) {
		MetaData result = new MetaData();
		if (null != metaData) {
			result.setBinding(metaData.getBinding());
			result.setResultSelector(metaData.getResultSelector());
			result.setBindClass(metaData.getBindClass());
			result.setValidation(metaData.getValidation());
			List<FieldDef> fieldDefinitions = metaData.getFields();
			List<FieldDef> fields = filterFieldDefinitions(request, fieldDefinitions, write);
			result.getFields().addAll(fields);
		}
		return result;
	}

	/**
	 * removes those {@link FieldDef}s from the given fieldDefinitions for whom the user has no permissions and returns
	 * a new list containing only the allowed {@link FieldDef}s
	 */
	private List<FieldDef> filterFieldDefinitions(ApplicationRequest request, List<FieldDef> fieldDefinitions,
			boolean write) {
		List<FieldDef> fields = new ArrayList<FieldDef>();
		PermissionProcessor permissionProcessor = request.getPermissionProcessor();
		if (null != fieldDefinitions) {
			for (FieldDef fieldDef : fieldDefinitions) {
				boolean hasPermission = false;
				if (write) {
					hasPermission = permissionProcessor.hasWritePermission(fieldDef);
				} else {
					hasPermission = permissionProcessor.hasReadPermission(fieldDef);
				}
				if (hasPermission) {
					if (!write) {
						request.setLabel(fieldDef.getLabel());
					}
					Condition condition = fieldDef.getCondition();
					boolean isValid = expressionMatchesOrContainsCurrent(condition, expressionEvaluator);
					if (isValid) {
						String format = fieldDef.getFormat();
						if (null != format) {
							format = expressionEvaluator.evaluate(format, String.class);
							fieldDef.setFormat(format);
						}
						filterFieldDefinitions(request, fieldDef.getFields(), write);
						fields.add(fieldDef);
					}
				}
			}
		}
		return fields;
	}

	void addTemplates(ApplicationConfigProvider applicationConfigProvider, Config config) {
		List<Template> templates = config.getTemplates();
		if (null != templates) {
			applicationConfigProvider.getApplicationRootConfig().getConfig().getTemplates().addAll(templates);
		}
	}

	Map<String, String> initializeParameters(String reference, ApplicationRequest applicationRequest,
			ParameterSupport parameterSupport, Params referenceParams, Params executionParams)
			throws ProcessingException {
		Map<String, String> executionParameters = new HashMap<String, String>();
		Map<String, String> referenceParameters = new HashMap<String, String>();
		if (null != referenceParams) {
			for (Param p : referenceParams.getParam()) {
				String newValue = parameterSupport.replaceParameters(p.getValue());
				if (StringUtils.isEmpty(newValue)) {
					if (StringUtils.isNotEmpty(p.getDefault())) {
						newValue = p.getDefault();
					}
				}
				p.setValue(newValue);
				if (null != newValue) {
					referenceParameters.put(p.getName(), newValue);
				}
			}

			if (null != executionParams) {
				for (Param p : executionParams.getParam()) {
					String value = p.getValue();
					if (StringUtils.isEmpty(value)) {
						value = referenceParameters.get(p.getName());
						if (StringUtils.isEmpty(value) && StringUtils.isNotEmpty(p.getDefault())) {
							value = p.getDefault();
						}
					}
					p.setValue(value);
					if (null != value) {
						executionParameters.put(p.getName(), value);
					}
				}
			}
		}
		if (applicationRequest.isPost()) {
			Map<String, List<String>> parametersList = applicationRequest.getParametersList();
			for (String param : parametersList.keySet()) {
				String postParam = StringUtils.join(parametersList.get(param), "|");
				String existingValue = executionParameters.get(param);
				if (null == existingValue) {
					executionParameters.put(param, postParam);
				} else {
					if (!existingValue.equals(postParam)) {
						String message = "the parameter '" + param
								+ "' is ambiguous, since it's a execution parameter for " + reference + " (value: '"
								+ existingValue + "') and also POST-parameter (value: '" + postParam
								+ "'). Avoid such overlapping parameters!";
						LOGGER.warn(message);
						// TODO APPNG-442
						// throwing ProcessingException may be too aggressive here
						// throw new ProcessingException(message, null);
					}
				}
			}
		}
		this.expressionEvaluator = new ExpressionEvaluator(executionParameters);
		expressionEvaluator.setVariable(ApplicationRequest.I18N_VAR, new I18n(applicationRequest));
		return executionParameters;
	}

	static Messages addMessages(Environment environment, Messages messages) {
		Messages messagesFromSession = environment.getAttribute(SESSION, Session.Environment.MESSAGES);
		if (messages.getMessageList().size() > 0) {
			if (null == messagesFromSession) {
				messagesFromSession = new Messages();
			}
			messagesFromSession.getMessageList().addAll(messages.getMessageList());
			environment.setAttribute(SESSION, Session.Environment.MESSAGES, messagesFromSession);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("messages : {}", messagesFromSession.getMessageList());
			}
		}
		return messagesFromSession;
	}

	public Messages removeMessages(Environment environment) {
		Messages messages = environment.removeAttribute(SESSION, Session.Environment.MESSAGES);
		if (LOGGER.isDebugEnabled() && null != messages) {
			LOGGER.debug("removed messages : {}", messages.getMessageList());
		}
		return messages;
	}

	public Messages getMessages(Environment environment) {
		Messages messages = environment.getAttribute(SESSION, Session.Environment.MESSAGES);
		if (LOGGER.isDebugEnabled() && null != messages) {
			LOGGER.debug("retrieved messages : {}", messages.getMessageList());
		}
		return messages;
	}

	public boolean hasMessages(Environment environment) {
		return null != getMessages(environment);
	}

	ExpressionEvaluator getExpressionEvaluator() {
		return expressionEvaluator;
	}

	boolean conditionMatches(Condition condition) {
		return conditionMatches(getExpressionEvaluator(), condition);
	}

	boolean conditionMatches(ExpressionEvaluator expressionEvaluator, Condition condition) {
		return null == condition || StringUtils.isBlank(condition.getExpression())
				|| expressionEvaluator.evaluate(condition.getExpression());
	}

	public void processDataContainer(org.appng.api.Request applicationRequest, DataContainer container,
			String callerName) throws ClassNotFoundException, ProcessingException {

		Data data = container.getWrappedData();
		FieldProcessor fieldProcessor = container.getFieldProcessor();

		ResultServiceImpl resultService = new ResultServiceImpl(getExpressionEvaluator());
		resultService.setConversionService(applicationRequest);
		resultService.setEnvironment(applicationRequest.getEnvironment());
		MessageSource messageSource = this.application.getBean(MessageSource.class);
		resultService.setMessageSource(messageSource);
		resultService.afterPropertiesSet();
		if (container.isSingleResult()) {
			Object item = container.getItem();
			verifyItemType(fieldProcessor.getMetaData(), item, callerName);
			Result result = resultService.getResult(fieldProcessor, item);
			data.setResult(result);
		} else {
			Resultset resultset = null;
			Collection<?> items = null;
			if (null != container.getPage()) {
				Page<?> page = container.getPage();
				resultset = resultService.getResultset(fieldProcessor, page);
				items = page.getContent();
			} else if (null != container.getItems()) {
				resultset = resultService.getResultset(fieldProcessor, container.getItems());
				items = container.getItems();
			} else {
				throw new ProcessingException("DataContainer must either have a page or a Collection of items",
						fieldProcessor);
			}
			if (!items.isEmpty()) {
				verifyItemType(fieldProcessor.getMetaData(), items.iterator().next(), callerName);
			}
			data.setResultset(resultset);
		}
	}

	private void verifyItemType(MetaData metaData, Object item, String dsId) throws ClassNotFoundException {
		if (null == item) {
			throw new IllegalArgumentException("datasource " + dsId + " did not return an item!");
		}
		Class<?> bindClass = ClassUtils.forName(metaData.getBindClass(), site.getSiteClassLoader());
		Class<? extends Object> itemClass = item.getClass();
		if (!bindClass.isAssignableFrom(itemClass)) {
			String message = "the object of type '" + itemClass.getName() + "' returned by '" + dsId
					+ "' is not of the desired type '" + metaData.getBindClass() + "' as defined in the meta-data!";
			throw new IllegalArgumentException(message);
		}
	}

	public boolean isMessageParam(Object o) {
		return null != o && (o instanceof MessageParam) && ((MessageParam) o).getMessageKey() != null;
	}

	public Class<?>[] getValidationGroups(MetaData metaData, Object bindObject) {
		List<Class<?>> groups = new ArrayList<Class<?>>();
		ValidationGroups validationGroups = metaData.getValidation();
		if (null != validationGroups) {
			getExpressionEvaluator().setVariable(AdapterBase.CURRENT, bindObject);
			for (ValidationGroups.Group group : new ArrayList<ValidationGroups.Group>(validationGroups.getGroups())) {
				String expression = group.getCondition();
				Condition condition = new Condition();
				condition.setExpression(expression);
				if (StringUtils.isBlank(expression) || conditionMatches(condition)) {
					try {
						groups.add(site.getSiteClassLoader().loadClass(group.getClazz()));
						group.setCondition(null);
					} catch (ClassNotFoundException e) {
						LOGGER.error("validation group {} not found!", group.getClazz());
					}
				} else {
					validationGroups.getGroups().remove(group);
				}
			}
		}
		return groups.toArray(new Class<?>[groups.size()]);
	}

}
