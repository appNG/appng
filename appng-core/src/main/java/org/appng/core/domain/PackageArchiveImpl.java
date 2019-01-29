/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.core.domain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.ResourceType;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.ZipFileProcessor;
import org.appng.core.service.TemplateService;
import org.appng.core.xml.repository.PackageType;
import org.appng.xml.MarshallService;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.application.PackageInfo;
import org.appng.xml.application.Template;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link PackageArchive}.
 * 
 * @author Matthias Herlitzius
 * 
 */
@Slf4j
public class PackageArchiveImpl implements PackageArchive {

	private static final String ZIP = ".zip";
	private static final char SEPARATOR = '-';

	private Boolean isValid = false;
	private Boolean strict = true;
	private PackageInfo packageInfo = null;
	private File file = null;
	private String originalFilename;
	private PackageType type;
	private String checksum;

	public PackageArchiveImpl(File archive, boolean strict) {
		if (null != archive && archive.isFile()) {
			this.file = archive;
			this.originalFilename = archive.getName();
			this.strict = strict;
			init();
		}
	}

	public PackageArchiveImpl(File archive, final String originalFilename) {
		if (null != archive && archive.isFile() && StringUtils.isNotBlank(originalFilename)) {
			this.file = archive;
			this.originalFilename = originalFilename;
			init();
		}
	}

	private void init() {
		LOGGER.debug("reading package file '{}' with originalFilename '{}'", file.getAbsolutePath(), originalFilename);
		ZipFileProcessor<Boolean> applicationValidationProcessor = getValidationProcessor();
		try {
			if (processZipFile(applicationValidationProcessor)) {
				this.isValid = true;
				this.type = PackageType.APPLICATION;
			} else if (checkTemplateValid()) {
				this.isValid = true;
				this.type = PackageType.TEMPLATE;
			}
			if (isValid()) {
				this.checksum = DigestUtils.sha256Hex(FileUtils.readFileToByteArray(file));
			}
		} catch (IOException e) {
			LOGGER.warn(String.format("invalid archive: %s", toString()), e);
		}
	}

	private ZipFileProcessor<Boolean> getValidationProcessor() {
		ZipFileProcessor<Boolean> validationProcessor = new ZipFileProcessor<Boolean>() {
			public Boolean process(ZipFile zipFile) throws IOException {
				ZipArchiveEntry zipEntry = zipFile.getEntry(ResourceType.APPLICATION_XML_NAME);
				if (null != zipEntry) {
					InputStream zipStream = zipFile.getInputStream(zipEntry);
					try {
						packageInfo = (PackageInfo) MarshallService.getApplicationMarshallService()
								.unmarshall(zipStream);
						if (validateFilename(packageInfo)) {
							return true;
						} else {
							LOGGER.debug(
									"'{}' is not a valid application, as the filename does not match to the one in {}",
									originalFilename, ResourceType.APPLICATION_XML_NAME);
						}
					} catch (JAXBException e) {
						LOGGER.trace("error while unmarshalling", e);
						LOGGER.debug("'{}' contains an invalid {}", originalFilename,
								ResourceType.APPLICATION_XML_NAME);
					}
				} else {
					LOGGER.debug("'{}' is not a valid appNG application, {} missing!", originalFilename,
							ResourceType.APPLICATION_XML_NAME);
				}
				return false;
			}
		};
		return validationProcessor;
	}

	private Boolean checkTemplateValid() throws IOException {
		ZipFileProcessor<Template> templateProcessor = TemplateService.getTemplateInfo(originalFilename);
		this.packageInfo = processZipFile(templateProcessor);
		return null != packageInfo;
	}

	public <T> T processZipFile(ZipFileProcessor<T> processor) throws IOException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			return processor.process(zipFile);
		} catch (IOException e) {
			throw e;
		} finally {
			if (null == packageInfo) {
				isValid = false;
			}
			ZipFile.closeQuietly(zipFile);
		}
	}

	public boolean isValid() {
		return isValid;
	}

	public PackageInfo getPackageInfo() {
		return packageInfo;
	}

	public String getFilePrefix() {
		return getFilePrefix(packageInfo.getName(), packageInfo.getVersion());
	}

	public byte[] getBytes() throws IOException {
		return FileUtils.readFileToByteArray(file);
	}

	public static String getFilePrefix(String applicationName, String applicationVersion) {
		return applicationName + SEPARATOR + applicationVersion;
	}

	public static String getFileName(String applicationName, String applicationVersion, String applicationTimestamp) {
		return getFilePrefix(applicationName, applicationVersion) + SEPARATOR + applicationTimestamp + ZIP;
	}

	/**
	 * Validates the filename against information in {@code application-info.xml}.
	 * 
	 * @param applicationInfo
	 *            a {@link ApplicationInfo}
	 * @return {@code true} if the file name of this {@link PackageArchive} matches the expected one, {@code false}
	 *         otherwise
	 */
	private boolean validateFilename(PackageInfo packageInfo) {
		boolean equalsStrict = originalFilename.equals(getFilePrefix() + SEPARATOR + packageInfo.getTimestamp() + ZIP);
		if (strict || equalsStrict) {
			return equalsStrict;
		}
		return originalFilename.equals(getFilePrefix() + ZIP);
	}

	public PackageType getType() {
		return type;
	}

	public String getChecksum() {
		return checksum;
	}

	@Override
	public String toString() {
		if (null != packageInfo) {
			return packageInfo.getName() + "-" + packageInfo.getVersion() + "-" + packageInfo.getTimestamp();
		}
		return "<invalid " + (null == originalFilename ? "" : originalFilename) + ">";
	}

}
