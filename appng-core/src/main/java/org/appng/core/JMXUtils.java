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
package org.appng.core;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility-class for registering/unregistering JMX-objects.
 * 
 * @author Matthias Müller
 */
public class JMXUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(JMXUtils.class);

	/**
	 * Registers the given object under the given name, if no other object is already registered under this name.
	 * 
	 * @param object
	 *            the object to register
	 * @param name
	 *            the name for the object to register
	 * @return {@code true} if the object has been registered, {@code false} otherwise (also if an exception occurred
	 *         while registering).
	 */
	public static boolean register(Object object, String name) {
		try {
			ObjectName objectName = new ObjectName(name);
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			if (!mbs.isRegistered(objectName)) {
				mbs.registerMBean(object, objectName);
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("error while registering " + name, e);
		}
		return false;
	}

	/**
	 * Unregisters the object currently registered under the given name, if existing.
	 * 
	 * @param name
	 *            the name for the object to unregister
	 * @return {@code true} if the object has been unregistered, {@code false} otherwise (also if an exception occurred
	 *         while unregistering).
	 */
	public static boolean unregister(String name) {
		try {
			ObjectName objectName = new ObjectName(name);
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			if (mbs.isRegistered(objectName)) {
				mbs.unregisterMBean(objectName);
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("error while deregistering " + name, e);
		}
		return false;
	}

	private JMXUtils() {
	}

}
