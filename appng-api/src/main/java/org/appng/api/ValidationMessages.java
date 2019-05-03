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
package org.appng.api;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Utility class providing constants for the message-keys used in validation-annotations.
 * 
 * @author Matthias Müller
 * 
 */
public class ValidationMessages {

	/**
	 * Message: 'Enter at least {min} characters'
	 * 
	 * @see Size
	 */
	public static final String VALIDATION_STRING_MIN = "{validation.string.min}";

	/**
	 * Message: 'Enter at most {max} characters'
	 * 
	 * @see Size
	 */
	public static final String VALIDATION_STRING_MAX = "{validation.string.max}";

	/**
	 * Message: 'Enter {max} characters'
	 * 
	 * @see Size
	 */
	public static final String VALIDATION_STRING_LENGTH = "{validation.string.length}";

	/**
	 * Message: 'Enter between {min} and {max} characters'
	 * 
	 * @see Size
	 */
	public static final String VALIDATION_STRING_MIN_MAX = "{validation.string.min.max}";

	/** Message: 'Please make a selection' */
	public static final String VALIDATION_NO_SELECTION = "{validation.no.selection}";

	/**
	 * Message: 'Please upload a file of the following types: {fileTypes}'
	 * 
	 * @see FileUpload
	 */
	public static final String VALIDATION_FILE_INVALID = "{validation.file.invalid}";

	/**
	 * Message: 'Please upload a file (max. {maxSize} {unit}) of the following types: {fileTypes}'
	 * 
	 * @see FileUpload
	 */
	public static final String VALIDATION_FILE_INVALID_SIZE = "{validation.file.invalidSize}";

	/**
	 * Message: 'Please upload {minCount} to {maxCount} files of the following types: {fileTypes}'
	 * 
	 * @see FileUpload
	 */
	public static final String VALIDATION_FILES_INVALID = "{validation.files.invalid}";

	/**
	 * Message: 'Please upload {minCount} to {maxCount} files (max. {maxSize} {unit} each) of the following types:
	 * {fileTypes}'
	 * 
	 * @see FileUpload
	 */
	public static final String VALIDATION_FILES_INVALID_SIZE = "{validation.files.invalidSize}";

	/**
	 * Message: 'Field must not be empty'
	 * 
	 * @see NotNull
	 */
	public static final String VALIDATION_NOT_NULL = "{validation.notNull}";

	/**
	 * Message: 'Value must be greater than or equal to {value}'
	 * 
	 * @see Min
	 */
	public static final String VALIDATION_MIN = "{validation.min}";

	/**
	 * Message: 'Value must be less than or equal to {value}'
	 * 
	 * @see Max
	 */
	public static final String VALIDATION_MAX = "{validation.max}";

	/** Message: 'Please enter a valid e-mail address' */
	public static final String VALIDATION_EMAIL = "{validation.email}";

	private ValidationMessages() {

	}
}
