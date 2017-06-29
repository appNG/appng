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
package org.appng.api.support;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.model.Application;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.xml.MarshallService;
import org.appng.xml.MarshallService.AppNGSchema;
import org.appng.xml.application.ApplicationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link Resources}-implementation
 * 
 * @author Matthias Herlitzius
 * 
 */
public class ApplicationResourceHolder implements Resources {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationResourceHolder.class);

	private static final String APPLICATION_XML_MISSING = ResourceType.APPLICATION_XML_NAME + " missing";

	private Map<Integer, Resource> idMap;
	private Map<ResourceType, Map<String, Resource>> storage = new HashMap<ResourceType, Map<String, Resource>>();
	private Application application;
	private File applicationFolder;
	private File outputFolder;
	private ApplicationInfo applicationInfo;

	/**
	 * Creates a new {@link ApplicationResourceHolder}
	 * 
	 * @param application
	 *            the {@link Application} that owns the {@link Resources}
	 * @param marshallService
	 *            a {@link MarshallService} using {@link AppNGSchema#APPLICATION}
	 * @param applicationFolder
	 *            the location of the {@link Application}'s {@link Resource}s on disc, only needed if the
	 *            {@link Application} is filebased
	 * @param outputFolder
	 *            the output folder for storing the cached resources of the {@link Application}
	 * @throws InvalidConfigurationException
	 *             if no valid application-info.xml could be found
	 * @see Application#getResourceSet()
	 * @see Application#isFileBased()
	 * @see #dumpToCache(ResourceType...)
	 */
	public ApplicationResourceHolder(Application application, MarshallService marshallService, File applicationFolder,
			File outputFolder) throws InvalidConfigurationException {
		this.application = application;
		this.applicationFolder = applicationFolder;
		this.outputFolder = outputFolder;
		idMap = new HashMap<Integer, Resource>();
		for (ResourceType type : ResourceType.values()) {
			storage.put(type, new HashMap<String, Resource>());
		}
		load();
		Resource applicationResource = getResource(ResourceType.APPLICATION, ResourceType.APPLICATION_XML_NAME);
		if (null == applicationResource) {
			throw new InvalidConfigurationException(application.getName(), APPLICATION_XML_MISSING);
		}
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(applicationResource.getBytes());
			this.applicationInfo = marshallService.unmarshall(in, ApplicationInfo.class);
		} catch (JAXBException e) {
			throw new InvalidConfigurationException(application.getName(), APPLICATION_XML_MISSING, e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private void add(ResourceType type, Resource applicationResource) {
		if (!hasBeansXmlAdded(type)) {
			storage.get(type).put(applicationResource.getName(), applicationResource);
			if (application.isFileBased()) {
				idMap.put(applicationResource.getName().hashCode(), applicationResource);
			} else {
				idMap.put(applicationResource.getId(), applicationResource);
			}
			LOGGER.debug("Resource {} of type {} has been added.", applicationResource.getName(),
					applicationResource.getResourceType());
		}
	}

	private boolean hasBeansXmlAdded(ResourceType type) {
		if (ResourceType.BEANS_XML.equals(type) && (0 != storage.get(type).size())) {
			LOGGER.warn("Resource of type {} is skipped, because a resource of this type has already been added.",
					type);
			return true;
		}
		return false;
	}

	public Set<Resource> getResources(ResourceType type) {
		return Collections.unmodifiableSet(new HashSet<Resource>(storage.get(type).values()));
	}

	private File getCacheDirectory(ResourceType type) {
		File cacheDirectory = new File(outputFolder, type.getFolder());
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdirs();
		}
		return cacheDirectory;
	}

	public void dumpToCache(ResourceType... types) {
		for (ResourceType type : types) {
			File cacheDirectory = getCacheDirectory(type);
			FileUtils.deleteQuietly(cacheDirectory);
			for (Resource resource : getResources(type)) {
				FileOutputStream fos = null;
				try {
					String proposedChecksum = DigestUtils.sha256Hex(resource.getBytes());
					if (!proposedChecksum.equals(resource.getCheckSum())) {
						throw new IOException(
								String.format("the checksum for applicationresource#%s (%s) did not match!",
										resource.getId(), resource.getName()));
					}
					File cachedFile = new File(cacheDirectory, resource.getName()).getAbsoluteFile();
					if (cachedFile.exists()) {
						FileUtils.deleteQuietly(cachedFile);
					} else {
						File parentFolder = cachedFile.getParentFile();
						if (!parentFolder.exists()) {
							FileUtils.forceMkdir(parentFolder);
						}
						cachedFile.createNewFile();
					}
					fos = new FileOutputStream(cachedFile);
					fos.write(resource.getBytes());
					LOGGER.debug("writing {} to {}", resource.getName(), cachedFile.getAbsolutePath());
					resource.setCachedFile(cachedFile);
				} catch (IOException e) {
					LOGGER.error("Error while dumping " + resource.getName(), e);
				} finally {
					IOUtils.closeQuietly(fos);
				}
			}
		}
	}

	public Resource getResource(ResourceType type, String fileName) {
		return storage.get(type).get(fileName);
	}

	public Set<Resource> getResources() {
		Set<Resource> resources = new HashSet<Resource>();
		for (ResourceType type : ResourceType.values()) {
			resources.addAll(storage.get(type).values());
		}
		return Collections.unmodifiableSet(resources);
	}

	private void load() throws InvalidConfigurationException {
		if (application.isFileBased()) {
			loadFilebasedApplication();
		} else {
			loadDatabasedApplication();
		}
	}

	private Resources loadDatabasedApplication() {
		for (Resource applicationResource : application.getResourceSet()) {
			add(applicationResource.getResourceType(), applicationResource);
		}
		return this;
	}

	private Resources loadFilebasedApplication() throws InvalidConfigurationException {
		if (applicationFolder.exists()) {
			for (ResourceType type : ResourceType.values()) {
				loadFileResources(type);
			}
			return this;
		} else {
			throw new InvalidConfigurationException(application.getName(), String.format(
					"application %s not found at %s", application.getName(), applicationFolder.getAbsolutePath()));
		}
	}

	private void loadFileResources(ResourceType type) throws InvalidConfigurationException {
		Set<String> allowedFileEndings = type.getAllowedFileEndings();
		File typeRootFolder = new File(applicationFolder, type.getFolder());
		if (typeRootFolder.exists()) {
			Collection<File> files = new ArrayList<File>();
			if (type.supportsSubfolders()) {
				String[] fileExtensions = null;
				if (!allowedFileEndings.isEmpty()) {
					fileExtensions = allowedFileEndings.toArray(new String[allowedFileEndings.size()]);
				}
				Collection<File> listFiles = FileUtils.listFiles(typeRootFolder, fileExtensions, true);
				files.addAll(listFiles);

			} else {
				File[] listFiles = typeRootFolder.listFiles(type);
				if (null != listFiles) {
					files.addAll(Arrays.asList(listFiles));
				}
			}

			for (File file : files) {
				try {
					byte[] binary = FileUtils.readFileToByteArray(file);
					String relativePath = file.getPath().substring(typeRootFolder.getPath().length() + 1);
					String normalized = FilenameUtils.normalize(relativePath, true);
					Resource applicationResource = new SimpleResource(type, binary, normalized);
					add(type, applicationResource);
				} catch (IOException e) {
					throw new InvalidConfigurationException(application.getName(), "Error while reading file "
							+ file.getName(), e);
				}
			}
		}
	}

	class SimpleResource implements Resource, Closeable {

		private ResourceType type;
		private byte[] data;
		private String name;
		private File cachedFile;

		SimpleResource(ResourceType type, byte[] data, String name) {
			this.type = type;
			this.data = data;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return null;
		}

		public Integer getId() {
			return name.hashCode();
		}

		public ResourceType getResourceType() {
			return type;
		}

		public byte[] getBytes() {
			return data;
		}

		public int getSize() {
			return null == data ? 0 : data.length;
		}

		public File getCachedFile() {
			return cachedFile;
		}

		public void setCachedFile(File cachedFile) {
			this.cachedFile = cachedFile;
		}

		public String getCheckSum() {
			return DigestUtils.sha256Hex(getBytes());
		}

		public void close() {
			this.data = null;
			this.cachedFile = null;
			this.type = null;
			this.name = null;
		}

	}

	public Resource getResource(Integer id) {
		return idMap.get(id);
	}

	public ApplicationInfo getApplicationInfo() {
		return applicationInfo;
	}

	public void close() throws IOException {
		if (null != idMap) {
			for (Integer key : idMap.keySet()) {
				((Closeable) idMap.get(key)).close();
			}
			idMap.clear();
			idMap = null;
		}
		if (null != storage) {
			for (ResourceType key : storage.keySet()) {
				storage.get(key).clear();
			}
			storage.clear();
			storage = null;
		}
		application = null;
		applicationFolder = null;
		outputFolder = null;
		applicationInfo = null;
	}

}
