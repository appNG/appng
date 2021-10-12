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
package org.appng.api.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Map;

import javax.servlet.ServletContext;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;

/**
 * An {@link ObjectInputStream} that is aware of the <code>org.appng.core.service.SiteClassLoader</code> of an appNG
 * {@link org.appng.api.model.Site}.
 * 
 * @author Matthias Mueller
 *
 * @see org.appng.api.model.Site#getSiteClassLoader()
 */
public class SiteAwareObjectInputStream extends ObjectInputStream {

	private final Environment environment;
	private String site;

	/**
	 * Creates an {@link SiteAwareObjectInputStream}, retrieving informations about the active
	 * <code>org.appng.api.model.Site</code>s from the given {@link ServletContext}
	 * 
	 * @param is
	 *                an {@link InputStream}
	 * @param context
	 *                the {@link ServletContext}
	 * 
	 * @throws IOException
	 *                     if reading from the {@link InputStream} fails
	 */
	public SiteAwareObjectInputStream(InputStream is, ServletContext context) throws IOException {
		this(is, DefaultEnvironment.get(context));
	}

	public SiteAwareObjectInputStream(InputStream is, Environment environment) throws IOException {
		super(is);
		this.environment = environment;
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException {
		return Class.forName(objectStreamClass.getName(), false, getSiteClassloader(site));
	}

	/**
	 * Retrieves the <code>org.appng.api.model.Site</code> with the given name
	 * 
	 * @param siteName
	 *                 the name of the site
	 * 
	 * @return the <code>org.appng.api.model.Site</code>
	 */
	@SuppressWarnings("unchecked")
	public Site getSite(String siteName) {
		return ((Map<String, Site>) environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES)).get(siteName);
	}

	public ClassLoader getSiteClassloader(String siteName) {
		Site site = null == siteName ? null : getSite(siteName);
		return null == site ? Thread.currentThread().getContextClassLoader() : site.getSiteClassLoader();
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setSite(String site) {
		this.site = site;
	}

}