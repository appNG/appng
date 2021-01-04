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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility-class used to parse {@code <appNG:searchable>}-tags from JSP-files.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class ParseTags {

	private static final char NBSP = (char) 160;
	private static final char BLANK = (char) 32;
	private static final String ATTR_FIELD = "field";
	private static final String ATTR_INDEX = "index";
	private static final String SEARCHABLE = "searchable";
	private String tagPrefix;

	/**
	 * Creates a new {@code ParseTags} using the given tag-prefix (usually {@code appNG}).
	 * 
	 * @param tagPrefix
	 *            the tag prefix
	 */
	public ParseTags(String tagPrefix) {
		this.tagPrefix = tagPrefix;
	}

	/**
	 * Parses the given {@link InputStream} and returns a {@link Map} containing the name of a field as a key and a
	 * {@link StringBuilder} (the text content of the {@code <appNG:searchable>}-tag) as the value.
	 * 
	 * @param is
	 *            the {@link InputStream}
	 * @return the {@link Map} of fields
	 * @throws IOException
	 *             if such an error occurred while reading/parsing the stream
	 */
	public Map<String, StringBuilder> parse(InputStream is) throws IOException {
		try (InputStream inner = is) {
			Map<String, StringBuilder> fieldMap = new HashMap<>();
			Document doc = Jsoup.parse(inner, null, "");
			Elements searchables = doc.getElementsByTag(tagPrefix + ":" + SEARCHABLE);
			List<Node> skipped = new ArrayList<>();
			for (Element node : searchables) {
				StringBuilder content = new StringBuilder();
				if (append(skipped, node, content)) {
					String field = node.attr(ATTR_FIELD);
					if (!fieldMap.containsKey(field)) {
						fieldMap.put(field, content);
					} else {
						StringBuilder existingBuffer = fieldMap.get(field);
						existingBuffer.append(content.toString().trim());
					}
				}
			}
			return fieldMap;
		} catch (IOException e) {
			throw e;
		}
	}

	private boolean doIndex(List<Node> skipped, Node node) {
		if (node instanceof Element) {
			Element element = (Element) node;
			Element parent = element;
			boolean skip = false;
			String nodeName = element.nodeName();
			while (null != (parent = parent.parent())) {
				if (skipped.contains(parent)) {
					skip = true;
					LOGGER.trace("skipping {} field = {}", nodeName, element.attr(ATTR_FIELD));
					break;
				}
			}
			String index = element.attr(ATTR_INDEX);
			return !skip
					&& (!nodeName.equalsIgnoreCase(tagPrefix + ":" + SEARCHABLE) || "true".equalsIgnoreCase(index));
		}
		return false;
	}

	private boolean append(List<Node> skipped, Node node, StringBuilder content) {
		boolean doAppend = false;
		if (!skipped.contains(node)) {
			doAppend = doIndex(skipped, node);
			if (doAppend) {
				for (Node child : node.childNodes()) {
					if (child instanceof TextNode) {
						String text = child.toString();
						if (content.lastIndexOf(" ") != content.length() - 1) {
							content.append(" ");
						}
						String unescaped = StringEscapeUtils.unescapeHtml4(text);
						content.append(unescaped.trim().replace(NBSP, BLANK));
					}
					append(skipped, child, content);
				}
				String tagName = tagPrefix + ":" + SEARCHABLE;
				if (node.nodeName().equalsIgnoreCase(tagName)) {
					String field = node.attr(ATTR_FIELD);
					LOGGER.trace("adding {} field = '{}'", tagName, field);
				}
			} else {
				skipped.add(node);
			}
		}
		return doAppend;
	}

	protected Map<String, StringBuilder> parse(File file) throws IOException {
		LOGGER.debug("parsing {}", file.getAbsolutePath());
		return parse(new FileInputStream(file));
	}

}
