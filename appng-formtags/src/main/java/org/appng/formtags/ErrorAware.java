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
package org.appng.formtags;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

interface ErrorAware {

	String getMandatoryMessage();

	String getErrorClass();

	String getErrorMessage();

	void setErrorMessage(String errorMessage);

	String getErrorElementId();

	String getContent();

	boolean isValid();

	boolean hasValue();

	String getName();

	String processContent();

	static class ErrorAppender {

		static String appendError(ErrorAware errorAware) {
			Document doc = Jsoup.parse(errorAware.getContent());
			doc.outputSettings().prettyPrint(false).syntax(OutputSettings.Syntax.xml);
			if (!errorAware.isValid()) {
				Elements elements = doc.getElementsByAttributeValue("name", errorAware.getName());
				if (elements.size() > 0 && StringUtils.isNotBlank(errorAware.getErrorClass())) {
					elements.get(0).attr("class", errorAware.getErrorClass());
				}
				if (StringUtils.isNotBlank(errorAware.getErrorElementId())) {
					Element errorElement = doc.getElementById(errorAware.getErrorElementId());
					if (null != errorElement) {
						if (!errorAware.hasValue() && StringUtils.isNotBlank(errorAware.getMandatoryMessage())) {
							errorElement.appendElement("span").append(errorAware.getMandatoryMessage());
						} else if (errorAware.hasValue() && StringUtils.isNotBlank(errorAware.getErrorMessage())) {
							errorElement.appendElement("span").append(errorAware.getErrorMessage());
						}
					}
				}
			} else {
				errorAware.setErrorMessage(null);
			}
			return doc.getElementsByTag("body").html();
		}
	}

}
