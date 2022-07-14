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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlatformProperties implements Properties {

	public static PlatformProperties get(Environment env) {
		return get((Properties) env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG));
	}

	public static PlatformProperties get(Properties platformConfig) {
		return new PlatformProperties(platformConfig);
	}

	private Properties properties;

	private PlatformProperties(Properties properties) {
		this.properties = properties;
	}

	public List<String> getList(String name, String defaultValue, String delimiter) {
		return properties.getList(name, defaultValue, delimiter);
	}

	public List<String> getList(String name, String delimiter) {
		return properties.getList(name, delimiter);
	}

	public String getString(String name) {
		return properties.getString(name);
	}

	public String getString(String name, String defaultValue) {
		return properties.getString(name, defaultValue);
	}

	public Boolean getBoolean(String name) {
		return properties.getBoolean(name);
	}

	public Boolean getBoolean(String name, Boolean defaultValue) {
		return properties.getBoolean(name, defaultValue);
	}

	public Integer getInteger(String name) {
		return properties.getInteger(name);
	}

	public Integer getInteger(String name, Integer defaultValue) {
		return properties.getInteger(name, defaultValue);
	}

	public Float getFloat(String name) {
		return properties.getFloat(name);
	}

	public Float getFloat(String name, Float defaultValue) {
		return properties.getFloat(name, defaultValue);
	}

	public Double getDouble(String name) {
		return properties.getDouble(name);
	}

	public Double getDouble(String name, Double defaultValue) {
		return properties.getDouble(name, defaultValue);
	}

	public String getClob(String name) {
		return properties.getClob(name);
	}

	public String getClob(String name, String defaultValue) {
		return properties.getClob(name, defaultValue);
	}

	public byte[] getBlob(String name) {
		return properties.getBlob(name);
	}

	public Set<String> getPropertyNames() {
		return properties.getPropertyNames();
	}

	public boolean propertyExists(String name) {
		return properties.propertyExists(name);
	}

	public Object getObject(String name) {
		return properties.getObject(name);
	}

	public java.util.Properties getPlainProperties() {
		return properties.getPlainProperties();
	}
	
	public java.util.Properties getProperties(String name) {
		return properties.getProperties(name);
	}

	public String getDescriptionFor(String name) {
		return properties.getDescriptionFor(name);
	}

	public InputStream getCacheConfig() throws IOException {
		String cacheConfig = getClob(Platform.Property.CACHE_CONFIG);
		if (StringUtils.isNotBlank(cacheConfig)) {
			LOGGER.info("Reading cache config from property {}", Platform.Property.CACHE_CONFIG);
			return new ByteArrayInputStream(cacheConfig.getBytes(StandardCharsets.UTF_8));
		} else {
			File cacheConfigFile = getAbsoluteFile(Platform.Property.CACHE_CONFIG);
			if (cacheConfigFile.exists()) {
				LOGGER.info("Reading caching config from {}", cacheConfigFile);
				return new FileInputStream(cacheConfigFile);
			}
			LOGGER.warn("Cache config file does not exist: {}", cacheConfigFile.getAbsoluteFile());
		}
		return null;
	}

	public File getUploadDir() {
		return getAbsoluteFile(Platform.Property.UPLOAD_DIR);
	}

	public File getApplicationDir() {
		return getAbsoluteFile(Platform.Property.APPLICATION_DIR);
	}

	public File getRepositoryRootFolder() {
		return getAbsoluteFile(Platform.Property.REPOSITORY_PATH);
	}

	private File getAbsoluteFile(String folder) {
		return Paths.get(getString(Platform.Property.APPNG_DATA), getString(folder)).normalize().toFile();
	}

}
