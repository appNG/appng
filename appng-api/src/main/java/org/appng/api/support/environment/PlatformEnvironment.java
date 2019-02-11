/*
 * Copyright 2011-2019 the original author or authors.
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

import javax.servlet.ServletContext;

import org.appng.api.Scope;

/**
 * A {@link ScopedEnvironment} for {@link Scope#PLATFORM}. Uses a {@link ServletContext} for storing its attributes.
 * 
 * @author Matthias Müller
 */
class PlatformEnvironment extends AbstractEnvironment {

	private ServletContext ctx;

	PlatformEnvironment(ServletContext ctx) {
		super(Scope.PLATFORM);
		this.ctx = ctx;
	}

	protected PlatformEnvironment(ServletContext ctx, String identifier) {
		super(identifier);
		this.ctx = ctx;
	}

	@SuppressWarnings("unchecked")
	public ConcurrentMap<String, Object> getContainer() {
		ConcurrentMap<String, Object> container = (ConcurrentMap<String, Object>) ctx.getAttribute(getIdentifier());
		if (null == container) {
			container = new ConcurrentHashMap<>();
			ctx.setAttribute(getIdentifier(), container);
		}
		return container;
	}

	public ServletContext getServletContext() {
		return ctx;
	}

}
