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
package org.appng.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@code JarInfo} provides some informations about a JAR-file, including some meta-informations retrieved from
 * {@code META-INF/MANIFEST.MF}.
 * 
 * @author Matthias Herlitzius
 */
public final class JarInfo implements Comparable<JarInfo> {

	private static final Logger log = LoggerFactory.getLogger(JarInfo.class);
	private final String fileName;
	private final String applicationName;

	private final String implementationTitle;
	private final String implementationVersion;
	private final String implementationVendorId;
	private final String implementationVendor;
	private long lastModified;
	private long length;

	private JarInfo(File fileName, String applicationName, Attributes attributes) {
		this.fileName = fileName.getName();
		this.applicationName = applicationName;
		this.lastModified = fileName.lastModified();
		this.length = fileName.length();

		this.implementationTitle = attributes.getValue(JarInfoResource.IMPLEMENTATION_TITLE.getName());
		this.implementationVersion = attributes.getValue(JarInfoResource.IMPLEMENTATION_VERSION.getName());
		this.implementationVendorId = attributes.getValue(JarInfoResource.IMPLEMENTATION_VENDOR_ID.getName());
		this.implementationVendor = attributes.getValue(JarInfoResource.IMPLEMENTATION_VENDOR.getName());
	}

	public String getFileName() {
		return fileName;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public long getLastModified() {
		return lastModified;
	}

	public Date getVersion() {
		return new Date(lastModified);
	}

	public long getLength() {
		return length;
	}

	public String getImplementationTitle() {
		return implementationTitle;
	}

	public String getImplementationVersion() {
		return implementationVersion;
	}

	public String getImplementationVendorId() {
		return implementationVendorId;
	}

	public String getImplementationVendor() {
		return implementationVendor;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (null != applicationName) {
			builder = builder.append("Application \"" + applicationName + "\" uses JAR ");
		}
		builder = builder.append(fileName + ": ");
		builder = addValue(builder, JarInfoResource.IMPLEMENTATION_TITLE, implementationTitle);
		builder = addValue(builder, JarInfoResource.IMPLEMENTATION_VERSION, implementationVersion);
		builder = addValue(builder, JarInfoResource.IMPLEMENTATION_VENDOR_ID, implementationVendorId);
		builder = addValue(builder, JarInfoResource.IMPLEMENTATION_VENDOR, implementationVendor);
		builder.setLength(builder.length() - 2);
		return builder.toString();
	}

	private StringBuilder addValue(StringBuilder builder, JarInfoResource resource, String value) {
		if (null != value) {
			return builder.append(resource.getAcronym() + ":" + value + ", ");
		}
		return builder;
	}

	private enum JarInfoResource {

		IMPLEMENTATION_TITLE("Implementation-Title", "Title"), IMPLEMENTATION_VERSION("Implementation-Version",
				"Version"), IMPLEMENTATION_VENDOR_ID("Implementation-Vendor-Id",
						"Vendor-ID"), IMPLEMENTATION_VENDOR("Implementation-Vendor", "Vendor");

		private final String name;
		private final String acronym;

		private JarInfoResource(String name, String acronym) {
			this.name = name;
			this.acronym = acronym;
		}

		private String getName() {
			return name;
		}

		private String getAcronym() {
			return acronym;
		}

	}

	public static final class JarInfoBuilder {

		public static final JarInfo getJarInfo(File jarFile) {
			return getJarInfo(jarFile, null);
		}

		public static final JarInfo getJarInfo(File jarFile, String applicationName) {
			try {
				return new JarInfo(jarFile, applicationName, readJarAttributes(jarFile));
			} catch (IOException e) {
				log.error("Error while reading JAR file: " + jarFile.getAbsolutePath(), e);
			}
			return null;
		}

		private static Attributes readJarAttributes(File jarFile) throws IOException {
			if (!jarFile.isFile()) {
				throw new IOException("File not exists or is not a file");
			}
			Attributes mainAttributes = null;
			try (
					InputStream is = new FileInputStream(jarFile);
					JarInputStream jarInputStream = new JarInputStream(is)) {
				Manifest manifest = jarInputStream.getManifest();
				if (null != manifest) {
					mainAttributes = manifest.getMainAttributes();
				} else {
					mainAttributes = new Attributes(0);
				}
			} catch (IOException e) {
				throw e;
			}
			return mainAttributes;
		}
	}

	public int compareTo(JarInfo other) {
		return getFileName().compareTo(other.getFileName());
	}

}
