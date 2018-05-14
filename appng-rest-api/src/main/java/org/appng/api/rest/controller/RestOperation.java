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
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.appng.api.Environment;
import org.appng.api.model.Subject;
import org.appng.api.rest.model.ErrorModel;
import org.appng.api.rest.model.FieldType;
import org.appng.api.rest.model.Message;
import org.appng.api.rest.model.Option;
import org.appng.api.rest.model.Message.LevelEnum;
import org.appng.api.rest.model.Parameter;
import org.appng.api.rest.model.User;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.Params;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

abstract class RestOperation {

	protected User getUser(Environment environment) {
		Subject subject = environment.getSubject();
		User user = new User();
		user.setEmail(subject.getEmail());
		user.setLocale(environment.getLocale().toString());
		user.setTimezone(environment.getTimeZone().getID());
		user.setName(subject.getRealname());
		return user;
	}

	protected List<Parameter> getParameters(Params params) {
		List<Parameter> parameterList = new ArrayList<>();
		params.getParam().forEach(p -> {
			Parameter param = new Parameter();
			param.setName(p.getName());
			param.setValue(p.getValue());
			parameterList.add(param);
		});
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
}
