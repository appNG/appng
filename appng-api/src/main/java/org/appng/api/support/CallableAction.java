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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.appng.api.ActionProvider;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.FormValidator;
import org.appng.api.Option;
import org.appng.api.Options;
import org.appng.api.ParameterSupport;
import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ActionRef;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Message;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.OptionGroup;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.PageReference;
import org.appng.xml.platform.Permissions;
import org.appng.xml.platform.Section;
import org.appng.xml.platform.SectionelementDef;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.UserData;
import org.appng.xml.platform.UserInputField;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * A {@code CallableAction} is responsible for preparing and performing an {@link Action}, based on a given
 * {@link ActionRef} which is part of a {@link PageDefinition}'s {@link SectionelementDef}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class CallableAction {

	private ApplicationRequest applicationRequest;
	private Site site;
	private Application application;
	private boolean include = false;
	private boolean execute = false;
	private boolean errors = false;
	private ElementHelper elementHelper;
	private String onSuccess;
	private ActionRef actionRef;
	private ParameterSupport actionParamSupport;
	private Action action;

	/**
	 * Creates a new {@code CallableAction} and also determines the return values for
	 * <ul>
	 * <li>{@link #doInclude()}
	 * <li>{@link #doExecute()}
	 * <li>{@link #getOnSuccess()}
	 * </ul>
	 * , based on {@link Permissions} and {@link Condition}s.
	 * 
	 * @param site
	 *            the current {@link Site}
	 * @param application
	 *            the current {@link Application}
	 * @param applicationRequest
	 *            the current {@link ApplicationRequest}
	 * @param actionRef
	 *            the {@link ActionRef} as given in the {@link SectionelementDef} of a {@link PageDefinition}.
	 * @throws ProcessingException
	 *             if an error occurs while assembling the {@code CallableAction}
	 */
	public CallableAction(Site site, Application application, ApplicationRequest applicationRequest,
			ActionRef actionRef) throws ProcessingException {
		this.site = site;
		this.application = application;
		this.applicationRequest = applicationRequest;
		this.actionRef = actionRef;

		String eventId = actionRef.getEventId();
		Event event = applicationRequest.getApplicationConfig().getEvent(eventId);
		if (null == event) {
			throw new ProcessingException("no such event: " + eventId, new FieldProcessorImpl(eventId));
		}
		PermissionProcessor permissionProcessor = applicationRequest.getPermissionProcessor();
		if (permissionProcessor.hasPermissions(new PermissionOwner(event))) {
			if (permissionProcessor.hasPermissions(new PermissionOwner(actionRef))) {
				initializeAction(event);
			} else {
				LOGGER.debug("no permission for actionRef '{}' of event '{}'", actionRef.getId(), event.getId());
			}
		} else {
			LOGGER.debug("no permission for event '{}'", eventId);

		}
	}

	// for testing
	protected CallableAction(Site site, ApplicationRequest applicationRequest, ElementHelper elementHelper) {
		this.site = site;
		this.applicationRequest = applicationRequest;
		this.elementHelper = elementHelper;
	}

	private void initializeAction(Event event) throws ProcessingException {
		String actionId = actionRef.getId();
		String eventId = event.getId();
		this.action = applicationRequest.getApplicationConfig().getAction(eventId, actionId);
		if (null == action) {
			throw new ProcessingException("no such action: " + actionId, new FieldProcessorImpl(actionId));
		}
		if (applicationRequest.getPermissionProcessor().hasPermissions(new PermissionOwner(action))) {
			ExpressionEvaluator pageExpressionEvaluator = applicationRequest.getExpressionEvaluator();
			if (actionRef.getClientValidation() != null) {
				boolean clientValidation = pageExpressionEvaluator.getBoolean(actionRef.getClientValidation());
				this.action.setClientValidation(String.valueOf(clientValidation));
			}
			this.elementHelper = new ElementHelper(site, application);
			DataConfig config = getAction().getConfig();
			getAction().setEventId(eventId);
			Map<String, String> actionParameters = elementHelper.initializeParameters(
					"action '" + actionRef.getId() + "' (" + actionRef.getEventId() + ")", applicationRequest,
					applicationRequest.getParameterSupportDollar(), actionRef.getParams(), config.getParams());

			LOGGER.trace("parameters for action '{}' of event '{}': {}", actionId, eventId, actionParameters);

			actionParamSupport = new DollarParameterSupport(actionParameters);
			Condition includeCondition = actionRef.getCondition();
			this.include = elementHelper.conditionMatches(pageExpressionEvaluator, includeCondition);
			if (include) {
				elementHelper.processConfig(applicationRequest.getApplicationConfig(), applicationRequest,
						action.getConfig(), actionParameters);

				elementHelper.addTemplates(applicationRequest.getApplicationConfig(), event.getConfig());
				LOGGER.debug("including  action '{}' of event '{}'", actionId, eventId);
			} else {
				LOGGER.debug("include condition for action '{}' of event '{}' did not match - {}", actionId, eventId,
						includeCondition.getExpression());
			}

			Condition executeCondition = action.getCondition();
			this.execute = elementHelper.conditionMatches(executeCondition);
			if (!execute) {
				LOGGER.debug("execute condition for action '{}' of event '{}' did not match - {}", actionId, eventId,
						executeCondition.getExpression());
			}
			if (include || execute) {
				Bean bean = getAction().getBean();
				if (null != bean) {
					elementHelper.initOptions(bean.getOptions());
				}
				setOnSuccess();
			}
		} else {
			LOGGER.debug("no permission(s) for action '{}' of event '{}'", actionId, eventId);
		}
	}

	public Action getAction() {
		return action;
	}

	protected boolean retrieveData(boolean setBeanNull) throws ProcessingException {
		boolean dataOk = true;
		ParameterSupport fieldParams = null;
		DatasourceRef datasourceRef = action.getDatasource();
		if (null != datasourceRef) {
			CallableDataSource datasourceElement = new CallableDataSource(site, application, applicationRequest,
					actionParamSupport, datasourceRef);
			if (datasourceElement.doInclude()) {

				List<Message> before = new ArrayList<>();
				Environment environment = applicationRequest.getEnvironment();
				if (elementHelper.hasMessages(environment)) {
					before.addAll(elementHelper.getMessages(environment).getMessageList());
				}

				Data data = datasourceElement.perform(null, setBeanNull, true);
				action.setData(data);
				DataConfig dsConfig = datasourceElement.getDatasource().getConfig();
				elementHelper.addTemplates(applicationRequest.getApplicationConfig(), dsConfig);
				action.getConfig().setMetaData(dsConfig.getMetaData());

				List<Message> after = new ArrayList<>();
				if (elementHelper.hasMessages(environment)) {
					after.addAll(elementHelper.getMessages(environment).getMessageList());
				}

				@SuppressWarnings("unchecked")
				Collection<Message> addedMessages = CollectionUtils.disjunction(before, after);

				if (!addedMessages.isEmpty()) {
					for (Message message : addedMessages) {
						dataOk &= !MessageType.ERROR.equals(message.getClazz());
					}
					if (!dataOk) {
						Messages messages = elementHelper.getMessages(environment);
						messages.getMessageList().removeAll(addedMessages);
						Messages actionMessages = new Messages();
						actionMessages.getMessageList().addAll(addedMessages);
						getAction().setMessages(actionMessages);
					}
				}

				if (null != data && null != data.getResult()) {
					Map<String, String> fieldValues = new HashMap<>();
					for (Datafield datafield : data.getResult().getFields()) {
						fieldValues.put(datafield.getName(), datafield.getValue());
					}
					fieldParams = new HashParameterSupport(fieldValues);
				}
			}
		}
		applicationRequest.setLabels(action.getConfig(), fieldParams);
		return dataOk;
	}

	private void setOnSuccess() {
		onSuccess = actionRef.getOnSuccess();
		if (!StringUtils.isBlank(onSuccess)) {
			if (!onSuccess.startsWith("/")) {
				onSuccess = "/" + onSuccess;
			}
			ParameterSupport pageParamSupport = applicationRequest.getParameterSupportDollar();
			onSuccess = application.getName() + pageParamSupport.replaceParameters(onSuccess);
			actionRef.setOnSuccess(onSuccess);
			getAction().setOnSuccess(onSuccess);
		}
	}

	/**
	 * Performs this {@link CallableAction}.<br/>
	 * If the {@link Action} is actually included and/or executed depends on the returns values of {@link #doInclude()}
	 * and {@link #doExecute()}. If the {@link Action} is executed and a forward-path exists, a redirect is send via
	 * {@link Site#sendRedirect(Environment, String)}.
	 * 
	 * @return a {@link FieldProcessor}, only non-{@code null} if the {@link Action} has been executed successfully
	 * @throws ProcessingException
	 *             if an error occurred while performing
	 * @see #doInclude()
	 * @see #doExecute()
	 * @see #doForward()
	 * @see #getOnSuccess()
	 */
	public FieldProcessor perform() throws ProcessingException {
		return perform(false);
	}

	/**
	 * Performs this {@link CallableAction}.<br/>
	 * If the {@link Action} is actually included and/or executed depends on the returns values of {@link #doInclude()}
	 * and {@link #doExecute()}. If the {@link Action} is executed and a forward-path exists, a redirect is send via
	 * {@link Site#sendRedirect(Environment, String)}.
	 * 
	 * @param isSectionHidden
	 *            whether this action is part of a hidden {@link Section}, meaning no {@link Messages} should be set for
	 *            the action.
	 * 
	 * @return a {@link FieldProcessor}, only non-{@code null} if the {@link Action} has been executed successfully
	 * @throws ProcessingException
	 *             if an error occurred while performing
	 * @see #doInclude()
	 * @see #doExecute()
	 * @see #doForward()
	 * @see #getOnSuccess()
	 */
	public FieldProcessor perform(boolean isSectionHidden) throws ProcessingException {
		FieldProcessor fp = null;
		if (doExecute()) {
			execute = retrieveData(false);
			if (doExecute()) {
				fp = execute();
				if (doForward() || forceForward()) {
					String outputPrefix = elementHelper.getOutputPrefix(applicationRequest.getEnvironment());
					StringBuilder target = new StringBuilder();
					if (null != outputPrefix) {
						target.append(outputPrefix);
					}
					target.append(getOnSuccess());
					site.sendRedirect(applicationRequest.getEnvironment(), target.toString());
					getAction().setOnSuccess(target.toString());
					applicationRequest.setRedirectTarget(target.toString());
				} else if (!isSectionHidden) {
					Messages messages = elementHelper.removeMessages(applicationRequest.getEnvironment());
					getAction().setMessages(messages);
				}
			}
		}
		if (doInclude()) {
			if (!doExecute() || !doForward()) {
				retrieveData(false);
				handleSelections();
			}
		}
		return fp;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private FieldProcessor execute() throws ProcessingException {

		Bean bean = getAction().getBean();
		FieldProcessor fieldProcessor = null;
		if (null != bean) {
			MetaData metaData = elementHelper.getFilteredMetaData(applicationRequest, action.getConfig().getMetaData(),
					true);
			try {
				String beanId = bean.getId();
				String actionId = actionRef.getId();
				LOGGER.trace("retrieving action '{}'", beanId);
				final ActionProvider actionProvider = this.application.getBean(beanId, ActionProvider.class);
				LOGGER.trace("action '{}' is of Type '{}'", beanId, actionProvider.getClass().getName());

				fieldProcessor = new FieldProcessorImpl(actionId, metaData);
				fieldProcessor.addLinkPanels(action.getConfig().getLinkpanel());
				final Environment env = applicationRequest.getEnvironment();

				final Options options = elementHelper.getOptions(bean.getOptions());

				LOGGER.trace("options for action '{}' of event '{}': {}", actionId, actionRef.getEventId(), options);

				Object bindObject = getBindObject(fieldProcessor);
				validateBindObject(fieldProcessor, actionProvider, options, bindObject);
				if (!fieldProcessor.hasErrors()) {
					final BeanWrapper bindObjectWrapper;
					if (null != bindObject) {
						bindObjectWrapper = new BeanWrapperImpl(bindObject);
					} else {
						bindObjectWrapper = new BeanWrapperImpl();
					}
					Boolean async = applicationRequest.getExpressionEvaluator().getBoolean(actionRef.getAsync());
					action.setAsync(String.valueOf(async));
					if (Boolean.TRUE.equals(async)) {
						LOGGER.debug("validation OK, asynchronously calling bean '{}' of application '{}'", beanId,
								application.getName());
						final String actionTitle = getAction().getConfig().getTitle().getValue();
						final FieldProcessor innerFieldProcessor = new FieldProcessorImpl(actionId, metaData);
						innerFieldProcessor.addLinkPanels(action.getConfig().getLinkpanel());
						final Locale locale = applicationRequest.getEnvironment().getLocale();
						String pattern = String.format("%s-%s-async-%s", site.getName(), application.getName(), "%d");
						ThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern(pattern).build();
						ExecutorService executor = Executors.newFixedThreadPool(1, threadFactory);
						FutureTask<Void> futureTask = new FutureTask<Void>(new Callable<Void>() {
							public Void call() throws Exception {
								try {
									Object innerBindObject = bindObjectWrapper.getWrappedInstance();
									actionProvider.perform(site, application, env, options, applicationRequest,
											innerBindObject, innerFieldProcessor);
									String message = application.getMessage(locale, "backgroundTaskFinished",
											actionTitle);
									innerFieldProcessor.addOkMessage(message);
									ElementHelper.addMessages(env, innerFieldProcessor.getMessages());
									LOGGER.debug("Background task '{}' finished.", actionTitle);
								} catch (Exception e) {
									String id = String.valueOf(e.hashCode());
									LOGGER.error(String.format("error while executing background task %s, ID: %s",
											actionTitle, id), e);
									String backgroundTaskError = application.getMessage(locale, "backgroundTaskError",
											actionTitle, id);
									innerFieldProcessor.addErrorMessage(backgroundTaskError);
									ElementHelper.addMessages(env, innerFieldProcessor.getMessages());
									throw e;
								}
								return null;
							}
						});
						executor.execute(futureTask);
						executor.shutdown();
						String message = application.getMessage(locale, "backgroundTaskStarted", actionTitle);
						fieldProcessor.addOkMessage(message);
						LOGGER.debug("Background task '{}' started.", actionTitle);
					} else {
						LOGGER.debug("validation OK, calling bean '{}' of application '{}'", beanId,
								application.getName());
						actionProvider.perform(site, application, env, options, applicationRequest, bindObject,
								fieldProcessor);
					}
				}
				errors = fieldProcessor.hasErrors();
				ElementHelper.addMessages(env, fieldProcessor.getMessages());
				if (errors) {
					LOGGER.debug("validation returned errors");
					addUserdata(metaData.getFields());
					handleSelections();
				}
			} catch (Exception e) {
				String id = String.valueOf(e.hashCode());
				if (!elementHelper.isMessageParam(e) && null != fieldProcessor) {
					fieldProcessor.clearMessages();
					String message = applicationRequest.getMessage(ElementHelper.INTERNAL_ERROR, id);
					fieldProcessor.addErrorMessage(message);
				}
				throw new ProcessingException(String.format("error performing action '%s' of event '%s', ID: %s",
						action.getId(), actionRef.getEventId(), id), e, fieldProcessor);
			}
		}
		return fieldProcessor;
	}

	/**
	 * Creates, fills and returns a new bindobject.
	 * 
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} to use
	 * @return a new bindobject
	 * @throws BusinessException
	 *             if
	 *             {@link ApplicationRequest#getBindObject(FieldProcessor, org.appng.forms.RequestContainer, ClassLoader)}
	 *             throws such an exception
	 * @see ApplicationRequest#getBindObject(FieldProcessor, org.appng.forms.RequestContainer, ClassLoader)
	 */
	protected Object getBindObject(FieldProcessor fieldProcessor) throws BusinessException {
		Object bindObject = null;
		if (fieldProcessor.getMetaData().getBindClass() != null) {
			bindObject = applicationRequest.getBindObject(fieldProcessor, applicationRequest,
					site.getSiteClassLoader());
		} else {
			LOGGER.debug("no bindclass given for action '{}'", getAction().getId());
		}
		return bindObject;
	}

	private void validateBindObject(FieldProcessor fieldProcessor, final ActionProvider<?> actionProvider,
			final Options options, Object bindObject) {
		if (null != bindObject) {
			applicationRequest.validateBean(bindObject, fieldProcessor,
					elementHelper.getValidationGroups(fieldProcessor.getMetaData(), bindObject));

			Environment env = applicationRequest.getEnvironment();
			if (bindObject instanceof FormValidator) {
				((FormValidator) bindObject).validate(site, application, env, options, applicationRequest,
						fieldProcessor);
			}
			if (actionProvider instanceof FormValidator) {
				((FormValidator) actionProvider).validate(site, application, env, options, applicationRequest,
						fieldProcessor);
			}
		}
	}

	/**
	 * Adds the {@link UserData}-object to the given {@link Action}. The {@link UserData} contains the form-input made
	 * by the user.
	 * 
	 * @see #handleSelections()
	 * @param action
	 * @param request
	 * @param writeableFields
	 *            a list of writeable {@link FieldDef}initions
	 */
	private void addUserdata(List<FieldDef> writeableFields) {
		MetaData metaData = action.getConfig().getMetaData();
		if (null != metaData) {
			action.setUserdata(new UserData());
			addFieldsToUserdata(writeableFields);
		}
	}

	private void addFieldsToUserdata(List<FieldDef> fields) {
		for (FieldDef fieldDef : fields) {
			// do NOT evaluate expressions here!
			boolean isWriteable = !Boolean.TRUE.toString().equalsIgnoreCase(fieldDef.getReadonly());
			if (isWriteable) {
				String binding = fieldDef.getBinding();
				FieldType type = fieldDef.getType();
				if (!FieldType.PASSWORD.equals(type)) {
					List<String> parameterList = applicationRequest.getParameterList(binding);
					if (!parameterList.isEmpty()) {
						for (String value : parameterList) {
							UserInputField f = new UserInputField();
							f.setName(binding);
							f.setContent(value);
							action.getUserdata().getInput().add(f);
						}
					}
				}
				List<FieldDef> childFields = fieldDef.getFields();
				addFieldsToUserdata(childFields);
			}
		}
	}

	/**
	 * Checks whether or not to include this {@code CallableAction} on the {@link PageReference}.
	 * 
	 * @return {@code true} if this {@code CallableAction} should be included, {@code false} otherwise
	 */
	public boolean doInclude() {
		return include;
	}

	/**
	 * Checks whether or not this {@code CallableAction} should execute the {@link ActionProvider} provided by the
	 * referenced {@link Action}.
	 * 
	 * @return {@code true} if this {@code CallableAction} should be executed, {@code false} otherwise
	 */
	public boolean doExecute() {
		return execute;
	}

	/**
	 * Checks whether some errors occurred while performing the {@link Action}.
	 * 
	 * @return {@code true} if some errors occurred, {@code false} otherwise
	 */
	public boolean hasErrors() {
		return errors;
	}

	/**
	 * Returns the forward-path that was defined by the {@link ActionRef} this {@link CallableAction} was build from.
	 * 
	 * @return the forward-path, may be {@code null}
	 */
	public String getOnSuccess() {
		return onSuccess;
	}

	/**
	 * Checks whether this {@link CallableAction} causes a page-forward, which is the case when {@link #hasErrors()}
	 * returns {@code false} and {@link #getOnSuccess()} a non-{@code null} value
	 * 
	 * @return {@code true} if a page-forward is caused, {@code false} otherwise
	 */
	public boolean doForward() {
		return !hasErrors() && null != onSuccess;
	}

	private boolean forceForward() {
		return null != onSuccess && actionRef.isForceForward();
	}

	/**
	 * selects the {@link Option}s of a {@link Selection} based on the users form-inputs, which are stored in the
	 * {@link Action}s {@link UserData}<br/>
	 * Don't really like this solution...open for improvement
	 * 
	 * 
	 * @see #addUserdata(List)
	 * @param action
	 */
	// XXX MM this is the root of all evil
	private void handleSelections() {
		if (null != action.getData()) {
			Map<String, Selection> selectionMap = new HashMap<>();
			for (Selection selection : action.getData().getSelections()) {
				selectionMap.put(selection.getId(), selection);
			}

			UserData userdata = action.getUserdata();
			if (null != userdata) {

				Map<String, List<String>> userinput = new HashMap<>();
				for (UserInputField userInputField : userdata.getInput()) {
					String name = userInputField.getName();
					if (!userinput.containsKey(name)) {
						userinput.put(name, new ArrayList<>());
					}
					userinput.get(name).add(userInputField.getContent());
				}
				List<FieldDef> fields = action.getConfig().getMetaData().getFields();
				for (FieldDef fieldDef : fields) {
					Selection selection = selectionMap.get(fieldDef.getName());
					String binding = fieldDef.getBinding();
					List<String> userInputs = userinput.get(binding);
					if (null != selection && null != userInputs) {
						deselectOptions(selection.getOptions());
						for (OptionGroup optionGroup : selection.getOptionGroups()) {
							deselectOptions(optionGroup.getOptions());
						}

						for (String value : userInputs) {
							selectOptions(value, selection.getOptions());
							for (OptionGroup optionGroup : selection.getOptionGroups()) {
								selectOptions(value, optionGroup.getOptions());
							}
						}
					}
				}
			}
		}
	}

	private void deselectOptions(List<org.appng.xml.platform.Option> options) {
		for (org.appng.xml.platform.Option option : options) {
			option.setSelected(false);
		}
	}

	private void selectOptions(String value, List<org.appng.xml.platform.Option> options) {
		for (org.appng.xml.platform.Option option : options) {
			boolean selected = Objects.equals(option.getValue(), value);
			if (selected) {
				option.setSelected(selected);
			}
		}
	}

	@Override
	public String toString() {
		return actionRef.getEventId() + ":" + actionRef.getId();
	}

}
