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
package org.appng.search.indexer;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.appng.api.observe.Observable;
import org.appng.api.observe.Observer;
import org.appng.api.observe.impl.ObservableDelegate;
import org.appng.api.search.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.extern.slf4j.Slf4j;

/**
 * A basic {@link Document}-implementation, suitable for most cases. Uses an {@link ObservableDelegate} to manage its
 * {@link Observer}s.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class SimpleDocument implements Document {

	public static final FastDateFormat DATEFORMAT = FastDateFormat.getInstance(YYYY_MM_DD_HH_MM_SS);

	private Observable<Document> observable;

	private String language;

	private String type;

	private String content;

	private String path;

	private String image;

	private String description;

	private Date date;

	private String name;

	private String id;

	private int docId = -1;

	private float score = -1.0f;

	private String fragment;

	private Map<String, IndexableField> fields;

	public SimpleDocument() {
		observable = new ObservableDelegate<Document>(this);
		fields = new HashMap<>();
	}

	public SimpleDocument(float score) {
		this();
		this.score = score;
	}

	public boolean addObserver(Observer<? super Document> observer) {
		return observable.addObserver(observer);
	}

	public boolean removeObserver(Observer<? super Document> observer) {
		return observable.removeObserver(observer);
	}

	public void notifyObservers(org.appng.api.observe.Observable.Event event) {
		observable.notifyObservers(event);
	}

	@JsonIgnore
	public Iterable<IndexableField> getAdditionalFields() {
		return fields.values();
	}

	public void addField(IndexableField field) {
		fields.put(field.name(), field);
	}

	public IndexableField getField(String name) {
		return fields.get(name);
	}

	@JsonIgnore
	public Observable<Document> getObservable() {
		return observable;
	}

	public void setObservable(Observable<Document> observable) {
		this.observable = observable;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public String getFragment() {
		return fragment;
	}

	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	public int compareTo(Document other) {
		return Float.valueOf(other.getScore()).compareTo(score);
	}

	public static SimpleDocument extract(org.apache.lucene.document.Document doc, int docId, float score) {
		SimpleDocument simpleDoc = new SimpleDocument(score);
		List<IndexableField> fields = new ArrayList<>(doc.getFields());

		simpleDoc.setDocId(docId);
		simpleDoc.setContent(getStringFromField(doc, FIELD_CONTENT, fields));
		simpleDoc.setDescription(getStringFromField(doc, FIELD_TEASER, fields));
		simpleDoc.setId(getStringFromField(doc, FIELD_ID, fields));
		simpleDoc.setLanguage(getStringFromField(doc, FIELD_LANGUAGE, fields));
		simpleDoc.setName(getStringFromField(doc, FIELD_TITLE, fields));
		simpleDoc.setPath(getStringFromField(doc, FIELD_PATH, fields));
		simpleDoc.setImage(getStringFromField(doc, FIELD_IMAGE, fields));
		simpleDoc.setType(getStringFromField(doc, FIELD_TYPE, fields));
		String dateString = getStringFromField(doc, FIELD_DATE, fields);
		if (null != dateString) {
			try {
				simpleDoc.setDate(DATEFORMAT.parse(dateString));
			} catch (ParseException e) {
				LOGGER.error("error parsing date", e);
			}
		}

		for (IndexableField field : fields) {
			simpleDoc.addField(field);
		}
		return simpleDoc;
	}

	private static String getStringFromField(org.apache.lucene.document.Document doc, String name,
			List<IndexableField> fieldsList) {
		IndexableField field = getField(doc, name, fieldsList);
		return null == field ? null : field.stringValue();
	}

	protected static Number getNumberFromField(org.apache.lucene.document.Document doc, String name,
			List<IndexableField> fieldsList) {
		IndexableField field = getField(doc, name, fieldsList);
		return null == field ? null : field.numericValue();
	}

	private static IndexableField getField(org.apache.lucene.document.Document doc, String name,
			List<IndexableField> fieldsList) {
		IndexableField field = doc.getField(name);
		fieldsList.remove(field);
		return field;
	}

	public void setFragment(String field, Analyzer analyzer, Query query, String highlightWith) {
		setFragment(this, field, analyzer, query, highlightWith);
	}

	public static void setFragment(SimpleDocument doc, String field, Analyzer analyzer, Query query,
			String highlightWith) {
		if (StringUtils.isNotBlank(doc.getContent()) && StringUtils.isNotBlank(highlightWith)) {
			try {
				Formatter formatter = new SimpleHTMLFormatter("<" + highlightWith + ">", "</" + highlightWith + ">");
				QueryScorer queryScorer = new QueryScorer(query);
				Highlighter highlighter = new Highlighter(formatter, queryScorer);
				TokenStream tokenStream = analyzer.tokenStream(field, doc.getContent());
				String fragment = highlighter.getBestFragments(tokenStream, doc.getContent(), 3, "...");
				fragment = Jsoup.clean(fragment, "", new Whitelist().addTags(highlightWith),
						new OutputSettings().prettyPrint(false));
				doc.setFragment(fragment);
			} catch (Exception e) {
				LOGGER.warn("error while extracting fragment", e);
			}
		}
	}

}
