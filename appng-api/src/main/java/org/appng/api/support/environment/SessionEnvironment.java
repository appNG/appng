/*
 * Copyright 2011-2017 the original author or authors.
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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpSession;

import org.appng.api.Scope;
import org.appng.api.support.SiteClassLoader;

/**
 * A {@link ScopedEnvironment} for {@link Scope#SESSION}. Uses a {@link HttpSession} for storing its attributes.
 * 
 * @author Matthias Müller
 */
class SessionEnvironment extends AbstractEnvironment {

	private static final String CHANGED = "__changed__";
	private HttpSession session;
	private boolean valid;

	SessionEnvironment(HttpSession session) {
		super(Scope.SESSION);
		this.session = session;
		this.valid = true;
	}

	@SuppressWarnings("unchecked")
	public ConcurrentMap<String, Object> getContainer() {
		Object container = session.getAttribute(getIdentifier());
		if (null == container) {
			container = new ConcurrentHashMap<String, Object>();
			session.setAttribute(getIdentifier(), container);
		}
		return (ConcurrentMap<String, Object>) container;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String name) {
		Object attribute = getContainer().get(name);
		if (null != attribute && attribute instanceof AttributeWrapper) {
			attribute = ((AttributeWrapper) attribute).getValue();
		}
		return (T) attribute;
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (!Objects.equals(value, getAttribute(name))) {
			markSessionDirty();
			ClassLoader classLoader = value.getClass().getClassLoader();
			if (null != classLoader && classLoader instanceof SiteClassLoader) {
				String siteName = ((SiteClassLoader) classLoader).getSiteName();
				getContainer().put(name, new AttributeWrapper(siteName, value));
			} else {
				getContainer().put(name, value);
			}
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

}
