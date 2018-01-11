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
package org.appng.core.model;

import java.util.ArrayList;
import java.util.List;

import org.appng.api.model.ApplicationSubject;
import org.appng.api.model.Role;

/**
 * Default {@link ApplicationSubject} implementation.
 * 
 * @author Matthias Müller
 */
public class ApplicationSubjectImpl implements ApplicationSubject {

	private final String authName;
	private final String realName;
	private final String language;
	private final String timeZone;
	private final String email;
	private List<Role> applicationRoles;

	public ApplicationSubjectImpl(String authName, String realName, String email, String language, String timeZone) {
		this.authName = authName;
		this.realName = realName;
		this.language = language;
		this.timeZone = timeZone;
		this.email = email;
		this.applicationRoles = new ArrayList<Role>();
	}

	public String getAuthName() {
		return authName;
	}

	public String getRealname() {
		return realName;
	}

	public String getLanguage() {
		return language;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public String getEmail() {
		return email;
	}

	public List<Role> getRoles() {
		return applicationRoles;
	}

}
