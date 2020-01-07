/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.api.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import org.appng.xml.application.ApplicationInfo;

/**
 * 
 * A container providing easy access to the a {@link Application}s multiple {@link Resource}s.
 * 
 * @author Matthias Müller
 * 
 * @see Resource
 * @see Application
 */
public interface Resources extends Closeable {

	/**
	 * Returns all {@link Resource}s of the given {@link ResourceType}.
	 * 
	 * @param type
	 *            the ResourceType
	 * @return the {@link Resource}s of the given {@link ResourceType}
	 */
	Set<Resource> getResources(ResourceType type);

	/**
	 * Writes all {@link Resource}s to the local caching location.
	 * 
	 * @param types
	 *            the types to write the cachefiles for
	 * @see Resource#getCachedFile()
	 */
	void dumpToCache(ResourceType... types);

	/**
	 * Returns the {@link Resource} of the given type with the given name, if any.
	 * 
	 * @param type
	 *            the {@link ResourceType} of the {@link Resource}
	 * @param fileName
	 *            the name of the {@link Resource}
	 * @return the {@link Resource}, or {@code null} if no such {@link Resource} exists.
	 */
	Resource getResource(ResourceType type, String fileName);

	/**
	 * Returns the underlying {@link Resource}
	 * 
	 * @return the underlying {@link Resource}s
	 */
	Set<Resource> getResources();

	/**
	 * Returns the {@link Resource} with the given ID.
	 * 
	 * @param id
	 *            the ID of the {@link Resource}
	 * @return the {@link Resource}, or {@code null} if no such resource exists.
	 */
	Resource getResource(Integer id);

	/**
	 * Returns the {@link ApplicationInfo}, read from the {@link Application}'s {@code application-info.xml}.
	 * 
	 * @return the {@link ApplicationInfo}
	 */
	ApplicationInfo getApplicationInfo();

	void close() throws IOException;

}
