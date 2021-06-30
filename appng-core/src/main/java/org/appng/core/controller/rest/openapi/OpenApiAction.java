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
package org.appng.core.controller.rest.openapi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.MessageInterpolator;
import javax.ws.rs.ext.ParamConverter.Lazy;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DefaultPermissionProcessor;
import org.appng.api.support.LabelSupport;
import org.appng.api.support.RequestSupportImpl;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.api.support.validation.LocalizedMessageInterpolator;
import org.appng.core.model.ApplicationProvider;
import org.appng.forms.impl.RequestBean;
import org.appng.openapi.model.Action;
import org.appng.openapi.model.ActionField;
import org.appng.openapi.model.FieldType;
import org.appng.openapi.model.Option;
import org.appng.openapi.model.Options;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionType;
import org.appng.xml.platform.UserInputField;
import org.appng.xml.platform.Validation;
import org.slf4j.Logger;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Lazy
@RestController
@RequestScope(proxyMode = ScopedProxyMode.NO)
public class OpenApiAction extends OpenApiOperation {

	private static final String ACTION_PATH = "/openapi/action/{event-id}/{id}";

	@Autowired
	public OpenApiAction(Site site, Application application, Request request, MessageSource messageSource,
			@Value("${restUsePathParameters:true}") boolean supportPathParameters) throws JAXBException {
		super(site, application, request, messageSource, supportPathParameters);
	}

	// @formatter:off
	@GetMapping(
		path = {
			ACTION_PATH,
			ACTION_PATH + "/{pathVar1:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}/{pathVar4:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}/{pathVar4:.+}/{pathVar5:.+}"
		},
		produces =  MediaType.APPLICATION_JSON_UTF8_VALUE
	)
	// @formatter:on
	public ResponseEntity<Action> getAction(@PathVariable(name = "event-id") String eventId,
			@PathVariable(name = "id") String actionId,
			@PathVariable(required = false) Map<String, String> pathVariables, Environment env,
			HttpServletRequest servletReq, HttpServletResponse servletResp)
			throws JAXBException, InvalidConfigurationException, ProcessingException {
		ApplicationProvider applicationProvider = (ApplicationProvider) application;
		org.appng.xml.platform.Action originalAction = applicationProvider.getApplicationConfig().getAction(eventId,
				actionId);

		if (null == originalAction) {
			LOGGER.debug("Action {}:{} not found on application {} of site {}", eventId, actionId,
					application.getName(), site.getName());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		MarshallService marshallService = MarshallService.getMarshallService();

		RestRequest initialRequest = getInitialRequest(site, application, env, servletReq, applicationProvider);

		if (supportPathParameters) {
			applyPathParameters(pathVariables, originalAction.getConfig(), initialRequest);
		}

		org.appng.xml.platform.Action initialAction = applicationProvider.processAction(servletResp, false,
				initialRequest, actionId, eventId, marshallService);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Processed action: {}", marshallService.marshallNonRoot(initialAction));
		}

		if (servletResp.getStatus() != HttpStatus.OK.value()) {
			LOGGER.debug("Action {}:{} on application {} of site {} returned status {}", eventId, actionId,
					application.getName(), site.getName(), servletResp.getStatus());
			return new ResponseEntity<>(HttpStatus.valueOf(servletResp.getStatus()));
		}

		Action action = getAction(initialRequest, initialAction, env, null);

		postProcessAction(action, site, application, env);
		return new ResponseEntity<Action>(action, hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
	}

	// @formatter:off
	@PostMapping(
		path = {
			ACTION_PATH,
			ACTION_PATH + "/{pathVar1:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}/{pathVar4:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}/{pathVar4:.+}/{pathVar5:.+}"
		},
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
		produces =  MediaType.APPLICATION_JSON_VALUE
	)
	// @formatter:on
	public ResponseEntity<Action> performActionMultiPart(
	// @formatter:off
 			@PathVariable(name = "event-id") String eventId,
			@PathVariable(name = "id") String actionId,
			@PathVariable(required = false) Map<String, String> pathVariables,
			@RequestPart("body") Action receivedData,
			Environment env,
			HttpServletRequest servletReq,
			HttpServletResponse servletResp
		// @formatter:on
	) throws ProcessingException, JAXBException, InvalidConfigurationException, org.appng.api.ProcessingException {

		return performAction(eventId, actionId, pathVariables, receivedData, env, servletReq, servletResp);
	}

	// @formatter:off
	@PostMapping(
		path = {
			ACTION_PATH,
			ACTION_PATH + "/{pathVar1:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}/{pathVar4:.+}",
			ACTION_PATH + "/{pathVar1:.+}/{pathVar2:.+}/{pathVar3:.+}/{pathVar4:.+}/{pathVar5:.+}"
		},
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces =  MediaType.APPLICATION_JSON_VALUE
	)
	// @formatter:on
	protected ResponseEntity<Action> performAction(
	// @formatter:off
 			@PathVariable(name = "event-id") String eventId,
			@PathVariable(name = "id") String actionId,
			@PathVariable(required = false) Map<String, String> pathVariables,
			@RequestBody Action receivedData,
			Environment env,
			HttpServletRequest servletReq,
			HttpServletResponse servletResp
		// @formatter:on
	)
			throws JAXBException, InvalidConfigurationException, ProcessingException {
		ApplicationProvider applicationProvider = (ApplicationProvider) application;
		org.appng.xml.platform.Action originalAction = applicationProvider.getApplicationConfig().getAction(eventId,
				actionId);

		if (null == originalAction) {
			LOGGER.debug("Action {}:{} not found on application {} of site {}", eventId, actionId,
					application.getName(), site.getName());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		MarshallService marshallService = MarshallService.getMarshallService();

		RestRequest initialRequest = getInitialRequest(site, application, env, servletReq, applicationProvider);

		if (supportPathParameters) {
			applyPathParameters(pathVariables, originalAction.getConfig(), initialRequest);
		}

		org.appng.xml.platform.Action initialAction = applicationProvider.processAction(servletResp, false,
				initialRequest, actionId, eventId, marshallService);
		if (servletResp.getStatus() != HttpStatus.OK.value()) {
			LOGGER.debug("Action {}:{} on application {} of site {} returned status {}", eventId, actionId,
					application.getName(), site.getName(), servletResp.getStatus());
			return new ResponseEntity<>(HttpStatus.valueOf(servletResp.getStatus()));
		}

		RestRequest executingRequest = new RestRequest(servletReq, initialAction, receivedData);
		initRequest(site, application, env, applicationProvider, executingRequest);
		if (supportPathParameters) {
			applyPathParameters(pathVariables, initialAction.getConfig(), executingRequest);
		}
		org.appng.xml.platform.Action processedAction = applicationProvider.processAction(servletResp, false,
				executingRequest, actionId, eventId, marshallService);
		if (servletResp.getStatus() != HttpStatus.OK.value()) {
			return new ResponseEntity<>(HttpStatus.valueOf(servletResp.getStatus()));
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Processed action: {}", marshallService.marshallNonRoot(processedAction));
		}

		Action action = getAction(executingRequest, processedAction, env, receivedData);
		postProcessAction(action, site, application, env);
		return new ResponseEntity<>(action, hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
	}

	protected RestRequest getInitialRequest(Site site, Application application, Environment environment,
			HttpServletRequest httpServletRequest, ApplicationProvider applicationProvider) {
		RestRequest initialRequest = getRestRequest(httpServletRequest);
		httpServletRequest.getParameterMap().keySet().stream().filter(k -> !k.equals(FORM_ACTION)).forEach(key -> {
			String[] parameterValues = httpServletRequest.getParameterValues(key);
			for (String parameterValue : parameterValues) {
				initialRequest.addParameter(key, parameterValue);
			}
		});
		initRequest(site, application, environment, applicationProvider, initialRequest);
		return initialRequest;
	}

	protected Action getAction(ApplicationRequest request, org.appng.xml.platform.Action processedAction,
			Environment environment, Action receivedData) {
		addValidationRules(processedAction.getConfig().getMetaData());
		Action action = new Action();
		action.setId(processedAction.getId());
		action.setEventId(processedAction.getEventId());
		action.setUser(getUser(environment));
		action.setParameters(getParameters(processedAction.getConfig().getParams()));
		action.setPermissions(getPermissions(processedAction.getConfig().getPermissions()));
		action.setSelf("/service/" + site.getName() + "/" + application.getName() + "/rest/openapi/action/"
				+ processedAction.getEventId() + "/" + processedAction.getId());

		Data data = processedAction.getData();
		if (null != data && null != data.getResult()) {
			data.getResult().getFields().forEach(fieldData -> {
				MetaData metaData = processedAction.getConfig().getMetaData();
				Optional<FieldDef> originalDef = metaData.getFields().stream()
						.filter(originalField -> originalField.getName().equals(fieldData.getName())).findFirst();
				BeanWrapper beanWrapper = getBeanWrapper(metaData);
				ActionField actionField = getActionField(request, processedAction, receivedData, fieldData, originalDef,
						beanWrapper);
				action.addFieldsItem(actionField);
			});
		}

		action.setMessages(getMessages(processedAction.getMessages()));

		StringBuilder self = getSelf("/action/" + action.getEventId() + "/" + action.getId());
		appendParams(processedAction.getConfig().getParams(), self);
		action.setSelf(self.toString());

		return action;
	}

	protected ActionField getActionField(ApplicationRequest request, org.appng.xml.platform.Action processedAction,
			Action receivedData, Datafield fieldData, Optional<FieldDef> originalDef, BeanWrapper beanWrapper) {
		ActionField actionField = new ActionField();
		actionField.setName(fieldData.getName());

		if (originalDef.isPresent()) {
			FieldDef fieldDef = originalDef.get();

			boolean isPassword = org.appng.xml.platform.FieldType.PASSWORD.equals(fieldDef.getType());
			boolean isDate = org.appng.xml.platform.FieldType.DATE.equals(fieldDef.getType());
			boolean isSelection = isSelectionType(fieldDef.getType());

			actionField.setFormat(fieldDef.getFormat());
			boolean hasFormat = StringUtils.isNotBlank(fieldDef.getFormat());
			if (!isDate && hasFormat) {
				actionField.setFormattedValue(fieldData.getValue());
			}
			if (null != fieldDef.getLabel()) {
				actionField.setLabel(fieldDef.getLabel().getId());
			}
			actionField.setReadonly(Boolean.TRUE.toString().equals(fieldDef.getReadonly()));
			actionField.setVisible(!Boolean.TRUE.toString().equals(fieldDef.getHidden()));
			actionField.setFieldType(FieldType.valueOf(fieldDef.getType().name().toUpperCase()));

			final List<String> parameterList;
			if (!isPassword) {
				if (null != processedAction.getUserdata()) {
					// get the user's input from the UserData
					Stream<UserInputField> userDataStream = processedAction.getUserdata().getInput().parallelStream();
					parameterList = userDataStream.filter(i -> i.getName().equals(fieldDef.getBinding()))
							.map(i -> i.getContent()).collect(Collectors.toList());
				} else {
					parameterList = Collections.emptyList();
				}
				Object objectValue = null;
				objectValue = getObjectValue(fieldData, fieldDef, beanWrapper.getPropertyType(fieldDef.getBinding()),
						parameterList);
				actionField.setValue(objectValue);
				if (hasFormat) {
					String formattedValue = getStringValue(actionField);
					actionField.setFormattedValue(formattedValue);
				}
				LOGGER.debug("Setting value {} for field {}", objectValue, actionField.getName());
			} else {
				parameterList = Collections.emptyList();
			}

			if (null != receivedData && !isPassword) {
				// a successfully executed action does not contain UserData, so we have to take the data originally
				// submitted by the user
				Optional<ActionField> receivedField = receivedData.getFields().stream()
						.filter(af -> af.getName().equals(fieldDef.getBinding())).findFirst();
				if (receivedField.isPresent()) {
					Object objectValue = receivedField.get().getValue();
					actionField.setValue(objectValue);
					LOGGER.debug("Setting value {} for field {}", objectValue, actionField.getName());
				}
			}
			applyValidationRules(request, actionField, originalDef.get());

			actionField.setMessages(getMessages(fieldDef.getMessages()));
			if (isSelection) {
				Optional<Selection> selection = processedAction.getData().getSelections().parallelStream()
						.filter(s -> s.getId().equals(actionField.getName())).findFirst();
				if (selection.isPresent()) {
					Options options = new Options();
					options.setMultiple(selection.get().getType().equals(SelectionType.CHECKBOX)
							|| selection.get().getType().equals(SelectionType.SELECT_MULTIPLE));
					actionField.setOptions(options);
					selection.get().getOptions().forEach(o -> {
						Option option = getOption(fieldDef.getBinding(), o, parameterList);
						actionField.getOptions().addEntriesItem(option);
					});
					selection.get().getOptionGroups().forEach(og -> {
						Option optionGroup = new Option();
						optionGroup.setLabel(og.getLabel().getValue());
						actionField.getOptions().addEntriesItem(optionGroup);
						optionGroup.setOptions(new ArrayList<>());
						og.getOptions().forEach(o -> {
							optionGroup.getOptions().add(getOption(fieldDef.getBinding(), o, parameterList));
						});
					});
				}

			}

			List<Datafield> childDataFields = fieldData.getFields();
			if (null != childDataFields) {
				final AtomicInteger i = new AtomicInteger(0);
				Set<String> childNames = new HashSet<>();
				for (Datafield childData : childDataFields) {
					if (!childNames.contains(childData.getName())) {
						LOGGER.debug("Processing child {} of field {} with index {}.", childData.getName(),
								fieldData.getName(), i.get());
						Optional<FieldDef> childField = getChildField(fieldDef, fieldData, i.getAndIncrement(),
								childData);
						ActionField childActionField = getActionField(request, processedAction, receivedData, childData,
								childField, beanWrapper);
						actionField.getFields().add(childActionField);
						if (childField.isPresent()) {
							applyValidationRules(request, childActionField, childField.get());
						}
						childNames.add(childData.getName());
					} else {
						LOGGER.debug("Child {} of field {} with index {} already processed.", childData.getName(),
								fieldData.getName(), i.get());
					}
				}
			}

		}
		return actionField;
	}

	protected void applyValidationRules(ApplicationRequest request, ActionField actionField, FieldDef originalDef) {
		Validation validation = originalDef.getValidation();
		if (null != validation) {
			actionField.setRules(new ArrayList<>());
			validation.getRules().forEach(r -> {
				List<String> existingTypes = actionField.getRules().stream().map(x -> x.getType())
						.collect(Collectors.toList());
				if (!existingTypes.contains(r.getType())) {
					org.appng.openapi.model.ValidationRule rule = getRule(r);
					actionField.getRules().add(rule);
					LOGGER.debug("Added rule {} to field {} (contains {} rules)", rule.getType(), actionField.getName(),
							actionField.getRules().size());
				}
			});
			Map<String, ActionField> childFields = getActionFieldMap(actionField.getFields());
			if (null != childFields) {
				for (ActionField childField : childFields.values()) {
					Optional<FieldDef> childDef = originalDef.getFields().stream()
							.filter(f -> f.getName().equals(childField.getName())).findFirst();
					if (childDef.isPresent()) {
						applyValidationRules(request, childField, childDef.get());
					}
				}
			}
		}
	}

	protected RestRequest initRequest(Site site, Application application, Environment environment,
			ApplicationProvider applicationProvider, RestRequest restRequest) {
		restRequest.setPermissionProcessor(new DefaultPermissionProcessor(environment.getSubject(), site, application));
		restRequest.setLabelSupport(new LabelSupport(messageSource, environment.getLocale()));
		restRequest.setApplicationConfig(applicationProvider.getApplicationConfig());
		MessageInterpolator messageInterpolator = new LocalizedMessageInterpolator(environment.getLocale(),
				messageSource);
		restRequest.setValidationProvider(
				new DefaultValidationProvider(messageInterpolator, messageSource, environment.getLocale(), true));
		ConversionService conversionService = application.getBean("conversionService", ConversionService.class);
		RequestSupportImpl requestSupport = new RequestSupportImpl(conversionService, environment, messageSource);
		requestSupport.afterPropertiesSet();
		restRequest.setRequestSupport(requestSupport);
		return restRequest;
	}

	protected void extractRequestParameters(org.appng.xml.platform.Action original, Action receivedData,
			org.appng.forms.Request formRequest) {
		if (null != receivedData && null != original) {
			Params params = original.getConfig().getParams();
			if (null != params) {
				params.getParam().forEach(originalParam -> {
					String name = originalParam.getName();
					String value = receivedData.getParameters().get(name);
					if (null != value) {
						formRequest.addParameter(name, value);
					}

				});
			}

			MetaData metaData = original.getConfig().getMetaData();
			for (FieldDef field : metaData.getFields()) {
				extractRequestParameter(StringUtils.EMPTY, field, getActionFieldMap(receivedData.getFields()),
						formRequest);
			}
		}
	}

	private Map<String, ActionField> getActionFieldMap(List<ActionField> receivedData) {
		Map<String, ActionField> actionFields = new HashMap<>();
		if (null != receivedData) {
			receivedData.forEach(f -> actionFields.put(f.getName(), f));
		}
		return actionFields;
	}

	private void extractRequestParameter(String pathPrefix, FieldDef field, Map<String, ActionField> actionFields,
			org.appng.forms.Request formRequest) {
		if (!Boolean.TRUE.toString().equalsIgnoreCase(field.getReadonly())) {
			Condition condition = field.getCondition();
			if (null == condition || Boolean.TRUE.toString().equalsIgnoreCase(condition.getExpression())) {
				ActionField actionField = actionFields.get(field.getName());
				boolean isObject = org.appng.xml.platform.FieldType.OBJECT.equals(field.getType());
				boolean isObjectList = org.appng.xml.platform.FieldType.LIST_OBJECT.equals(field.getType());
				if (isObjectList) {
					for (FieldDef child : field.getFields()) {
						extractRequestParameter(pathPrefix, child, getActionFieldMap(actionField.getFields()),
								formRequest);
					}
				} else if (isObject) {
					boolean isArray = field.getBinding().endsWith(INDEXED);
					if (isArray) {
						int i = 0;
						ActionField nested;
						while (null != (nested = actionFields
								.get(field.getName().replace(INDEXED, String.format("[%s]", i++))))) {
							for (FieldDef child : field.getFields()) {
								Map<String, ActionField> nestedFields = getActionFieldMap(nested.getFields());
								String objectPrefix = pathPrefix + nested.getName() + ".";
								extractRequestParameter(objectPrefix, child, nestedFields, formRequest);
							}
						}
					} else {
						for (FieldDef child : field.getFields()) {
							extractRequestParameter(field.getBinding() + ".", child,
									getActionFieldMap(actionField.getFields()), formRequest);
						}
					}
				} else if (isSelectionType(field.getType())) {
					extractSelectionValue(field, actionField, formRequest);
				} else {
					String stringValue = getStringValue(actionField);
					if (null != stringValue) {
						boolean isPassword = org.appng.xml.platform.FieldType.PASSWORD.equals(field.getType());
						String parameterName = pathPrefix + actionField.getName();
						formRequest.addParameter(parameterName, stringValue);
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Added parameter {} = {}", parameterName,
									isPassword ? stringValue.replaceAll(".", "*") : stringValue);
						}
					}
				}
			} else {
				LOGGER.debug("Conditon for field {} did not match.", field.getBinding());
			}
		} else {
			LOGGER.debug("Field {} is readonly.", field.getBinding());
		}
	}

	private String getStringValue(ActionField actionField) {
		if (actionField.getValue() == null) {
			return null;
		}
		FieldType fieldType = actionField.getFieldType();
		if (FieldType.DECIMAL.equals(fieldType) || FieldType.LONG.equals(fieldType)
				|| FieldType.INT.equals(fieldType)) {
			return getDecimalFormat(actionField.getFormat()).format(actionField.getValue());
		}
		return actionField.getValue() == null ? null : actionField.getValue().toString();
	}

	private void extractSelectionValue(FieldDef originalField, ActionField actionField,
			org.appng.forms.Request formRequest) {
		List<Option> options = actionField.getOptions().getEntries();
		List<String> selectedValues = options.stream().filter(o -> Boolean.TRUE.equals(o.getSelected()))
				.map(o -> o.getValue()).collect(Collectors.toList());
		selectedValues.forEach(s -> formRequest.addParameter(originalField.getBinding(), s));
		options.stream().forEach(o -> {
			List<Option> groups = o.getOptions();
			if (null != groups) {
				List<String> selectedValuesfromGroups = groups.stream()
						.filter(groupOption -> Boolean.TRUE.equals(groupOption.getSelected()))
						.map(groupOption -> groupOption.getValue()).collect(Collectors.toList());
				selectedValuesfromGroups.forEach(s -> formRequest.addParameter(originalField.getBinding(), s));
			}
		});
	}

	RestRequest getRestRequest(HttpServletRequest servletRequest) {
		return new RestRequest(servletRequest);
	}

	class RestRequest extends ApplicationRequest {
		RestRequest(HttpServletRequest servletRequest, org.appng.xml.platform.Action original, Action receivedData) {
			RequestBean wrappedRequest = initWrappedRequest(servletRequest);
			extractRequestParameters(original, receivedData, wrappedRequest);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Parameters: {}", wrappedRequest.getParametersList());
			}
		}

		private RestRequest(HttpServletRequest servletRequest) {
			initWrappedRequest(servletRequest);
		}

		private RequestBean initWrappedRequest(final HttpServletRequest servletRequest) {
			RequestBean wrappedRequest = new RequestBean() {
				@Override
				public void addParameter(String key, String value) {
					if (!parameters.containsKey(key)) {
						parameters.put(key, new ArrayList<>());
					}
					parameters.get(key).add(value);
				}

				@Override
				public HttpServletRequest getHttpServletRequest() {
					return servletRequest;
				}

				@Override
				public boolean isGet() {
					return HttpMethod.GET.matches(servletRequest.getMethod());
				}

				@Override
				public boolean isPost() {
					return HttpMethod.POST.matches(servletRequest.getMethod());
				}

				@Override
				public boolean isValid() {
					return true;
				}

				@Override
				public String getEncoding() {
					return StandardCharsets.UTF_8.name();
				}

				@Override
				public String getHost() {
					return servletRequest.getServerName();
				}

			};
			setWrappedRequest(wrappedRequest);
			return wrappedRequest;
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + getWrappedRequest().getParametersList();
		}
	}

	protected void postProcessAction(Action action, Site site, Application application, Environment environment) {
		// optionally implemented by subclass
	}

	Logger getLogger() {
		return LOGGER;
	}
}
