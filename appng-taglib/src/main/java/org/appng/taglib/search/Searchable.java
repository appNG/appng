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
package org.appng.taglib.search;

import java.io.IOException;

import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a Searchable Tag.<br/>
 * This tag is used to mark areas of a JSP as indexable/searchable. It can also be used to exclude certain areas form
 * indexing/searching, for example the navigation of page. During indexing, a {@link org.appng.api.search.Document} is being created from
 * each indexed JSP.
 * <p/>
 * <b>Attributes:</b>
 * <ul>
 * <li>index - whether the body content should be indexed</li>
 * <li>visible (true) - whether or not the body content should be displayed</li>
 * <li>field - the name of the field</li>
 * </ul>
 * <p/>
 * <b>Fields:</b> The following fields of a {@link org.appng.api.search.Document} can be set:
 * <ul>
 * <li>{@value org.appng.api.search.Document#FIELD_TITLE} - the title</li>
 * <li>{@value org.appng.api.search.Document#FIELD_CONTENT} - the content</li>
 * <li>{@value org.appng.api.search.Document#FIELD_IMAGE} - a preview image</li>
 * <li>{@value org.appng.api.search.Document#FIELD_TEASER} - a teaser text</li>
 * <li>{@value org.appng.api.search.Document#FIELD_DATE} - the date</li>
 * </ul>
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;!-- set the 'title' field for this document -->
 * &lt;appNG:searchable index="true" field="title" visible="false">The Hitchhiker's Guide to the Galaxy&lt;/appNG:searchable>
 * &lt;!-- set the 'contents' field for this document -->
 * &lt;appNG:searchable index="true" field="contents" visible="true">
 * 	&lt;div>The Hitchhiker's Guide to the Galaxy is a comedy science fiction series created by Douglas Adams.&lt;/div>
 * 	&lt;!-- skip some content -->
 * 	&lt;appNG:searchable index="false">content to be skipped...&lt;/appNG:searchable>
 * &lt;/appNG:searchable>
 * </pre>
 * 
 * @author Matthias Herlitzius
 * 
 * @see org.appng.api.search.Document
 */
public class Searchable extends BodyTagSupport {

	private static final long serialVersionUID = 1L;
	private static Logger log = LoggerFactory.getLogger(Searchable.class);

	private Boolean index;
	private Boolean visible = Boolean.TRUE;
	private String field;

	@Override
	public int doAfterBody() {
		BodyContent body = getBodyContent();
		if (body != null && Boolean.TRUE.equals(visible)) {
			try {
				// Tag does nothing, just removes itself.
				// Tag is needed for indexing the search engine, attributes are
				// parsed by search engine indexer.
				String content = body.getString();
				body.getEnclosingWriter().print(content);
			} catch (IOException ioe) {
				log.error("error while writing to JspWriter", ioe);
			}
		}
		return SKIP_BODY;
	}

	@Override
	public void release() {
		index = null;
		visible = Boolean.TRUE;
	}

	public Boolean getIndex() {
		return index;
	}

	public void setIndex(Boolean index) {
		this.index = index;
	}

	public Boolean getVisible() {
		return visible;
	}

	public void setVisible(Boolean visible) {
		this.visible = visible;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

}
