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
package org.appng.api.support.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.appng.api.FileUpload;
import org.appng.forms.FormUpload;
import org.appng.forms.FormUploadValidator;

/**
 * 
 * Checks that a {@link FormUpload} matches the restrictions given by a {@link FileUpload}.
 * 
 * @author Matthias Müller
 * 
 */
public class FileUploadValidator implements ConstraintValidator<FileUpload, FormUpload> {

	protected FileUpload file;

	public void initialize(FileUpload file) {
		this.file = file;
	}

	public boolean isValid(FormUpload upload, ConstraintValidatorContext constraintContext) {
		if (upload == null) {
			return file.minCount() < 1;
		}
		Class<? extends FormUploadValidator>[] validators = file.uploadValidators();
		for (Class<? extends FormUploadValidator> validatorClass : validators) {
			if (!upload.isValid(validatorClass)) {
				return false;
			}
		}
		String[] types = file.fileTypes().toLowerCase().replaceAll(" ", "").split(",");
		long factor = file.unit().getFactor();
		boolean valid = upload.isValid(types, file.minSize() * factor, file.maxSize() * factor);
		return valid;
	}
}
