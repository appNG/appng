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
package org.appng.core.controller.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.MessageInterpolator;
import javax.xml.bind.JAXBException;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.rest.model.Action;
import org.appng.api.rest.model.ActionField;
import org.appng.api.rest.model.ErrorModel;
import org.appng.api.rest.model.FieldType;
import org.appng.api.rest.model.Option;
import org.appng.api.rest.model.Options;
import org.appng.api.rest.model.Parameter;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DefaultPermissionProcessor;
import org.appng.api.support.LabelSupport;
import org.appng.api.support.RequestSupportImpl;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.api.support.validation.LocalizedMessageInterpolator;
import org.appng.core.model.ApplicationProvider;
import org.appng.forms.impl.RequestBean;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

abstract class RestActionBase extends RestOperation {

	private static final Logger log = LoggerFactory.getLogger(RestActionBase.class);
	private static final String FORM_ACTION = "form_action";
	private MessageSource messageSource;

	@Autowired
	public RestActionBase(Site site, Application application, Request request, boolean supportPathParameters,
			MessageSource messageSource) {
		super(site, application, request, supportPathParameters);
		this.messageSource = messageSource;
	}

	@GetMapping(path = { "/action/{event-id}/{id}", "/action/{event-id}/{id}/{pathVar1}",
			"/action/{event-id}/{id}/{pathVar1}/{pathVar2}", "/action/{event-id}/{id}/{pathVar1}/{pathVar2}/{pathVar3}",
			"/action/{event-id}/{id}/{pathVar1}/{pathVar2}/{pathVar3}/{pathVar4}",
			"/action/{event-id}/{id}/{pathVar1}/{pathVar2}/{pathVar3}/{pathVar4}/{pathVar5}" })
	public ResponseEntity<Action> getAction(@PathVariable(name = "event-id") String eventId,
			@PathVariable(name = "id") String actionId,
			@PathVariable(required = false) Map<String, String> pathVariables, Environment env,
			HttpServletRequest servletReq, HttpServletResponse servletResp)
			throws JAXBException, InvalidConfigurationException, ProcessingException {
		ApplicationProvider applicationProvider = (ApplicationProvider) application;
		org.appng.xml.platform.Action originalAction = applicationProvider.getApplicationConfig().getAction(eventId,
				actionId);

		if (null == originalAction) {
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
			return new ResponseEntity<>(HttpStatus.valueOf(servletResp.getStatus()));
		}

		Action action = getAction(initialRequest, initialAction, env, null);
		postProcessAction(action, site, application, env);
		return new ResponseEntity<Action>(action, hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
	}

	@RequestMapping(path = { "/action/{event-id}/{id}", "/action/{event-id}/{id}/{pathVar1}",
			"/action/{event-id}/{id}/{pathVar1}/{pathVar2}", "/action/{event-id}/{id}/{pathVar1}/{pathVar2}/{pathVar3}",
			"/action/{event-id}/{id}/{pathVar1}/{pathVar2}/{pathVar3}/{pathVar4}",
			"/action/{event-id}/{id}/{pathVar1}/{pathVar2}/{pathVar3}/{pathVar4}/{pathVar5}" }, method = {
					RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE })
	public ResponseEntity<Action> performAction(@PathVariable(name = "event-id") String eventId,
			@PathVariable(name = "id") String actionId,
			@PathVariable(required = false) Map<String, String> pathVariables, @RequestBody Action receivedData,
			Environment env, HttpServletRequest servletReq, HttpServletResponse servletResp) throws ProcessingException,
			JAXBException, InvalidConfigurationException, org.appng.api.ProcessingException {

		ApplicationProvider applicationProvider = (ApplicationProvider) application;
		org.appng.xml.platform.Action originalAction = applicationProvider.getApplicationConfig().getAction(eventId,
				actionId);

		if (null == originalAction) {
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
			return new ResponseEntity<>(HttpStatus.valueOf(servletResp.getStatus()));
		}

		RestRequest executingRequest = new RestRequest(initialAction, receivedData);
		initRequest(site, application, env, applicationProvider, executingRequest);
		org.appng.xml.platform.Action processedAction = applicationProvider.processAction(servletResp, false,
				executingRequest, actionId, eventId, marshallService);
		if (servletResp.getStatus() != HttpStatus.OK.value()) {
			return new ResponseEntity<>(HttpStatus.valueOf(servletResp.getStatus()));
		}

		Action action = getAction(executingRequest, processedAction, env, receivedData);
		postProcessAction(action, site, application, env);
		return new ResponseEntity<>(action, hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
	}

	protected RestRequest getInitialRequest(Site site, Application application, Environment environment,
			HttpServletRequest httpServletRequest, ApplicationProvider applicationProvider) {
		RestRequest initialRequest = new RestRequest();
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
		Action action = new Action();
		action.setId(processedAction.getId());
		action.setEventId(processedAction.getEventId());
		action.setUser(getUser(environment));
		action.setParameters(getParameters(processedAction.getConfig().getParams()));

		action.setFields(new ArrayList<>());
		processedAction.getData().getResult().getFields().forEach(f -> {
			ActionField actionField = new ActionField();
			actionField.setName(f.getName());
			actionField.setValue(f.getValue());
			action.getFields().add(actionField);

			Optional<FieldDef> originalDef = processedAction.getConfig().getMetaData().getFields().stream()
					.filter(originalField -> originalField.getName().equals(f.getName())).findFirst();
			if (originalDef.isPresent()) {
				FieldDef fieldDef = originalDef.get();
				actionField.setFormat(fieldDef.getFormat());
				actionField.setLabel(fieldDef.getLabel().getId());
				actionField.setReadonly(Boolean.TRUE.toString().equals(fieldDef.getReadonly()));
				actionField.setVisible(!Boolean.TRUE.toString().equals(fieldDef.getHidden()));
				actionField.setFieldType(FieldType.valueOf(fieldDef.getType().name().toUpperCase()));
				if (null != receivedData) {
					actionField.setValue(receivedData.getFields().stream()
							.filter(pdf -> pdf.getName().equals(f.getName())).findFirst().get().getValue());
				}
				applyValidationRules(request, actionField, originalDef.get());

				actionField.setMessages(getMessages(fieldDef.getMessages()));
				if (isSelectionType(actionField.getFieldType())) {
					Optional<Selection> selection = processedAction.getData().getSelections().parallelStream()
							.filter(s -> s.getId().equals(actionField.getName())).findFirst();
					if (selection.isPresent()) {
						actionField.setOptions(new Options());
						selection.get().getOptions().forEach(o -> {
							Option option = getOption(o);
							actionField.getOptions().addEntriesItem(option);
						});
						selection.get().getOptionGroups().forEach(og -> {
							Option optionGroup = new Option();
							optionGroup.setLabel(og.getLabel().getValue());
							actionField.getOptions().addEntriesItem(optionGroup);
							optionGroup.setGroups(new ArrayList<>());
							og.getOptions().forEach(o -> {
								optionGroup.getGroups().add(getOption(o));
							});
						});
					}

				}
			}
		});

		action.setMessages(getMessages(processedAction.getMessages()));
		return action;
	}

	protected void applyValidationRules(ApplicationRequest request, ActionField actionField, FieldDef originalDef) {
		Validation validation = originalDef.getValidation();
		if (null != validation) {
			actionField.setRules(new ArrayList<>());
			validation.getRules().forEach(r -> {
				List<String> existingTypes = actionField.getRules().stream().map(x -> x.getType())
						.collect(Collectors.toList());
				if (!existingTypes.contains(r.getType())) {
					org.appng.api.rest.model.ValidationRule rule = new org.appng.api.rest.model.ValidationRule();
					rule.setMessage(r.getMessage().getContent());
					rule.setType(r.getType());
					List<org.appng.xml.platform.Rule.Option> options = r.getOption();
					if (null != options) {
						rule.setOptions(new ArrayList<>());
						options.forEach(o -> {
							Parameter p = new Parameter();
							p.setName(o.getName());
							p.setValue(o.getValue());
							rule.getOptions().add(p);
						});
					}
					actionField.getRules().add(rule);
					log.debug("Added rule {} to field {} (contains {} rules)", rule.getType(), actionField.getName(),
							actionField.getRules().size());
				}
			});
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
					Optional<Parameter> parameter = receivedData.getParameters().stream()
							.filter(p -> p.getName().equals(originalParam.getName())).findFirst();
					if (parameter.isPresent()) {
						String value = parameter.get().getValue();
						formRequest.addParameter(parameter.get().getName(), null == value ? null : value.toString());
					}

				});
			}

			original.getConfig().getMetaData().getFields().forEach(originalField -> {
				if (!Boolean.TRUE.toString().equalsIgnoreCase(originalField.getReadonly())) {
					Optional<ActionField> actionField = receivedData.getFields().stream()
							.filter(f -> f.getName().equals(originalField.getName())).findFirst();
					if (actionField.isPresent()) {
						if (isSelectionType(originalField.getType())) {
							List<Option> options = actionField.get().getOptions().getEntries();
							List<String> selectedValues = options.stream()
									.filter(o -> Boolean.TRUE.equals(o.isSelected())).map(o -> o.getValue())
									.collect(Collectors.toList());
							selectedValues.forEach(s -> formRequest.addParameter(originalField.getBinding(), s));
							options.stream().forEach(o -> {
								List<Option> groups = o.getGroups();
								if (null != groups) {
									List<String> selectedValuesfromGroups = groups.stream()
											.filter(groupOption -> Boolean.TRUE.equals(groupOption.isSelected()))
											.map(groupOption -> groupOption.getValue()).collect(Collectors.toList());
									selectedValuesfromGroups
											.forEach(s -> formRequest.addParameter(originalField.getBinding(), s));
								}
							});
						} else {
							formRequest.addParameter(originalField.getBinding(), actionField.get().getValue());
						}
					}
				}
			});
		}
	}

	class RestRequest extends ApplicationRequest {
		RestRequest(org.appng.xml.platform.Action original, Action receivedData) {
			RequestBean wrappedRequest = initWrappedRequest();
			extractRequestParameters(original, receivedData, wrappedRequest);
			log.debug("Parameters: {}", wrappedRequest.getParametersList());
		}

		RestRequest() {
			initWrappedRequest();
		}

		protected RequestBean initWrappedRequest() {
			RequestBean wrappedRequest = new RequestBean() {
				@Override
				public void addParameter(String key, String value) {
					if (!parameters.containsKey(key)) {
						parameters.put(key, new ArrayList<>());
					}
					log.debug("added parameter {}={}", key, value);
					parameters.get(key).add(value);
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

	@ExceptionHandler
	public ResponseEntity<ErrorModel> handleError(Exception e, HttpServletResponse response) {
		return super.handleError(e, response);
	}

	protected void postProcessAction(Action action, Site site, Application application, Environment environment) {
		// optionally implemented by subclass
	}

	Logger getLogger() {
		return log;
	}
}
