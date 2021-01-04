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
 * Similar to a {@link Taglet}, a {@code XMLTaglet} can be used inside a JSP-Page to embed (static or dynamic) content
 * that is retrieved from a {@link Application}. But instead of returning a {@link String} directly, {@code XMLTaglet}
 * returns a {@link DataContainer}, which usually is being marshaled to XML and then processed with an XSL stylesheet.
 * Therefore, the taglet-attribute {@code xsl} should contain the site-relative path to a XSL-stylesheet. This
 * stylesheet is then being used to transform the XML.<br/>
 * Example ({@code method} refers to bean name of the implementing class):<br/>
 * 
 * <pre>
 * &lt;%@taglib uri="http://appng.org/tags" prefix="appNG" %>
 * 
 * &lt;appNG:taglet application="application-name" method="taglet-name" type="xml">
 *     &lt;param:xsl>meta/xsl/stylesheet.xsl&lt;/param:xsl>
 * &lt;/appNG:taglet>
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 * @see Taglet
 */
public interface XMLTaglet {

	/**
	 * Executes the {@code Taglet} and returns a {@link String} to be embedded at the calling JSP-page.
	 * 
	 * @param site
	 *                         the current {@link Site}
	 * @param application
	 *                         the current {@link Application}
	 * @param request
	 *                         the current {@link Request}
	 * @param tagletAttributes
	 *                         a {@link Map} containing the attributes for the {@code XMLTaglet}
	 * 
	 * @return a {@link DataContainer} to be marshaled to XML and transformed
	 */
	DataContainer processTaglet(Site site, Application application, Request request,
			Map<String, String> tagletAttributes);
}
