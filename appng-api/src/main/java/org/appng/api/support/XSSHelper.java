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
package org.appng.api.support;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Platform;
import org.appng.api.model.Properties;
import org.appng.forms.XSSUtil;
import org.jsoup.safety.Whitelist;
import org.owasp.esapi.ESAPI;

/**
 * Utility class for creating {@link XSSUtil}-instances.
 * 
 * @author Matthias Müller
 *
 */
public class XSSHelper {

	/**
	 * Creates and returns a {@link XSSUtil}.<br/>
	 * If the boolean property {@code xssProtect} of the given {@link Properties} is {@code true}, a new {@link XSSUtil}
	 * is being created. The list-type property {@code xssAllowedTags} can be used to allow additional tags and
	 * attributes. The format is:
	 * 
	 * <pre>
	 * &lt;tag1&gt; &lt;attribute1a&gt; &lt;attribute1b&gt;|&lt;tag2&gt; &lt;attribute2a&gt; &lt;attribute2b&gt;
	 * </pre>
	 * 
	 * <br/>
	 * <strong>Example:</strong>
	 * 
	 * <pre>
	 * h1|h2|a href class style|div align style
	 * </pre>
	 * 
	 * @param platformProps
	 * @param exceptions
	 * @return
	 */
	public static XSSUtil getXssUtil(Properties platformProps, String... exceptions) {
		XSSUtil util = null;
		if (platformProps.getBoolean(Platform.Property.XSS_PROTECT)) {
			Whitelist whitelist = Whitelist.basic();
			for (String tag : platformProps.getList(Platform.Property.XSS_ALLOWED_TAGS, StringUtils.EMPTY, "\\|")) {
				String[] splitted = tag.split(StringUtils.SPACE);
				String name = splitted[0];
				if (splitted.length > 1) {
					for (int i = 1; i < splitted.length; i++) {
						whitelist.addAttributes(name, splitted[i]);
					}
				} else {
					whitelist.addTags(name);
				}
			}
			util = new XSSUtil(ESAPI.encoder(), whitelist, exceptions);
		}
		return util;
	}

}
