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
package org.appng.api.support.environment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.appng.api.Scope;

/**
 * A {@link ScopedEnvironment} for {@link Scope#REQUEST}. Uses a {@link ServletRequest} for storing its attributes.
 * 
 * @author Matthias Müller
 */
class RequestEnvironment extends AbstractEnvironment {

	private ServletRequest request;
	private ServletResponse response;

	RequestEnvironment(ServletRequest request, ServletResponse response) {
		super(Scope.REQUEST);
		this.request = request;
		this.response = response;
	}

	@SuppressWarnings("unchecked")
	public ConcurrentMap<String, Object> getContainer() {
		Object container = request.getAttribute(getIdentifier());
		if (null == container) {
			container = new ConcurrentHashMap<String, Object>();
			request.setAttribute(getIdentifier(), container);
		}
		return (ConcurrentMap<String, Object>) container;
	}

	public ServletRequest getServletRequest() {
		return request;
	}

	public ServletResponse getServletResponse() {
		return response;
	}

}
