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
package org.appng.core.model;

import java.io.File;
import java.io.IOException;

import org.appng.api.model.Application;
import org.appng.core.xml.repository.PackageType;
import org.appng.xml.application.PackageInfo;

/**
 * An ApplicationArchive contains informations about a {@link Application} in a {@link Repository}. Those informations
 * are obtained from the {@link Application}'s ZIP-file.
 * 
 * @author Matthias Herlitzius
 */
public interface PackageArchive {

	/**
	 * Checks whether this is a valid {@link PackageArchive}, meaning it's ZIP-file has a valid structure and contains a
	 * valid {@value org.appng.api.model.ResourceType#APPLICATION_XML_NAME}.
	 * 
	 * @return {@code true} if this a valid {@link PackageArchive}, {@code false} otherwise
	 */
	boolean isValid();

	/**
	 * Returns a {@link PackageInfo} unmarshalled from the archive's
	 * {@value org.appng.api.model.ResourceType#APPLICATION_XML_NAME}. This methods returns {@code null}, if
	 * {@link #isValid()} returns {@code false}.
	 * 
	 * @return a {@link PackageInfo}
	 */
	PackageInfo getPackageInfo();

	/**
	 * Processes the {@link PackageArchive}'s ZIP-file with the given {@link ZipFileProcessor}.
	 * 
	 * @param processor
	 *                  a {@link ZipFileProcessor}
	 * @param <T>
	 *                  the return type of the {@link ZipFileProcessor}
	 * 
	 * @return an object of type {@code <T>}
	 * 
	 * @throws IOException
	 *                     <ul>
	 *                     <li>if {@link ZipFileProcessor#process(org.apache.commons.compress.archivers.zip.ZipFile)}
	 *                     throws an {@link IOException}
	 *                     <li>if the ZIP-file could not be read
	 *                     </ul>
	 */
	<T> T processZipFile(ZipFileProcessor<T> processor) throws IOException;

	/**
	 * Returns the archive's ZIP-file.
	 * 
	 * @return the file
	 */
	File getFile();

	/**
	 * Returns the binary data of the archive's ZIP-file. Should only be called if {@link #isValid()} returns
	 * {@code true}.
	 * 
	 * @return the binary data
	 * 
	 * @throws IOException
	 *                     if the ZIP-file could not be read
	 */
	byte[] getBytes() throws IOException;

	/**
	 * Returns the {@link PackageType} for this {@link PackageArchive}.
	 * 
	 * @return the {@link PackageType}
	 */
	PackageType getType();

	/**
	 * Returns the checksum for this archive (SHA256).
	 * 
	 * @return the checksum
	 */
	String getChecksum();
}
