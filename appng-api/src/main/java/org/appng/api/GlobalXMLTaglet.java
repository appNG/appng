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
package org.appng.api;

import java.util.Map;

import org.appng.api.model.Application;
import org.appng.api.model.Site;

/**
 * A {@code GlobalXMLTaglet} is a specialized kind of a {@link XMLTaglet}. The {@link Site} that is calling the
 * {@link GlobalXMLTaglet} can be different from the {@link Site} that is executing the call, whereas in a
 * {@link XMLTaglet}, the calling and executing {@link Site} are always the same.
 * 
 * @author Matthias Müller
 */
public interface GlobalXMLTaglet extends XMLTaglet {

	/**
	 * Executes the {@code GlobalTaglet} and returns a {@link String} to be embedded at the calling JSP-page.
	 * 
	 * @param callingSite
	 *                         the {@link Site} where the original JSP-call came from
	 * @param executingSite
	 *                         the {@link Site} in which the JSP-call will be executed
	 * @param application
	 *                         the current {@link Application}
	 * @param request
	 *                         the current {@link Request}
	 * @param tagletAttributes
	 *                         a {@link Map} containing the attributes for the {@code XMLTaglet}
	 * 
	 * @return a {@link DataContainer} to be marshaled to XML and transformed
	 */
	DataContainer processTaglet(Site callingSite, Site executingSite, Application application, Request request,
			Map<String, String> tagletAttributes);
}
