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
package org.appng.forms;

/**
 * Interface to support further validation of the uploaded file, for example check the dimensions of an image or the
 * structure of a text file.
 * 
 * @see FormUpload#isValid(FormUploadValidator)
 * @see FormUpload#isValid(Class)
 * 
 * @author Matthias Müller
 * 
 */
public interface FormUploadValidator {

	/**
	 * Validates the given {@link FormUpload}.
	 * 
	 * @param formUpload
	 *            the {@link FormUpload} to validate
	 * @return {@code true} if the given {@link FormUpload} is valid, {@code false} otherwise.
	 */
	boolean isValid(FormUpload formUpload);

}
