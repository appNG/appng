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
package org.appng.api.support;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.ResourceBundle;

import org.springframework.core.SmartClassLoader;
import org.springframework.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SiteClassLoader extends URLClassLoader implements SmartClassLoader {

	private final String site;

	public SiteClassLoader(URL[] urls, ClassLoader parent, String site) {
		super(urls, parent);
		this.site = site;
		LOGGER.info("{} created", this);
		cleanup();
	}

	public SiteClassLoader(String site) {
		this(new URL[0], SiteClassLoader.class.getClassLoader(), site);
	}

	@Override
	/* for simpler debugging */
	protected void finalize() throws Throwable {
		super.finalize();
	}

	@Override
	/* for simpler debugging */
	public void close() throws IOException {
		super.close();
		ResourceBundle.clearCache(this);
	}

	private void cleanup() {
		// use this SiteClassloader for existing ProtectionDomain referencing an outdated SiteClassloader
		AccessControlContext acc = getFieldValue(URLClassLoader.class, "acc", this);
		ProtectionDomain[] context = getFieldValue(AccessControlContext.class, "context", acc);
		for (ProtectionDomain protectionDomain : context) {
			ClassLoader classLoader = protectionDomain.getClassLoader();
			if (isSameSite(classLoader)) {
				setFieldValue(ProtectionDomain.class, "classloader", protectionDomain, this);
				LOGGER.debug("changed {} which referenced the outdated {}", protectionDomain, classLoader);
			}
		}

	}

	private <T> T getFieldValue(Class<?> type, String name, Object target) {
		Field field = getField(type, name);
		return getValue(field, target);
	}

	private void setFieldValue(Class<?> type, String name, Object target, Object value) {
		Field field = getField(type, name);
		ReflectionUtils.setField(field, target, value);
	}

	@SuppressWarnings("unchecked")
	private <T> T getValue(Field field, Object target) {
		return (T) ReflectionUtils.getField(field, target);
	}

	private Field getField(Class<?> type, String name) {
		Field field = ReflectionUtils.findField(type, name);
		ReflectionUtils.makeAccessible(field);
		return field;
	}

	private boolean isSameSite(ClassLoader classLoader) {
		return null != classLoader && classLoader instanceof SiteClassLoader
				&& this.site.equals(((SiteClassLoader) classLoader).site);
	}

	// avoids caching of cglib proxy classes
	public boolean isClassReloadable(Class<?> clazz) {
		return true;
	}

	public String getSiteName() {
		return site;
	}

	@Override
	public String toString() {
		return "SiteClassLoader#" + hashCode() + " for site " + site + " with parent "
				+ getParent().getClass().getName() + "#" + getParent().hashCode();
	}

}
