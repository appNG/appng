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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.appng.forms.FormUpload;
import org.appng.forms.Request;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestRequest implements Request {

	Map<String, List<String>> parametersList = new HashMap<>();
	Map<String, List<FormUpload>> uploads = new HashMap<>();
	private String encoding;
	private boolean isGet = true;
	private HttpServletRequest httpServletRequest;
	private HttpSession session;
	private boolean isMultiPart;

	public TestRequest() {
		this(new HashMap<>());
	}

	public TestRequest(Map<String, Object> sessionAttributes) {
		this.httpServletRequest = Mockito.mock(HttpServletRequest.class);
		this.session = Mockito.mock(HttpSession.class);
		Mockito.when(httpServletRequest.getSession()).thenReturn(session);
		setSessionAttributes(sessionAttributes);
	}

	public void addUploads(String name, List<FormUpload> uploads) {
		this.uploads.put(name, uploads);
	}

	public void addUpload(String name, FormUpload upload) {
		this.uploads.put(name, Arrays.asList(upload));
	}

	public String getHost() {
		return "localhost";
	}

	public Map<String, List<String>> getParametersList() {
		return parametersList;
	}

	public Map<String, String> getParameters() {
		Map<String, String> parameters = new HashMap<>();
		for (String key : parametersList.keySet()) {
			String parameter = getParameter(key);
			if (null != parameter) {
				parameters.put(key, parameter);
			}
		}
		return parameters;
	}

	public String getParameter(String name) {
		List<String> params = parametersList.get(name);
		return null == params ? null : (params.size() == 0 ? null : params.get(0));
	}

	public Set<String> getParameterNames() {
		return parametersList.keySet();
	}

	public boolean hasParameter(String name) {
		return getParameterNames().contains(name);
	}

	public List<String> getParameterList(String name) {
		if (hasParameter(name)) {
			return parametersList.get(name);
		}
		return Collections.unmodifiableList(new ArrayList<>());
	}

	public Map<String, List<FormUpload>> getFormUploads() {
		return uploads;
	}

	public List<FormUpload> getFormUploads(String name) {
		return uploads.containsKey(name) ? uploads.get(name) : new ArrayList<>();
	}

	public void process(HttpServletRequest httpServletRequest) {
		// nothing to do
	}

	public HttpServletRequest getHttpServletRequest() {
		return httpServletRequest;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public boolean isMultiPart() {
		return isMultiPart;
	}

	public void setMultiPart(boolean isMultiPart) {
		this.isMultiPart = isMultiPart;
	}

	public boolean isPost() {
		return !isGet();
	}

	public void setGet(boolean isGet) {
		this.isGet = isGet;
	}

	public boolean isGet() {
		return isGet;
	}

	public boolean isValid() {
		return true;
	}

	public void setTempDir(File tempDir) {

	}

	public void setMaxSize(long maxSize) {

	}

	public void setMaxSize(long maxSize, boolean strict) {

	}

	public void setAcceptedTypes(String uploadName, String... types) {

	}

	public List<String> getAcceptedTypes(String uploadName) {
		return null;
	}

	public void addParameters(Map<String, String> parameters) {
		for (Entry<String, String> entry : parameters.entrySet()) {
			addParameter(entry.getKey(), entry.getValue());
		}
	}

	public void addParameter(String key, String value) {
		if (null != value) {
			parametersList.put(key, Arrays.asList(value));
		}
	}

	public void addParameter(String key, String... values) {
		if (null != values) {
			parametersList.put(key, Arrays.asList(values));
		}
	}

	private void setSessionAttributes(final Map<String, Object> hashMap) {

		Enumeration<String> attributes = new Enumeration<String>() {

			Iterator<String> it = hashMap.keySet().iterator();

			@Override
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			@Override
			public String nextElement() {
				return it.next();
			}
		};
		Mockito.when(session.getAttributeNames()).thenReturn(attributes);
		Mockito.when(session.getAttribute(Mockito.isA(String.class))).then(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return hashMap.get(invocation.getArguments()[0]);
			}
		});
	}
}
