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
package org.appng.api.support.environment;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.appng.api.Scope;

/**
 * A {@link ScopedEnvironment} for {@link Scope#SESSION}. Uses a {@link HttpSession} for storing its attributes.
 * 
 * @author Matthias Müller
 */
class SessionEnvironment extends AbstractEnvironment {

	private static final String CHANGED = "__changed__";
	private HttpSession session;
	private boolean valid;
	private String siteName;

	SessionEnvironment(HttpSession session, String siteName) {
		super(Scope.SESSION);
		this.session = session;
		this.valid = true;
		this.siteName = siteName;
	}

	Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(
				getContainer().keySet().stream().collect(Collectors.toMap(Function.identity(), this::getAttribute)));
	}

	@SuppressWarnings("unchecked")
	public ConcurrentMap<String, Object> getContainer() {
		Object container = session.getAttribute(getIdentifier());
		if (null == container) {
			container = new ConcurrentHashMap<>();
			session.setAttribute(getIdentifier(), container);
		}
		return (ConcurrentMap<String, Object>) container;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String name) {
		Object attribute = getContainer().get(name);
		if (null != attribute && attribute instanceof AttributeWrapper) {
			attribute = AttributeWrapper.class.cast(attribute).getValue();
		}
		return (T) attribute;
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (!Objects.equals(value, getAttribute(name))) {
			markSessionDirty();
			getContainer().put(name, new AttributeWrapper(siteName, value));
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T removeAttribute(String name) {
		if (keySet().contains(name)) {
			markSessionDirty();
			Object attribute = getContainer().remove(name);
			if (attribute instanceof AttributeWrapper) {
				attribute = ((AttributeWrapper) attribute).getValue();
			}
			return (T) attribute;
		}
		return null;
	}

	protected void markSessionDirty() {
		getHttpSession().setAttribute(CHANGED, true);
	}

	public void logout() {
		session.invalidate();
		session = null;
		valid = false;
	}

	public HttpSession getHttpSession() {
		return session;
	}

	public boolean isValid() {
		return valid;
	}

	String getSiteName() {
		return siteName;
	}
}
