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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Scope;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.el.ExpressionEvaluator;

import lombok.extern.slf4j.Slf4j;

/**
 * This class defines an attribute tag which can be used in JSPs.<br/>
 * It is used to read attributes from/write attributes to the current {@link Environment} using one of the supported
 * {@link Scope}s.
 * <p>
 * For write-mode, only {@link Scope#REQUEST} and {@link Scope#SESSION} are supported.
 * </p>
 * <p>
 * In read-mode, you can also &quot;navigate&quot; through objects that are present in the current scope, for example
 * {@code SESSION.currentSubject.realname}, where {@code SESSION.currentSubject} is the name of the attribute (which is
 * a {@link org.appng.api.model.Subject}) and {@code realname} is a property of the subject.
 * </p>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;appNG:attribute scope="SESSION" mode="read"  name="foo"  />
 * &lt;appNG:attribute scope="REQUEST" mode="read"  name="bar" />
 * &lt;appNG:attribute scope="SESSION" mode="write" name="someName" value="someValue"/>
 * &lt;-- for URL-scope, the name is the zero based index of the path segment (segments are separated by '/') -->
 * &lt;-- for example, if the path is '/en/foo/bar/42' then you can access the '42' with index 3-->
 * &lt;appNG:attribute scope="URL" mode="read" name="3" />
 * </pre>
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 */
@Slf4j
public class Attribute extends TagSupport implements Tag {

	/**
	 * the mode for an {@link Attribute}
	 */
	public enum Mode {
		READ, WRITE;
	}

	private static final long serialVersionUID = 1L;
	private String name;
	private String value;
	private Mode modeInternal = Mode.READ; // default mode: read
	private Scope scopeInternal = Scope.SESSION; // default scope: session

	public Attribute(String scope, String name) {
		setName(name);
		setScope(scope);
	}

	public Attribute() {
	}

	@Override
	public int doEndTag() throws JspException {
		release();
		return super.doEndTag();
	}

	@Override
	public int doStartTag() {

		switch (modeInternal) {
		case READ:
			readValue();
			try {
				JspWriter out = pageContext.getOut();
				out.print(value);
			} catch (IOException ioe) {
				LOGGER.error("error while writing to JspWriter", ioe);
			}
			break;
		case WRITE:
			writeValue();
			break;
		}
		return SKIP_BODY;
	}

	@Override
	public void release() {
		name = null;
		value = null;
		scopeInternal = null;
		modeInternal = null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getMode() {
		return modeInternal.name();
	}

	public void setMode(String mode) {
		try {
			this.modeInternal = Mode.valueOf(StringUtils.upperCase(mode));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"invalid value '" + mode + "' for mode, allowed are " + StringUtils.join(Mode.values(), ", "));
		}
	}

	public String getScope() {
		return scopeInternal.name();
	}

	public void setScope(String scope) {
		try {
			this.scopeInternal = Scope.valueOf(StringUtils.upperCase(scope));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"invalid value '" + scope + "' for scope, allowed are " + StringUtils.join(Scope.values(), ", "));
		}
	}

	private void readValue() {
		Environment env = DefaultEnvironment.get(pageContext);

		switch (scopeInternal) {
		case URL:
			boolean matches = name.matches("\\d+");
			if (matches) {
				int position = Integer.valueOf(name);
				String servletPath = env.getAttributeAsString(Scope.REQUEST, EnvironmentKeys.SERVLETPATH);
				String[] values = servletPath.split("/");
				if (values.length > position) {
					value = values[position];
				}
			} else {
				throw new IllegalArgumentException("'" + name
						+ "' is not a valid name for an attribute with scope 'URL', name must be an integer number!");
			}
			break;

		default:
			String attributeRealName = resolveRealAttributeName(env, name);
			int dotIdx = attributeRealName.lastIndexOf('.');
			Map<String, Object> params = new HashMap<>();
			String parameterName = attributeRealName.substring((dotIdx > 0 ? dotIdx : -1) + 1);
			Object attribute = env.getAttribute(scopeInternal, attributeRealName);
			if (null != attribute) {
				params.put(parameterName, attribute);
				String placeHolder = name.substring(name.indexOf(parameterName));
				Object objectValue = new ExpressionEvaluator(params).evaluate("${" + placeHolder + "}", Object.class);
				if (null != objectValue) {
					value = objectValue.toString();
				}
			}
		}

		if (value == null) {
			value = "";
		}
	}

	private String resolveRealAttributeName(Environment env, String attrName) {
		if (env.keySet(scopeInternal).contains(attrName)) {
			return attrName;
		}
		int dotIdx = attrName.lastIndexOf('.');
		int bracketIdx = attrName.lastIndexOf('[');
		boolean hasDot = dotIdx > 0;
		if (hasDot || bracketIdx > 0) {
			String newName = attrName.substring(0, hasDot ? dotIdx : bracketIdx);
			return resolveRealAttributeName(env, newName);
		}
		return attrName;
	}

	private void writeValue() {
		switch (scopeInternal) {

		case REQUEST:
		case SESSION:
			DefaultEnvironment.get(pageContext).setAttribute(scopeInternal, name, value);
			break;

		default:
			throw new UnsupportedOperationException("Can not write attribute '" + name
					+ "', as mode=\"write\" is not allowed for scope =\"" + scopeInternal.name().toLowerCase() + "\"!");
		}
	}

}
