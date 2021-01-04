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
package org.appng.search.searcher;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to replace special characters from the search terms.
 * 
 */
@Slf4j
public class GermanSearchTermProcessor implements SearchTermProcessor {
	private static String[] charsToRemove = { "!", "?", ".", ",", "<", ">", "{", "}", "[", "]", "+", "-", "*", "/",
			"\\", "&", "|", "=", "%", "$", "§", "#" };
	private GermanAnalyzer analyzer;
	private Map<String, String> searchTerms;

	public GermanSearchTermProcessor() {
		this(new HashMap<>());
	}

	public GermanSearchTermProcessor(Map<String, String> searchTerms) {
		this.analyzer = new GermanAnalyzer();
		this.searchTerms = searchTerms;
	}

	public String getSearchTerm(String searchWord) {
		searchWord = cleanSearchWord(searchWord);

		StringTokenizer tokenizer = new StringTokenizer(searchWord);
		StringBuilder searchTerm = new StringBuilder();
		int count = 0;
		try {
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				append(searchTerm, count, " AND ");
				if (null != searchTerms && searchTerms.containsKey(token)) {
					append(searchTerm, count, "(");
					searchTerm.append(searchTerms.get(token));
					append(searchTerm, count, ")");
				} else {
					TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(token));
					tokenStream.reset();
					while (tokenStream.incrementToken()) {
						String stemmedSearchWord = tokenStream.getAttribute(CharTermAttribute.class).toString();
						if (!"".equals(stemmedSearchWord)) {
							if (hasUmlaut(token)) {
								searchTerm.append("(*" + stemmedSearchWord + " OR " + removeUmlauts(token) + "*)");
							} else {
								searchTerm.append("(*" + stemmedSearchWord + " OR " + stemmedSearchWord + "*)");
							}
						} else {
							searchTerm.append(searchWord);
						}
					}
					tokenStream.end();
					tokenStream.close();
				}
				count++;
			}
		} catch (IOException e) {
			LOGGER.error("error assembling searchterm", e);
		}
		return searchTerm.toString();
	}

	private void append(StringBuilder searchTerm, int count, String string) {
		if (0 != count) {
			searchTerm.append(string);
		}
	}

	/**
	 * replace German special alphabet.
	 * 
	 * @param term the term
	 * @return the term without german Umlaut characters
	 */
	public String removeUmlauts(String term) {
		term = term.replace("ä", "a");
		term = term.replace("ö", "o");
		term = term.replace("ü", "u");
		term = term.replace("ß", "ss");
		return term;
	}

	/**
	 * remove special characters from the search term.
	 * 
	 * @param term
	 * @return the cleaned search term
	 */
	public String cleanSearchWord(String term) {
		for (String replacment : charsToRemove) {
			term = term.replace(replacment, "");
		}
		return term.toLowerCase();
	}

	/**
	 * Check whether search word has a German special character.
	 * 
	 * @param searchWord
	 * @return
	 */
	private boolean hasUmlaut(String searchWord) {
		return searchWord.indexOf("ä") > -1 || searchWord.indexOf("ö") > -1 || searchWord.indexOf("ü") > -1
				|| searchWord.indexOf("ß") > -1;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}
}
