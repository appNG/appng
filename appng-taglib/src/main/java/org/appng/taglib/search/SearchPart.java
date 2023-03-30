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
package org.appng.taglib.search;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.lucene.analysis.Analyzer;
import org.appng.api.model.Application;
import org.appng.search.SearchProvider;
import org.appng.taglib.ParameterOwner;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link SearchPart} offers the search results produced by a {@link SearchProvider} of an {@link Application}.
 * <p/>
 * <b>Attributes (defaults in brackets):</b>
 * <ul>
 * <li>application - the {@link Application} that offers the {@link SearchProvider}</li>
 * <li>method - the bean name of the {@link SearchProvider} within the application</li>
 * <li>analyzerClass - the fully qualified name of the {@link Analyzer} to use for searching</li>
 * <li>fields - a comma-separated list of fields to search in</li>
 * <li>title - the title of this search part</li>
 * <li>language - the two-letter language code for the search</li>
 * </ul>
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;appNG:searchPart application="acme-products" method="productSearchProvider" language="en"
 *  title="acme Products" fields="title,contents" analyzerClass="org.apache.lucene.analysis.en.EnglishAnalyzer">
 *    &lt;appNG:param name="foo">bar&lt;/appNG:param>
 *    &lt;appNG:param name="jin">fizz&lt;/appNG:param>
 *    &lt;/appNG:searchPart>
 * </pre>
 * 
 * @see SearchProvider
 * 
 * @author Matthias Müller
 */

@Slf4j
public class SearchPart extends BodyTagSupport implements ParameterOwner, Cloneable {

	private String application;
	private String method;
	private String analyzerClass;
	private String fields;
	private String title;
	private String language;
	private Map<String, String> parameters = new HashMap<>();

	@Override
	public int doEndTag() throws JspException {
		Search search = (Search) findAncestorWithClass(this, Search.class);
		if (null == search) {
			throw new JspException("searchPart can only be used inside search");
		}
		SearchPart cloned = this.clone();
		search.addPart(cloned);
		LOGGER.debug("added part {} to {}", cloned, search);
		clear();
		return super.doEndTag();
	}

	private void clear() {
		application = null;
		method = null;
		analyzerClass = null;
		fields = null;
		title = null;
		language = null;
		parameters.clear();
	}

	public void addParameter(String name, String value) {
		parameters.put(name, value);
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getFields() {
		return fields;
	}

	public void setFields(String fields) {
		this.fields = fields;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getAnalyzerClass() {
		return analyzerClass;
	}

	public void setAnalyzerClass(String analyzerClass) {
		this.analyzerClass = analyzerClass;
	}

	public Analyzer getAnalyzer() throws ReflectiveOperationException {
		return (Analyzer) Class.forName(analyzerClass).newInstance();
	}

	@Override
	protected SearchPart clone() {
		SearchPart p = new SearchPart();
		p.setAnalyzerClass(analyzerClass);
		p.setApplication(application);
		p.setFields(fields);
		p.setLanguage(language);
		p.setMethod(method);
		p.setTitle(title);
		for (String key : parameters.keySet()) {
			p.addParameter(key, parameters.get(key));
		}
		return p;
	}

	@Override
	public String toString() {
		return application + ":" + method;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}
}
