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
package org.appng.api.model;

import java.io.File;

/**
 * A {@link Application} provides several {@link Resource}s, which are needed to load and execute the {@link Application}. A
 * {@link Resource} is created from the physical files that come with a {@link Application}.
 * 
 * @author Matthias Herlitzius
 * 
 * @see ResourceType
 */
public interface Resource extends Named<Integer> {

	/**
	 * Returns the type of this {@link Resource}.
	 * 
	 * @return the type
	 */
	ResourceType getResourceType();

	/**
	 * Returns the byte data of the {@link Resource}.
	 * 
	 * @return the byte data
	 */
	byte[] getBytes();

	/**
	 * Returns the size (in bytes) of this {@link Resource}.
	 * 
	 * @return the size (in bytes)
	 */
	int getSize();

	/**
	 * Returns a cached version of the {@link Resource}s file.
	 * 
	 * @return the cached file
	 */
	File getCachedFile();

	/**
	 * Sets the cached version of the {@link Resource}s file.
	 * 
	 * @param cachedFile
	 *            the cached file
	 */
	void setCachedFile(File cachedFile);

	/**
	 * Returns the checksum of the byte data
	 * 
	 * @return the checksum of the byte data
	 */
	String getCheckSum();

}
