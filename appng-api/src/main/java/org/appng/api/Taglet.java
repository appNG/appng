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

import java.util.Map;

import org.appng.api.model.Application;
import org.appng.api.model.Site;

/**
 * 
 * A {@code Taglet} can be used inside a JSP-Page to embed (static or dynamic) content that is retrieved from a
 * {@link Application}.<br/>
 * A {@code Taglet} is used as follows ({@code method} refers to bean name of the implementing class):
 * 
 * <pre>
 * &lt;%@taglib uri="http://appng.org/tags" prefix="appNG" %>
 * 
 * &lt;appNG:taglet application="application-name" method="taglet-name">
 *     &lt;param:anAttribute>aValue&lt;/param:anAttribute>
 * &lt;/appNG:taglet>
 * </pre>
 * 
 * Every {@code <param>} is passed in as a taglet-attribute.
 * 
 * @author Matthias Müller
 * 
 * @see XMLTaglet
 * 
 */
public interface Taglet {

	/**
	 * Executes the {@code Taglet} and returns a {@link String} to be embedded at the calling JSP-page.
	 * 
	 * @param site
	 *            the current {@link Site}
	 * @param application
	 *            the current {@link Application}
	 * @param request
	 *            the current {@link Request}
	 * @param tagletAttributes
	 *            a {@link Map} containing the attributes for the {@code Taglet}
	 * 
	 * @return a String to be embedded into the calling JSP
	 */
	String processTaglet(Site site, Application application, Request request, Map<String, String> tagletAttributes);
}
