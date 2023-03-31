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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.apache.commons.fileupload.FileUpload;

/**
 * A {@link RequestContainer} conveniently wraps an {@link ServletRequest} and simplifies the access to (possibly
 * multi-valued) request-parameters including {@link FormUpload}s.
 * 
 * @author Matthias Müller
 */
public interface RequestContainer {

	/**
	 * Returns the host name for this request
	 * 
	 * @return the host name
	 */
	String getHost();

	/**
	 * Returns an immutable {@link Map} containing the name of each request-parameter as a key and the list of submitted
	 * values as value.
	 * 
	 * @return a {@link Map} containing the request parameters (including multi-valued ones)
	 */
	Map<String, List<String>> getParametersList();

	/**
	 * Returns an immutable {@link Map} containing the name of each request-parameter as a key and a single parameter
	 * value as the map's value.<br/>
	 * <b>Note that for multi-valued parameters, the returned map contains any of the submitted values. Use
	 * {@link #getParametersList()} instead if you want to deal with multi-valued parameters.</b>
	 * 
	 * @return a {@link Map} containing the request parameters
	 */
	Map<String, String> getParameters();

	/**
	 * Returns the value for the parameter with the given name.<br/>
	 * <b>Note that for a multi-valued parameter, any of the submitted values is returned. Use
	 * {@link #getParameterList(String)} instead if you want to deal with a multi-valued parameter.</b>
	 * 
	 * @param name
	 *             the name of the parameter
	 * 
	 * @return the value of the parameter (may be null)
	 */
	String getParameter(String name);

	/**
	 * Returns an immutable {@link Set} containing the names of all request parameters.
	 * 
	 * @return the parameters names
	 */
	Set<String> getParameterNames();

	/**
	 * Checks whether a parameter with the given name exists.
	 * 
	 * @param name
	 *             the name of the parameter
	 * 
	 * @return {@code true} if such a parameter exists, {@code false} otherwise
	 */
	boolean hasParameter(String name);

	/**
	 * Returns a {@link List} containing all the values for the parameter with the given name.
	 * 
	 * @param name
	 *             the name of the parameter
	 * 
	 * @return a {@link List} (never {@code null}, but may be empty) containing all the values for the given parameter
	 */
	List<String> getParameterList(String name);

	/**
	 * Returns all {@link FormUpload}s for the request as immutable map. Returns a {@link Map} using the parameters name
	 * as a key and a {@link List} of {@link FormUpload}s as the value.
	 * 
	 * @return a {@link Map} containing the {@link FileUpload}s
	 */
	Map<String, List<FormUpload>> getFormUploads();

	/**
	 * Returns an immutable {@link List} of all {@link FormUpload}s for the parameter with the given name.
	 * 
	 * @param name
	 *             the name of the parameters
	 * 
	 * @return a list if {@link FormUpload}s (never {@code null}, but may be empty)
	 */
	List<FormUpload> getFormUploads(String name);

}
