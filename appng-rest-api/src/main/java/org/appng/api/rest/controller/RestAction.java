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
package org.appng.api.rest.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.MessageInterpolator;
import javax.xml.bind.JAXBException;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.ProcessingException;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.rest.model.Action;
import org.appng.api.rest.model.ErrorModel;
import org.appng.api.rest.model.FieldType;
import org.appng.api.rest.model.FormField;
import org.appng.api.rest.model.Option;
import org.appng.api.rest.model.Parameter;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DefaultPermissionProcessor;
import org.appng.api.support.LabelSupport;
import org.appng.api.support.RequestSupportImpl;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.api.support.validation.LocalizedMessageInterpolator;
import org.appng.core.model.ApplicationProvider;
import org.appng.forms.Request;
import org.appng.forms.impl.RequestBean;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.Validation;
import org.slf4j.Logger;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class RestAction extends RestOperation {

	private static final String FORM_ACTION = "form_action";
	private MessageSource messageSource;

	@Autowired
	public RestAction(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@GetMapping(path = "/action/{event-id}/{id}")
	public ResponseEntity<Action> getAction(@PathVariable(name = "event-id") String eventId,
			@PathVariable(name = "id") String actionId, Site site, Application application, Environment environment,
			HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws JAXBException, InvalidConfigurationException, ProcessingException {

		RestRequest initialRequest = new RestRequest(null, null);
		httpServletRequest.getParameterMap().keySet().stream().filter(k -> !k.equals(FORM_ACTION)).forEach(key -> {
			String[] parameterValues = httpServletRequest.getParameterValues(key);
			for (String parameterValue : parameterValues) {
				initialRequest.addParameter(key, parameterValue);
			}
		});

		ApplicationProvider applicationProvider = (ApplicationProvider) application;
		MarshallService marshallService = MarshallService.getMarshallService();
		initRequest(site, application, environment, applicationProvider, initialRequest);
		org.appng.xml.platform.Action initialAction = applicationProvider.processAction(httpServletResponse, false,
				initialRequest, actionId, eventId, marshallService);
		if (httpServletResponse.getStatus() != HttpStatus.OK.value()) {
			return new ResponseEntity<>(HttpStatus.valueOf(httpServletResponse.getStatus()));
		}

		Action action = getAction(initialRequest, initialAction, environment, null);
		postProcessAction(action, site, application, environment);
		return new ResponseEntity<Action>(action, HttpStatus.OK);
	}

	@RequestMapping(path = "/action/{event-id}/{id}", method = { RequestMethod.POST, RequestMethod.PUT,
			RequestMethod.DELETE })
	public ResponseEntity<Action> performAction(@PathVariable(name = "event-id") String eventId,
			@PathVariable(name = "id") String actionId, @RequestBody Action receivedData, Site site,
			Application application, Environment environment, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws ProcessingException, JAXBException,
			InvalidConfigurationException, org.appng.api.ProcessingException {

		ApplicationProvider applicationProvider = (ApplicationProvider) application;
		MarshallService marshallService = MarshallService.getMarshallService();

		RestRequest initialRequest = initRequest(site, application, environment, applicationProvider,
				new RestRequest(null, null));
		org.appng.xml.platform.Action initialAction = applicationProvider.processAction(httpServletResponse, false,
				initialRequest, actionId, eventId, marshallService);
		if (httpServletResponse.getStatus() != HttpStatus.OK.value()) {
			return new ResponseEntity<>(HttpStatus.valueOf(httpServletResponse.getStatus()));
		}

		RestRequest executingRequest = new RestRequest(initialAction, receivedData);
		initRequest(site, application, environment, applicationProvider, executingRequest);
		org.appng.xml.platform.Action processedAction = applicationProvider.processAction(httpServletResponse, false,
				executingRequest, actionId, eventId, marshallService);
		if (httpServletResponse.getStatus() != HttpStatus.OK.value()) {
			return new ResponseEntity<>(HttpStatus.valueOf(httpServletResponse.getStatus()));
		}

		Action action = getAction(executingRequest, processedAction, environment, receivedData);
		postProcessAction(action, site, application, environment);
		return new ResponseEntity<>(action, HttpStatus.OK);
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
			FormField formField = new FormField();
			formField.setName(f.getName());
			formField.setValue(f.getValue());
			action.getFields().add(formField);

			Optional<FieldDef> originalDef = processedAction.getConfig().getMetaData().getFields().stream()
					.filter(originalField -> originalField.getName().equals(f.getName())).findFirst();
			if (originalDef.isPresent()) {
				FieldDef fieldDef = originalDef.get();
				formField.setFormat(fieldDef.getFormat());
				formField.setLabel(fieldDef.getLabel().getId());
				formField.setReadonly(Boolean.TRUE.toString().equals(fieldDef.getReadonly()));
				formField.setVisible(!Boolean.TRUE.toString().equals(fieldDef.getHidden()));
				formField.setFieldType(FieldType.valueOf(fieldDef.getType().name().toUpperCase()));
				if (null != receivedData) {
					formField.setValue(receivedData.getFields().stream()
							.filter(pdf -> pdf.getName().equals(f.getName())).findFirst().get().getValue());
				}
				applyValidationRules(request, formField, originalDef);

				formField.setMessages(getMessages(fieldDef.getMessages()));
				if (isSelectionType(formField.getFieldType())) {
					Optional<Selection> selection = processedAction.getData().getSelections().parallelStream()
							.filter(s -> s.getId().equals(formField.getName())).findFirst();
					if (selection.isPresent()) {
						selection.get().getOptions().forEach(o -> {
							Option option = getOption(o);
							formField.addOptionsItem(option);
						});
						selection.get().getOptionGroups().forEach(og -> {
							Option optionGroup = new Option();
							optionGroup.setLabel(og.getLabel().getValue());
							formField.addOptionsItem(optionGroup);
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

	protected void applyValidationRules(ApplicationRequest request, FormField formField,
			Optional<FieldDef> originalDef) {
		Validation validation = originalDef.get().getValidation();
		if (null != validation) {
			formField.setRules(new ArrayList<>());
			validation.getRules().forEach(r -> {
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
				formField.getRules().add(rule);
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
			Request request) {
		if (null != receivedData && null != original) {
			Params params = original.getConfig().getParams();
			if (null != params) {
				params.getParam().forEach(originalParam -> {
					Optional<Parameter> parameter = receivedData.getParameters().stream()
							.filter(p -> p.getName().equals(originalParam.getName())).findFirst();
					if (parameter.isPresent()) {
						String value = parameter.get().getValue();
						request.addParameter(parameter.get().getName(), null == value ? null : value.toString());
					}

				});
			}

			original.getConfig().getMetaData().getFields().forEach(originalField -> {
				if (!Boolean.TRUE.toString().equalsIgnoreCase(originalField.getReadonly())) {
					Optional<FormField> formField = receivedData.getFields().stream()
							.filter(f -> f.getName().equals(originalField.getName())).findFirst();
					if (formField.isPresent()) {
						if (isSelectionType(originalField.getType())) {
							List<Option> options = formField.get().getOptions();
							List<String> selectedValues = options.stream()
									.filter(o -> Boolean.TRUE.equals(o.isSelected())).map(o -> o.getValue())
									.collect(Collectors.toList());
							selectedValues.forEach(s -> request.addParameter(originalField.getBinding(), s));
							options.stream().forEach(o -> {
								List<Option> groups = o.getGroups();
								if (null != groups) {
									List<String> selectedValuesfromGroups = groups.stream()
											.filter(groupOption -> Boolean.TRUE.equals(o.isSelected()))
											.map(groupOption -> groupOption.getValue()).collect(Collectors.toList());
									selectedValuesfromGroups
											.forEach(s -> request.addParameter(originalField.getBinding(), s));
								}
							});
						} else {
							Object value = formField.get().getValue();
							request.addParameter(originalField.getBinding(), null == value ? null : value.toString());
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

		protected RequestBean initWrappedRequest() {
			RequestBean wrappedRequest = new RequestBean() {
				@Override
				public void addParameter(String key, String value) {
					if (!parameters.containsKey(key)) {
						log.debug("added parameter {}={}", key, value);
						parameters.put(key, new ArrayList<>());
					}
					parameters.get(key).add(value);
				}
			};
			setWrappedRequest(wrappedRequest);
			return wrappedRequest;
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
