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
package org.appng.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Parameter} can be added to any {@link ParameterOwner}.
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;appNG:param name="foo">bar&lt;/appNG:param>
 * </pre>
 * 
 * @author Matthias Müller
 * 
 * @see ApplicationAdapter
 * @see org.appng.taglib.form.FormConfirmation
 * @see org.appng.taglib.search.Search
 * @see org.appng.taglib.search.SearchPart
 * @see TagletAdapter
 */
@Slf4j
public class Parameter extends BodyTagSupport {

	private String name;
	private boolean unescape = false;

	public Parameter() {
	}

	@Override
	public int doEndTag() throws JspException {
		ParameterOwner tag = (ParameterOwner) findAncestorWithClass(this, ParameterOwner.class);
		if (null == tag) {
			throw new JspException(
					"<appNG:param> can only be used inside <appNG:search>, <appNG:formConfirmation>, <appNG:searchPart> or <appNG:taglet>");
		}
		String textValue = null == bodyContent ? null : StringUtils.trim(bodyContent.getString());
		String value = unescape ? StringEscapeUtils.unescapeHtml4(textValue) : textValue;
		tag.addParameter(name, value);
		LOGGER.debug("added parameter {}={} to parent tag {}", name, value, tag);
		this.name = null;
		this.unescape = false;
		return super.doEndTag();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isUnescape() {
		return unescape;
	}

	public void setUnescape(boolean unescape) {
		this.unescape = unescape;
	}

	@Override
	public String toString() {
		return getClass().getName() + "#" + hashCode() + " " + name + "="
				+ (null == bodyContent ? "" : bodyContent.getString());
	}

}
