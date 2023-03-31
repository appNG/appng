/*
 * Copyright 2011-2023 the original author or authors.
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

import javax.servlet.ServletContext;

import org.appng.api.Scope;
import org.appng.api.model.Site;

/**
 * A {@link ScopedEnvironment} for {@link Scope#SITE}. Uses a {@link ServletContext} for storing its attributes, with a
 * {@link Site}s name as additional identifier.
 * 
 * @author Matthias Müller
 */
class SiteEnvironment extends PlatformEnvironment {

	static final String SITE = "site";

	SiteEnvironment(ServletContext ctx, Site site) {
		super(ctx, Scope.SITE.forSite(site.getName()));
		setAttribute(SITE, site);
	}

	public Scope getScope() {
		return Scope.SITE;
	}

	static String remove(ServletContext ctx, Site site) {
		String identifier = Scope.SITE.forSite(site.getName());
		ctx.removeAttribute(identifier);
		return identifier;
	}

}
