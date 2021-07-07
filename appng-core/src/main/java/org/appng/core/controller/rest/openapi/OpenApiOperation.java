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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.MessageInterpolator;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Request;
import org.appng.api.ValidationProvider;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.DollarParameterSupport;
import org.appng.api.support.validation.DefaultValidationProvider;
import org.appng.api.support.validation.LocalizedMessageInterpolator;
import org.appng.openapi.model.ErrorModel;
import org.appng.openapi.model.FieldType;
import org.appng.openapi.model.Message;
import org.appng.openapi.model.Message.LevelEnum;
import org.appng.openapi.model.Option;
import org.appng.openapi.model.Permission;
import org.appng.openapi.model.Permission.ModeEnum;
import org.appng.openapi.model.User;
import org.appng.xml.BuilderFactory;
import org.appng.xml.MarshallService;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Permissions;
import org.appng.xml.platform.Rule;
import org.appng.xml.platform.ValidationGroups;
import org.appng.xml.platform.ValidationGroups.Group;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

abstract class OpenApiOperation {

	protected static final String INDEXED = "[]";
	protected static final String INDEXED_EXPR = "\\[\\]";
	protected static final String INDEX = "\\[\\d+\\]";
	protected static final String FORM_ACTION = "form_action";
	protected static final String PATH_VAR = "pathVar";
	protected Site site;
	protected Application application;
	protected ApplicationRequest request;
	protected boolean supportPathParameters;
	protected boolean errors = false;
	protected MessageSource messageSource;
	protected MarshallService marshallService;

	public OpenApiOperation(Site site, Application application, Request request, MessageSource messageSource,
			boolean supportPathParameters) throws JAXBException {
		this.site = site;
		this.application = application;
		this.request = (ApplicationRequest) request;
		this.messageSource = messageSource;
		this.supportPathParameters = supportPathParameters;
		this.marshallService = MarshallService.getMarshallService();
		marshallService.setDocumentBuilderFactory(BuilderFactory.documentBuilderFactory());
	}

	protected User getUser(Environment environment) {
		User user = new User();
		Subject subject = environment.getSubject();
		if (null != subject) {
			user.setEmail(subject.getEmail());
			user.setName(subject.getRealname());
			user.setAuthenticated(subject.isAuthenticated());
		} else {
			user.setAuthenticated(false);
		}
		user.setLocale(environment.getLocale().toString());
		user.setTimezone(environment.getTimeZone().getID());
		DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(environment.getLocale());
		user.setDecimalSeparator(String.valueOf(decimalFormatSymbols.getDecimalSeparator()));
		user.setGroupingSeparator(String.valueOf(decimalFormatSymbols.getGroupingSeparator()));
		return user;
	}

	protected List<Permission> getPermissions(Permissions permissions) {
		List<Permission> permissionList = null;
		if (null != permissions) {
			permissionList = new ArrayList<>();
			for (org.appng.xml.platform.Permission p : permissions.getPermissionList()) {
				Permission permission = new Permission();
				permission.setValue(Boolean.TRUE.equals(Boolean.valueOf(p.getValue())));
				permission.setRef(p.getRef());
				permission.setMode(ModeEnum.valueOf(p.getMode().name()));
				permissionList.add(permission);
			}
		}
		return permissionList;
	}

	protected Map<String, String> getParameters(Params params) {
		Map<String, String> parameters = new HashMap<>();
		if (null != params) {
			params.getParam().stream().filter(p -> !p.getName().equals(FORM_ACTION)).forEach(p -> {
				parameters.put(p.getName(), p.getValue());
			});
		}
		return parameters;
	}

	protected List<Message> getMessages(Messages messages) {
		List<Message> messageList = new ArrayList<>();
		if (null != messages) {
			messages.getMessageList().forEach(originalMessage -> {
				Message message = new Message();
				message.setLevel(LevelEnum.fromValue(originalMessage.getClazz().name()));
				message.setText(originalMessage.getContent());
				message.setKey(originalMessage.getCode());
				messageList.add(message);
				errors |= originalMessage.getClazz().equals(MessageType.ERROR);
			});
		}
		return messageList;
	}

	protected Option getOption(String binding, org.appng.xml.platform.Option o, Collection<String> optionUserInput) {
		Option option = new Option();
		option.setLabel(o.getName());
		if (optionUserInput.contains(o.getValue())) {
			option.setSelected(true);
			getLogger().debug("Option {} for selection {} was selected by user", o.getValue(), binding);
		} else {
			option.setSelected(o.isSelected());
		}
		option.setValue(o.getValue());
		option.setHits(o.getHits());
		return option;
	}

	protected boolean isSelectionType(FieldType type) {
		return null != type
				&& Arrays.asList(FieldType.LIST_CHECKBOX, FieldType.LIST_RADIO, FieldType.LIST_SELECT).contains(type);
	}

	protected boolean isSelectionType(org.appng.xml.platform.FieldType type) {
		return null != type && Arrays.asList(org.appng.xml.platform.FieldType.LIST_CHECKBOX,
				org.appng.xml.platform.FieldType.LIST_RADIO, org.appng.xml.platform.FieldType.LIST_SELECT)
				.contains(type);
	}

	private boolean isObjectOrListOf(org.appng.xml.platform.FieldType type) {
		return null != type
				&& Arrays.asList(org.appng.xml.platform.FieldType.LIST_OBJECT, org.appng.xml.platform.FieldType.OBJECT)
						.contains(type);
	}

	abstract Logger getLogger();

	protected void applyPathParameters(Map<String, String> pathVariables, DataConfig config,
			ApplicationRequest applicationRequest) {
		Params params = config.getParams();
		if (null != params) {
			List<Param> paramList = params.getParam();
			int maxParams = 5;
			int paramNo = 1;
			while (paramNo <= paramList.size() && paramNo < maxParams) {
				String paramName = paramList.get(paramNo - 1).getName();
				String pathVariable = pathVariables.get(PATH_VAR + paramNo);
				if (StringUtils.isNotBlank(pathVariable)) {
					applicationRequest.addParameter(paramName, pathVariable);
					getLogger().debug("added path parameter {}:{}", paramName, pathVariable);
				}
				paramNo++;
			}
		}
	}

	protected boolean hasErrors() {
		return errors;
	}

	protected Optional<FieldDef> getChildField(FieldDef fieldDef, Datafield fieldData, final int index,
			Datafield childData) {
		final String name = childData.getName();
		Optional<FieldDef> childField;
		List<FieldDef> childFieldDefs = fieldDef.getFields();
		if (fieldData.getType().equals(org.appng.xml.platform.FieldType.LIST_OBJECT)) {
			childField = childFieldDefs.stream().filter(originalField -> {
				String indexedName = getIndexedName(originalField.getName(), index);
				boolean matches = indexedName.equals(name);
				getLogger().debug(
						"Child {} ({}) of field {} ({})" + (matches ? "" : " not")
								+ " found. (Name of child data field: {})",
						indexedName, originalField.getType(), fieldDef.getName(), fieldDef.getType(), name);
				return matches;
			}).findFirst();
		} else {
			String childDataName = name.replaceAll(INDEX, INDEXED);
			childField = childFieldDefs.stream().filter(originalField -> originalField.getName().equals(childDataName))
					.findFirst();
			getLogger().debug("Child {} of field {} ({})" + (childField.isPresent() ? "" : " not") + " found.", name,
					fieldDef.getName(), fieldDef.getType());
		}
		return childField;
	}

	private String getIndexedName(String name, int index) {
		return name.replaceAll(INDEXED_EXPR, "[" + index + "]");
	}

	protected Object getObjectValue(Datafield data, FieldDef field, Class<?> type, List<String> userInput) {
		org.appng.xml.platform.FieldType fieldType = field.getType();
		if (isSelectionType(fieldType) || isObjectOrListOf(fieldType)) {
			return null;
		}

		boolean isDecimal = org.appng.xml.platform.FieldType.DECIMAL.equals(fieldType);
		String value = data.getValue();
		if (userInput.size() > 0) {
			value = userInput.get(0);
			getLogger().debug("Value '{}' for field '{}' was provided by user", value, field.getBinding());
		}

		if (isDecimal || org.appng.xml.platform.FieldType.LONG.equals(fieldType)
				|| org.appng.xml.platform.FieldType.INT.equals(fieldType)) {
			String format = field.getFormat();
			if (StringUtils.isNotBlank(value)) {
				try {
					Number number = getDecimalFormat(format).parse(value);
					return isDecimal ? number.doubleValue() : number;
				} catch (Exception e) {
					getLogger().error(String.format("error while parsing value '%s' for field '%s' using pattern %s",
							value, field.getBinding(), format), e);
				}
			}
			return null;
		} else if (org.appng.xml.platform.FieldType.CHECKBOX.equals(fieldType) || boolean.class.equals(type)
				|| Boolean.class.equals(type)) {
			return Boolean.valueOf(value);
		}

		return value;
	}

	protected Class<?> getBindClass(MetaData metaData) {
		try {
			return site.getSiteClassLoader().loadClass(metaData.getBindClass());
		} catch (ClassNotFoundException e) {
			getLogger().error("bindClass not found", e);
			return null;
		}
	}

	protected DecimalFormat getDecimalFormat(String format) {
		return new DecimalFormat(format, new DecimalFormatSymbols(request.getLocale()));
	}

	@Slf4j
	@ControllerAdvice
	static class RestErrorHandler extends ResponseEntityExceptionHandler {

		@ExceptionHandler
		public ResponseEntity<ErrorModel> handleError(Exception exception, Site site, Application application,
				Environment environment, HttpServletRequest request, HttpServletResponse response) throws Exception {
			LOGGER.error("error in REST service", exception);
			String message;
			if (application.getProperties().getBoolean("restErrorPrintStackTrace", true)) {
				StringWriter writer = new StringWriter();
				exception.printStackTrace(new PrintWriter(writer));
				message = writer.toString();
			} else {
				message = String.format("%s : %s", exception.getClass().getName(), exception.getMessage());
			}
			ErrorModel errorModel = new ErrorModel();
			errorModel.setMessage(message);
			errorModel.setCode(response.getStatus());
			return new ResponseEntity<>(errorModel, HttpStatus.valueOf(response.getStatus()));
		}
	}

	protected org.appng.openapi.model.ValidationRule getRule(Rule r) {
		org.appng.openapi.model.ValidationRule rule = new org.appng.openapi.model.ValidationRule();
		rule.setMessage(r.getMessage().getContent());
		rule.setType(r.getType());
		rule.setMessageKey(r.getMessage().getCode());
		List<org.appng.xml.platform.Rule.Option> options = r.getOption();
		if (null != options) {
			rule.setOptions(new HashMap<>());
			options.forEach(o -> {
				rule.getOptions().put(o.getName(), o.getValue());
			});
		}
		return rule;
	}

	protected void addValidationRules(MetaData metaData) {
		List<Class<?>> validationGroups = new ArrayList<>();
		try {
			ValidationGroups validation = metaData.getValidation();
			if (null != validation) {
				for (Group g : validation.getGroups()) {
					Class<?> group = site.getSiteClassLoader().loadClass(g.getClazz());
					validationGroups.add(group);
				}
			}
			Locale locale = request.getLocale();
			MessageInterpolator messageInterpolator = new LocalizedMessageInterpolator(locale, messageSource);
			ValidationProvider validationProvider = new DefaultValidationProvider(messageInterpolator, messageSource,
					locale, true);
			validationProvider.addValidationMetaData(metaData, site.getSiteClassLoader(),
					validationGroups.toArray(new Class[0]));
		} catch (ClassNotFoundException e) {
			getLogger().error("error retrieving validation group", e);
		}
	}

	protected StringBuilder getSelf(String suffix) {
		return new StringBuilder("/service/" + site.getName() + "/" + application.getName() + "/rest/openapi" + suffix);
	}

	protected boolean appendParams(Params params, StringBuilder self) {
		if (null != params) {
			params.getParam().forEach(p -> {
				try {
					self.append(params.getParam().indexOf(p) == 0 ? "?" : "&");
					self.append(URLEncoder.encode(p.getName(), StandardCharsets.UTF_8.name()));
					self.append("=");
					self.append(URLEncoder.encode(p.getValue(), StandardCharsets.UTF_8.name()));
				} catch (UnsupportedEncodingException e) {
					// will not happen
				}
			});
			return true;
		}
		return false;
	}

	protected String getLabelMessage(Label label, MessageSource messages, Locale locale,
			DollarParameterSupport parameterSupport) {
		String code;
		if (null != label.getId()) {
			code = label.getId();
		} else {
			code = label.getValue();
		}
		Object[] params = null;
		if (null != label.getParams()) {
			params = Arrays.stream(label.getParams().split(",")) //
					.map(parameterSupport::replaceParameters) //
					.toArray();
		}
		return messages.getMessage(code, params, code, locale);
	}
}
