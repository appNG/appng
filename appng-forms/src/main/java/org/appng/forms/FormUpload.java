/*
 * Copyright 2011-2023 the original author or authors.
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

import java.io.File;
import java.util.List;

import javax.servlet.ServletRequest;

/**
 * A {@link FormUpload} represents a {@link File} which was uploaded through a HTML form using
 * {@code <input type="file">}.
 * 
 * @author Matthias Müller
 */
public interface FormUpload {

	/**
	 * Returns the name of the original (without any path prefix) file which was uploaded.
	 * 
	 * @return the name of the original file
	 */
	String getOriginalFilename();

	/**
	 * Returns the {@link File} which has been locally saved, but only if {@link #isValid()} returns {@code true}. The
	 * name of this file may have nothing to do with the file's original name.
	 * 
	 * @return the {@link File}, or {@code null} if no file was uploaded or {@link #isValid()} returns {@code false}
	 * 
	 * @see #getOriginalFilename()
	 */
	File getFile();

	/**
	 * Returns the bytes of the uploaded {@link File}.
	 * 
	 * @return the bytes
	 */
	byte[] getBytes();

	/**
	 * Returns the size (in bytes) of the uploaded {@link File}.
	 * 
	 * @return the size
	 */
	long size();

	/**
	 * Returns the minimum size (in bytes) for the uploaded file (default: 0).
	 * 
	 * @return the minimum size
	 */
	long getMinSize();

	/**
	 * Returns the maximum size (in bytes) for the uploaded {@link File} (default: -1, which means there is no limit).
	 * 
	 * @return the maximum size
	 */
	long getMaxSize();

	/**
	 * Returns {@code true} if, and only if, all of those methods return {@code true}:
	 * <ul>
	 * <li>{@link #isValidFile()}
	 * <li>{@link #isValidSize()}
	 * <li>{@link #isValidType()}
	 * </ul>
	 * 
	 * @return {@code true} if this {@link FormUpload} is valid, {@code false} otherwise.
	 */
	boolean isValid();

	/**
	 * Calls {@link FormUploadValidator#isValid(FormUpload)} using this {@link FormUpload} and returns the result.
	 * 
	 * @param validator
	 *                  a {@link FormUploadValidator}
	 * 
	 * @return the result returned by {@link FormUploadValidator#isValid(FormUpload)}
	 */
	boolean isValid(FormUploadValidator validator);

	/**
	 * Calls {@link FormUploadValidator#isValid(FormUpload)} using this {@link FormUpload} and returns the result.
	 * 
	 * @param validatorClass
	 *                       a type extending {@link FormUploadValidator}
	 * 
	 * @return the result returned by {@link FormUploadValidator#isValid(FormUpload)}
	 */
	boolean isValid(Class<? extends FormUploadValidator> validatorClass);

	/**
	 * Checks whether this {@link FormUpload} has one of the given types and matches the given size restrictions.
	 * Delegates to {@link #isValidSize()} and {@link #isValidType()}.
	 * 
	 * @param types
	 *                an array containing the allowed file-extensions (without {@code .}) and content-types
	 * @param minSize
	 *                the minimum size of the file (in bytes)
	 * @param maxSize
	 *                the maximum size of the file (in bytes)
	 * 
	 * @return {@code true} if this {@link FormUpload} is valid, {@code false} otherwise.
	 * 
	 * @see #isValidSize()
	 * @see #isValidType()
	 * @see #getContentType()
	 * @see #getAcceptedTypes()
	 */
	boolean isValid(String[] types, long minSize, long maxSize);

	/**
	 * Checks whether the {@link File} has a valid size. Note that by default, there is no minimum size. The default
	 * maximum size is provided by the concrete {@link FormUpload}.
	 * 
	 * @return {@code true} if the file's size is valid, {@code false} otherwise
	 * 
	 * @see #isValid(String[], long, long)
	 * @see #getMinSize()
	 * @see #getMaxSize()
	 */
	boolean isValidSize();

	/**
	 * Checks whether the {@link File}'s extension <b>or</b> content-type are contained in the list of accepted types
	 * for this {@link FormUpload}. If {@link #getAcceptedTypes()} is {@code null} or empty, all types are allowed.
	 * 
	 * @return {@code true} if the uploaded {@link File} has a valid type, {@code false} otherwise.
	 * 
	 * @see #getAcceptedTypes()
	 * @see #getContentType()
	 */
	boolean isValidType();

	/**
	 * Returns a {@link List} of accepted file-extensions (without {@code .})/content-types for this {@link FormUpload}.
	 * 
	 * @return the list of accepted types
	 * 
	 * @see #getContentType()
	 */
	List<String> getAcceptedTypes();

	/**
	 * Checks whether the {@link File} for this {@link FormUpload} exists and {@link File#isFile()} returns {@code true}
	 * .
	 * 
	 * @return {@code true} if the {@link File} is valid, {@code false} otherwise
	 */
	boolean isValidFile();

	/**
	 * Returns the content-type (if any) for this {@link FormUpload}. Usually the content-type has been retrieved using
	 * {@link ServletRequest#getContentType()}
	 * 
	 * @return the content type for this {@link FormUpload}, or {@code null}
	 */
	String getContentType();

}
