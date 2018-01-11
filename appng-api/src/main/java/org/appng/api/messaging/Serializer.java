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
package org.appng.api.messaging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.SiteAwareObjectInputStream;
import org.appng.api.support.SiteClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class helping to serialize/deserialize {@link Event}s to an {@link OutputStream}/ from an {@link InputStream}
 * . Uses a {@link SiteAwareObjectInputStream} internally, so the right {@link SiteClassLoader} is applied when
 * deserializing.
 * 
 * @author Matthias Müller
 * 
 * @see Event
 */
public class Serializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Serializer.class);
	private Environment environment;
	private String nodeId;

	/**
	 * Creates a new serializer
	 * 
	 * @param environment
	 *            the {@link Environment} to use
	 * @param nodeId
	 *            the node id to use
	 */
	Serializer(Environment environment, String nodeId) {
		this.environment = environment;
		this.nodeId = nodeId;
	}

	/**
	 * Serializes the given {@link Event} into the given {@link OutputStream}.
	 * 
	 * @param out
	 *            the {@link OutputStream} to serialized the {@link Event} into
	 * @param event
	 *            the {@link Event} to be serialized
	 * @throws IOException
	 *             if an error occurs during serialization
	 */
	public void serialize(OutputStream out, Event event) throws IOException {
		ObjectOutputStream oos = null;
		try {
			event.setNodeId(getNodeId());
			oos = new ObjectOutputStream(out);
			oos.writeObject(event.getSiteName());
			oos.writeObject(event);
			oos.flush();
		} finally {
			IOUtils.closeQuietly(oos);
		}
	}

	/**
	 * Deserializes an {@link Event} from the given data.
	 * 
	 * @param data
	 *            the bytes representing the serialized {@link Event}
	 * @return the {@link Event}, or {@code null} if no event could be deserialized from the given data
	 */
	public Event deserialize(byte[] data) {
		return deserialize(new ByteArrayInputStream(data));
	}

	/**
	 * Deserializes an {@link Event} from the given data.
	 * 
	 * @param data
	 *            the {@link InputStream} containing the serialized {@link Event}
	 * @return the {@link Event}, or {@code null} if no event could be deserialized from the given data
	 */
	public Event deserialize(InputStream data) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Event event = null;
		SiteAwareObjectInputStream oos = null;
		try {
			oos = new SiteAwareObjectInputStream(data, environment);
			String siteName = (String) oos.readObject();
			if (null != siteName) {
				LOGGER.debug("deserializing event for site {}", siteName);
				ClassLoader siteClassloader = oos.getSiteClassloader(siteName);
				if (null != siteClassloader) {
					Thread.currentThread().setContextClassLoader(siteClassloader);
					LOGGER.debug("using classloader: {}", siteClassloader);
				} else {
					LOGGER.warn("no classloader found for site {}", siteName);
				}
			} else {
				LOGGER.warn("no site given for event!");
			}
			event = (Event) oos.readObject();
		} catch (IOException | ClassNotFoundException e) {
			LOGGER.error("error while deserializing event", e);
		} finally {
			IOUtils.closeQuietly(oos);
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
		return event;
	}

	/**
	 * Returns the {@link Site} with the given name
	 * 
	 * @param siteName
	 *            the name of the site to retrieve
	 * @return the {@link Site}
	 */
	public Site getSite(String siteName) {
		return getSiteMap().get(siteName);
	}

	/**
	 * Returns to node id used when creating this serializer
	 * 
	 * @return the node id
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * Returns the {@link Environment} that was used when creating this serializer.
	 * 
	 * @return the {@link Environment}
	 */
	public Environment getEnvironment() {
		return environment;
	}

	/**
	 * Return the platform configuration as {@link Properties}.
	 * 
	 * @return the platform configuration
	 */
	public Properties getPlatformConfig() {
		return environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
	}

	Map<String, Site> getSiteMap() {
		return environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
	}

}
