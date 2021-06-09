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
package org.appng.forms.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.appng.forms.FormUpload;
import org.appng.forms.FormUploadValidator;

import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link FormUpload} implementation.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class FormUploadBean implements FormUpload {

	private String originalName;
	private File file;
	private String contentType;
	private List<String> acceptedTypes;
	private long maxSize = -1L;
	private long minSize = 0L;

	public FormUploadBean(File file, String originalName, String contentType, List<String> acceptedTypes,
			long maxSize) {
		this.file = file;
		this.originalName = FilenameUtils.getName(originalName);
		this.contentType = contentType;
		this.acceptedTypes = acceptedTypes;
		this.maxSize = maxSize;
	}

	public List<String> getAcceptedTypes() {
		return Collections.unmodifiableList(acceptedTypes);
	}

	public String getOriginalFilename() {
		return originalName;
	}

	public File getFile() {
		if (!isValidFile()) {
			return null;
		}
		return file;
	}

	public long size() {
		if (null == file) {
			return 0;
		}
		return file.length();
	}

	public long getMaxSize() {
		return maxSize;
	}

	public long getMinSize() {
		return minSize;
	}

	public byte[] getBytes() {
		if (!isValidFile()) {
			return null;
		}
		try {
			return FileUtils.readFileToByteArray(getFile());
		} catch (IOException e) {
			LOGGER.error("error while reading file", e);
		}
		return null;
	}

	public boolean isValidSize() {
		return (maxSize == -1L || maxSize >= file.length()) && file.length() >= minSize;
	}

	public boolean isValid() {
		return isValidFile() && isValidType() && isValidSize() && isValidName();
	}

	private boolean isValidName() {
		return null != originalName && !"".equals(originalName);
	}

	public boolean isValid(String[] types, long minSize, long maxSize) {
		this.acceptedTypes = Arrays.asList(types);
		this.maxSize = maxSize;
		this.minSize = minSize;
		return isValid();
	}

	public boolean isValid(FormUploadValidator validator) {
		return validator.isValid(this);
	}

	public boolean isValid(Class<? extends FormUploadValidator> validatorClass) {
		FormUploadValidator validator;
		try {
			validator = validatorClass.newInstance();
			return isValid(validator);
		} catch (Exception e) {
			LOGGER.error(String.format("unable to instanciate validator class '%s'", validatorClass.getName()), e);
		}
		return true;
	}

	public boolean isValidFile() {
		return file != null && file.exists() && file.isFile();
	}

	public boolean isValidType() {
		String extension = FilenameUtils.getExtension(getOriginalFilename());
		if (null != extension) {
			extension = extension.toLowerCase();
		}
		return (acceptedTypes == null || acceptedTypes.size() == 0) || acceptedTypes.indexOf(extension) > -1
				|| acceptedTypes.indexOf(getContentType()) > -1;
	}

	public String getContentType() {
		return contentType;
	}

	public String toString() {
		return file.getAbsolutePath() + " (size " + file.length() + ", type: " + contentType + ", original name:"
				+ originalName + ")";
	}

}
