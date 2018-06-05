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

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.FieldWrapper;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.api.rest.model.ErrorModel;
import org.appng.api.rest.model.FieldType;
import org.appng.api.rest.model.Message;
import org.appng.api.rest.model.Message.LevelEnum;
import org.appng.api.rest.model.Option;
import org.appng.api.rest.model.Parameter;
import org.appng.api.rest.model.Permission;
import org.appng.api.rest.model.Permission.ModeEnum;
import org.appng.api.rest.model.User;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.field.FieldConversionFactory;
import org.appng.el.ExpressionEvaluator;
import org.appng.forms.FormUpload;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MessageType;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Params;
import org.appng.xml.platform.Permissions;
import org.slf4j.Logger;
import org.springframework.beans.BeanWrapper;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

abstract class RestOperation {

	protected static final String FORM_ACTION = "form_action";
	protected static final String PATH_VAR = "pathVar";
	protected Site site;
	protected Application application;
	protected ApplicationRequest request;
	protected boolean supportPathParameters;
	protected boolean errors = false;
	protected MessageSource messageSource;
	protected FieldConversionFactory fieldConversionFactory;

	public RestOperation(Site site, Application application, Request request, MessageSource messageSource,
			boolean supportPathParameters) {
		this.site = site;
		this.application = application;
		this.request = (ApplicationRequest) request;
		this.messageSource = messageSource;
		this.supportPathParameters = supportPathParameters;
		this.fieldConversionFactory = new FieldConversionFactory(new ExpressionEvaluator(new HashMap<>()));
		fieldConversionFactory.setEnvironment(request.getEnvironment());
		fieldConversionFactory.setMessageSource(messageSource);
		fieldConversionFactory.setConversionService(new DefaultConversionService());
		fieldConversionFactory.afterPropertiesSet();
	}

	protected User getUser(Environment environment) {
		User user = new User();
		Subject subject = environment.getSubject();
		if (null != subject) {
			user.setEmail(subject.getEmail());
			user.setName(subject.getRealname());
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
			;
		}
		return permissionList;
	}

	protected List<Parameter> getParameters(Params params) {
		List<Parameter> parameterList = new ArrayList<>();
		if (null != params) {
			params.getParam().stream().filter(p -> !p.getName().equals(FORM_ACTION)).forEach(p -> {
				Parameter param = new Parameter();
				param.setName(p.getName());
				param.setValue(p.getValue());
				parameterList.add(param);
			});
		}
		return parameterList;
	}

	protected List<Message> getMessages(Messages messages) {
		List<Message> messageList = new ArrayList<>();
		if (null != messages) {
			messages.getMessageList().forEach(originalMessage -> {
				Message message = new Message();
				message.setLevel(LevelEnum.fromValue(originalMessage.getClazz().name()));
				message.setText(originalMessage.getContent());
				messageList.add(message);
				errors |= originalMessage.getClazz().equals(MessageType.ERROR);
			});
		}
		return messageList;
	}

	protected Option getOption(org.appng.xml.platform.Option o) {
		Option option = new Option();
		option.setLabel(o.getName());
		option.setSelected(o.isSelected());
		option.setValue(o.getValue());
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

	abstract Logger getLogger();

	public ResponseEntity<ErrorModel> handleError(Exception e, HttpServletResponse response) {
		getLogger().error("", e);
		ErrorModel errorModel = new ErrorModel();
		errorModel.setCode(response.getStatus());
		errorModel.setMessage(e.getMessage());
		return new ResponseEntity<>(errorModel, HttpStatus.INTERNAL_SERVER_ERROR);
	}

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

	protected Optional<FieldDef> getChildField(FieldDef fieldDef, Datafield fieldData, final AtomicInteger index,
			Datafield childData) {
		String name = childData.getName();
		Optional<FieldDef> childField;
		List<FieldDef> childFieldDefs = fieldDef.getFields();
		if (fieldData.getType().equals(org.appng.xml.platform.FieldType.LIST_OBJECT)) {
			childField = childFieldDefs.stream().filter(originalField -> {
				String indexedName = getIndexedName(originalField.getName(), index.getAndIncrement());
				return indexedName.equals(name);
			}).findFirst();
		} else {
			childField = childFieldDefs.stream().filter(originalField -> originalField.getName().equals(name))
					.findFirst();
		}
		return childField;
	}

	private String getIndexedName(String name, int index) {
		return name.replaceAll("\\[\\]", "[" + index + "]");
	}

	protected Object getObjectValue(ApplicationRequest request, FieldDef fieldDef, Datafield fieldData,
			BeanWrapper beanWrapper, int index) {
		if (org.appng.xml.platform.FieldType.DATE.equals(fieldDef.getType())) {
			return fieldData.getValue();
		}
		if (!org.appng.xml.platform.FieldType.LIST_OBJECT.equals(fieldDef.getType())
				&& !org.appng.xml.platform.FieldType.OBJECT.equals(fieldDef.getType())) {

			FieldWrapper fieldwrapper = new FieldWrapper(fieldDef, beanWrapper);
			String indexedName = getIndexedName(fieldDef.getBinding(), index);
			fieldwrapper.setBinding(indexedName);
			fieldConversionFactory.setObject(fieldwrapper, new RequestContainer() {

				public boolean hasParameter(String name) {
					return name.equals(fieldDef.getBinding());
				}

				public String getParameter(String name) {
					return fieldData.getValue();
				}

				public Map<String, List<String>> getParametersList() {
					return null;
				}

				public Map<String, String> getParameters() {
					return null;
				}

				public Set<String> getParameterNames() {
					return null;
				}

				public List<String> getParameterList(String name) {
					return null;
				}

				public String getHost() {
					return request.getHost();
				}

				public List<FormUpload> getFormUploads(String name) {
					return null;
				}

				public Map<String, List<FormUpload>> getFormUploads() {
					return null;
				}
			});
			return fieldwrapper.getObject();
		}
		return null;
	}
}
