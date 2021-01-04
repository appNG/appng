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
package org.appng.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.appng.api.model.Application;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.core.domain.ResourceImpl;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.ZipFileProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link ZipFileProcessor} to be processed by a {@link PackageArchive}.
 * 
 * @author Matthias Müller
 * 
 * @see PackageArchive#processZipFile(ZipFileProcessor)
 */
@Slf4j
public class ApplicationArchiveProcessor implements ZipFileProcessor<List<Resource>> {

	private static final String SLASH = "/";
	private Application application;
	private List<Resource> resources = new ArrayList<>();

	public ApplicationArchiveProcessor(Application application) {
		this.application = application;
	}

	private ZipFile zipFile;

	/**
	 * Processes the {@link ZipFile}.
	 * 
	 * @param zipFile
	 *            a {@link ZipFile} representing a {@link PackageArchive}.
	 * @return a list of {@link Resource}s contained in the {@link PackageArchive}
	 */
	public List<Resource> process(ZipFile zipFile) throws IOException {
		this.zipFile = zipFile;
		Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
		while (entries.hasMoreElements()) {
			ZipArchiveEntry zipArchiveEntry = entries.nextElement();
			if (!zipArchiveEntry.isDirectory()) {
				add(zipArchiveEntry);
			}
		}
		return resources;
	}

	private void add(ZipArchiveEntry entry) throws IOException, ZipException {
		String path = entry.getName();
		String[] pathParts = path.split(SLASH);
		if (ResourceType.BEANS_XML_NAME.equals(path)) {
			add(entry, ResourceType.BEANS_XML, ResourceType.BEANS_XML_NAME);
		} else if (ResourceType.APPLICATION_XML_NAME.equals(path)) {
			add(entry, ResourceType.APPLICATION, ResourceType.APPLICATION_XML_NAME);
		} else if (pathParts.length >= 2) {
			String dirName = pathParts[0];
			String fileName = path.substring(path.indexOf(SLASH) + 1);
			for (ResourceType type : ResourceType.values()) {
				boolean isdir = type.getFolder().equals(dirName);
				boolean validFileName = type.isValidFileName(fileName);
				if (isdir && validFileName) {
					add(entry, type, fileName);
					break;
				}
			}
		}
	}

	private void add(ZipArchiveEntry entry, ResourceType type, String fileName) throws IOException, ZipException {
		InputStream content = zipFile.getInputStream(entry);
		byte[] bytes = IOUtils.toByteArray(content);
		ResourceImpl applicationResource = new ResourceImpl();
		applicationResource.setBytes(bytes);
		applicationResource.setResourceType(type);
		applicationResource.setDescription("");
		applicationResource.setName(fileName);
		applicationResource.setApplication(application);
		applicationResource.setVersion(entry.getLastModifiedDate());
		applicationResource.calculateChecksum();
		LOGGER.info("adding application-resource '{}' for application '{}-{}'", fileName, application.getName(),
				application.getPackageVersion());
		resources.add(applicationResource);
	}
}
