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
package org.appng.api.search;

import java.util.Date;

import org.apache.lucene.index.IndexableField;
import org.appng.api.model.Site;
import org.appng.api.observe.Observable;

/**
 * A {@link Document} is tracked by the <a href="http://lucene.apache.org/">Lucene</a> searchindex. It implements
 * {@link Observable} so it can notify an {@link org.appng.api.observe.Observer} of changes.<br/>
 * A Lucene {@link org.apache.lucene.document.Document} is being created from a {@link Document} instance, which is then
 * being added/updated/deleted from/to the index, depending on the {@link DocumentEvent} passed to
 * {@link DocumentProducer}{@code #put({@link DocumentEvent})}.
 * <p>
 * TODO appng-search is the better place for this
 * </p>
 * 
 * @author Matthias Müller
 * 
 * @see DocumentProducer
 * @see DocumentEvent
 */
public interface Document extends Observable<Document>, Comparable<Document> {

	String FIELD_PATH = "path";
	String FIELD_TITLE = "title";
	String FIELD_TEASER = "teaser";
	String FIELD_IMAGE = "image";
	String FIELD_DATE = "date";
	String FIELD_TYPE = "type";
	String FIELD_LANGUAGE = "language";
	String FIELD_CONTENT = "contents";
	String FIELD_ID = "id";
	String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

	/**
	 * Event indicating a {@link Document} has been created.
	 */
	Event CREATE = new Event("create");

	/**
	 * Event indicating a {@link Document} has been updated.
	 */
	Event UPDATE = new Event("update");

	/**
	 * Event indicating a {@link Document} has been deleted.
	 */
	Event DELETE = new Event("delete");

	/**
	 * Returns the ID of this {@link Document}.<br/>
	 * <b>Has to be globally unique, speaking in the scope of a {@link Site}, since each site has it's own
	 * searchindex.</b>
	 * 
	 * @return the ID
	 */
	String getId();

	/**
	 * Returns the name of this {@link Document}.
	 * 
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the {@link Date} of the last modification of this {@link Document}.
	 * 
	 * @return the {@link Date} of the last modification of this {@link Document}
	 */
	Date getDate();

	/**
	 * Returns the description of this {@link Document}.
	 * 
	 * @return the description of this {@link Document}
	 */
	String getDescription();

	String getImage();

	/**
	 * Returns the path of this {@link Document}, relative to the {@link Site}s domain.
	 * 
	 * @return the path of this {@link Document}, relative to the {@link Site}s domain
	 */
	String getPath();

	/**
	 * Returns the content to be indexed.
	 * 
	 * @return the content to be indexed
	 */
	String getContent();

	/**
	 * Returns the type of this {@link Document}, to be used when searching {@link Document}s of a certain type.
	 * 
	 * @return the type
	 */
	String getType();

	/**
	 * Returns the language of this {@link Document}.
	 * 
	 * @return the language
	 */
	String getLanguage();

	/**
	 * Returns some additionally {@link IndexableField}s (if any) to be added to the
	 * {@link org.apache.lucene.document.Document} which is being created from this {@link Document}.
	 * 
	 * @return some {@link IndexableField}s
	 */
	Iterable<IndexableField> getAdditionalFields();

	IndexableField getField(String string);

	float getScore();

	int getDocId();

	String getFragment();

	void setFragment(String fragment);

}
