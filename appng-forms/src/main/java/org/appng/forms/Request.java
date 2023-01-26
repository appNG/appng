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
package org.appng.forms;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Extends the informations provided by a {@link RequestContainer} and allows processing a {@link HttpServletRequest} by
 * calling {@link #process(HttpServletRequest)}.
 * 
 * @author Matthias Müller
 */
public interface Request extends RequestContainer {

	/** request-attribute to store a parsed {@link HttpServletRequest} in */
	String REQUEST_PARSED = Request.class.getName() + ".parsedRequest";

	/**
	 * Processes the given {@link HttpServletRequest}. Must only be called once per {@link HttpServletRequest}.
	 * 
	 * @param httpServletRequest
	 *                           the {@link HttpServletRequest} to process
	 */
	void process(HttpServletRequest httpServletRequest);

	/**
	 * Returns the previously processed {@link HttpServletRequest}
	 * 
	 * @return the {@link HttpServletRequest}
	 */
	HttpServletRequest getHttpServletRequest();

	/**
	 * Return the encoding for this request
	 * 
	 * @return the encoding
	 */
	String getEncoding();

	/**
	 * Sets the encoding for this request
	 * 
	 * @param encoding
	 *                 the encoding
	 */
	void setEncoding(String encoding);

	/**
	 * Returns {@code true} if this is a multipart request.
	 * 
	 * @return {@code true} if this is a multipart request, {@code false} otherwise
	 */
	boolean isMultiPart();

	/**
	 * Returns {@code true} if this is a HTPP POST request.
	 * 
	 * @return {@code true} if this is a HTPP POST request, {@code false} otherwise
	 */
	boolean isPost();

	/**
	 * Returns {@code true} if this is a HTPP GET request.
	 * 
	 * @return {@code true} if this is a HTPP GET request, {@code false} otherwise
	 */
	boolean isGet();

	/**
	 * Checks whether this is a valid {@link Request}, i.e. no {@link Exception} occurred during
	 * {@link #process(HttpServletRequest)}.
	 * 
	 * @return {@code true} if this {@link Request} is valid, {@code false} otherwise
	 */
	boolean isValid();

	/**
	 * Sets the (absolute) temporary directory for storing {@link FormUpload}s. Note that, if not set or not existent,
	 * the directory specified by the {@link System} propertey {@code java.io.tmpdir} is used.
	 * 
	 * @param tempDir
	 *                the directory for storing {@link FormUpload}s
	 */
	void setTempDir(File tempDir);

	/**
	 * Sets the maximum size for a {@link FormUpload} within this {@link Request}.
	 * 
	 * @param maxSize
	 *                the maximum size of a {@link FormUpload}
	 * 
	 * @see FormUpload#getMaxSize()
	 */
	void setMaxSize(long maxSize);

	/**
	 * Sets the maximum size for a {@link FormUpload} within this {@link Request}, and additionally defines if
	 * violations should be handled strict.
	 * 
	 * @param maxSize
	 *                the maximum size of a {@link FormUpload}
	 * @param strict
	 *                if set to {@code true}, and a {@link FormUpload} exceeds the given size, the whole {@link Request}
	 *                will be marked as invalid.
	 * 
	 * @see Request#isValid()
	 */
	void setMaxSize(long maxSize, boolean strict);

	/**
	 * Sets the accepted mime-types or filetype-extensions for the given field
	 * 
	 * @param uploadName
	 *                   the name of the input-field (type="file")
	 * @param types
	 *                   the accepted mimetypes (e.g. image/jpeg, image/png) or file extensions (e.g jgp,png), those can
	 *                   be used in combination
	 */
	void setAcceptedTypes(String uploadName, String... types);

	/**
	 * Returns the accepted file-extensions/content-types for the input field with the given name.
	 * 
	 * @param uploadName
	 *                   the name of the input field
	 * 
	 * @return a list of accepted tyes
	 * 
	 * @see FormUpload#getAcceptedTypes()
	 */
	List<String> getAcceptedTypes(String uploadName);

	/**
	 * Adds the given parameters to this {@link Request}, but only for those parameters not already existsing. This
	 * prevents overwriting a parameter which is already present in the original {@link HttpServletRequest}. The given
	 * {@link Map} uses the parameter name as the key and the parameter value as the value.
	 * 
	 * @param parameters
	 *                   the parameters to add
	 * 
	 * @see #addParameter(String, String)
	 * @see Request#getParameter(String)
	 */
	void addParameters(Map<String, String> parameters);

	/**
	 * Adds the given parameter to this {@link Request}, but only if such a parameter not already exists. This prevents
	 * overwriting a parameter which is already present in the original {@link HttpServletRequest}.
	 * 
	 * @param name
	 *              the parameter name
	 * @param value
	 *              the parameter value
	 */
	void addParameter(String name, String value);

	/**
	 * Adds the given parameters to this {@link Request}, but only if such a parameter not already exists. This prevents
	 * overwriting a parameter which is already present in the original {@link HttpServletRequest}.
	 * 
	 * @param name
	 *              the parameter name
	 * @param values
	 *              the parameter values
	 */
	void addParameters(String name, List<String> values);

}
