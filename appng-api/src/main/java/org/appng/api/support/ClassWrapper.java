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

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;

/**
 * Provides operations to analyze a class, similar to Springs {@link BeanWrapper}, except it doesn't need an
 * instanciatable class to work on.<br/>
 * NOTE: The given Class does not have to be a JavaBean, meaning the presence of a getter does not imply the presence of
 * a setter!
 * 
 * @author Matthias Müller
 */
public final class ClassWrapper {

	private Class<?> wrappedClass;

	public ClassWrapper(Class<?> clazz) {
		this.wrappedClass = clazz;
	}

	public final boolean isWritableProperty(String property) {
		try {
			String[] splitted = property.split("\\.");
			int length = splitted.length;
			Class<?> innerClazz = wrappedClass;
			String propertyName = null;
			for (int i = 0; i < length; i++) {
				propertyName = splitted[i];
				if (i < length - 1) {
					innerClazz = getGetter(innerClazz, StringUtils.capitalize(propertyName)).getReturnType();
				} else {
					getSetter(innerClazz, propertyName);
				}
			}
			return true;
		} catch (Exception e) {
			// do nothing
		}
		return false;
	}

	public final boolean isReadableProperty(String property) {
		try {
			String[] splitted = property.split("\\.");
			Class<?> innerClazz = wrappedClass;
			for (String propertyName : splitted) {
				Method getter = getGetter(innerClazz, StringUtils.capitalize(propertyName));
				if (null == getter) {
					return false;
				}
				innerClazz = getter.getReturnType();
			}
			return true;
		} catch (Exception e) {
			// do nothing
		}
		return false;
	}

	private Method getGetter(Class<?> clazz, String property) throws SecurityException, NoSuchMethodException {
		String name = property.split("\\[")[0];
		Method method = null;
		try {
			method = clazz.getMethod("get" + StringUtils.capitalize(name), new Class[] {});
			return method;
		} catch (Exception e) {

		}
		try {
			method = clazz.getMethod("is" + StringUtils.capitalize(name), new Class[] {});
			return method;
		} catch (Exception e) {
			// do nothing
		}
		return null;
	}

	private Method getSetter(Class<?> clazz, String property) throws SecurityException, NoSuchMethodException {
		String name = property.split("\\[")[0];
		Method getter = getGetter(clazz, name);
		if (null != getter) {
			Class<?> type = getter.getReturnType();
			return clazz.getMethod("set" + StringUtils.capitalize(name), new Class[] { type });
		}
		return null;
	}

	public final Class<?> getWrappedClass() {
		return wrappedClass;
	}
}
