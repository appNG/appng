/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.search.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.lucene.index.IndexableField;
import org.appng.api.search.Document;
import org.appng.search.searcher.DateAdapter;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlType(propOrder = { "title", "link", "date", "score", "type", "language", "image", "fragment", "text", "fields" })
public class Result implements Comparable<Result> {

	private String title;
	private Date date;
	private String text;
	private String fragment;
	private String type;
	private String language;
	private String image;
	private String link;
	private float score;
	private Fields fields = new Fields();

	public Result() {
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@XmlJavaTypeAdapter(DateAdapter.class)
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getFragment() {
		return fragment;
	}

	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	@JsonIgnore
	public Fields getFields() {
		return fields;
	}

	public void setFields(Fields fields) {
		this.fields = fields;
	}
	
	

	public static Result fromDocument(Document d) {
		Result result = new Result();
		result.setDate(d.getDate());
		result.setFragment(d.getFragment());
		result.setImage(d.getImage());
		result.setLanguage(d.getLanguage());
		result.setLink(d.getPath());
		result.setScore(d.getScore());
		result.setText(d.getContent());
		result.setTitle(d.getName());
		result.setType(d.getType());
		result.setFields(new Fields());
		for (IndexableField field : d.getAdditionalFields()) {
			result.getFields().add(new Field(field.name(), field.stringValue()));
		}
		return result;
	}

	public int compareTo(Result other) {
		return Float.compare(other.getScore(), getScore());
	}

	@XmlType
	public static class Fields {
		private List<Field> fields = new ArrayList<>();

		@XmlElement(name = "field")
		public List<Field> getFields() {
			return fields;
		}

		public void add(Field field) {
			fields.add(field);
		}

		public void setFields(List<Field> fields) {
			this.fields = fields;
		}

	}

	@XmlType
	public static class Field {
		private String name;
		private String value;

		public Field() {
		}

		public Field(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@XmlAttribute
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@XmlValue
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	@XmlTransient
	@JsonGetter("fields")
	public List<Field> getFieldList() {
		return fields.getFields();
	}

}
