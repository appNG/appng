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
package org.appng.api.support.validation;

import java.util.Collection;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.appng.api.FileUpload;
import org.appng.forms.FormUpload;

/**
 * Checks that a {@link Collection} of {@link FormUpload}s matches the restrictions given by a {@link FileUpload}.
 * 
 * @author Matthias Müller
 */
public class FileUploadListValidator implements ConstraintValidator<FileUpload, Collection<FormUpload>> {

	private FileUploadValidator fileValidator;

	@Override
	public void initialize(FileUpload constraintAnnotation) {
		this.fileValidator = new FileUploadValidator();
		fileValidator.initialize(constraintAnnotation);
	}

	public boolean isValid(Collection<FormUpload> uploads, ConstraintValidatorContext context) {
		if (null == uploads) {
			return fileValidator.file.minCount() < 1;
		}
		int size = uploads.size();
		for (FormUpload upload : uploads) {
			if (!fileValidator.isValid(upload, context)) {
				return false;
			}
		}
		return fileValidator.file.minCount() <= size && fileValidator.file.maxCount() >= size;
	}
}
