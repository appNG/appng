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
package org.appng.search;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.appng.api.Environment;
import org.appng.api.model.Application;
import org.appng.api.model.FeatureProvider;
import org.appng.api.model.Site;
import org.appng.api.search.Document;

/**
 * When performing a global search within a {@link Site}, every {@link Application} of the site is checked for
 * {@link SearchProvider}s offering some {@link Document}s.
 * 
 * @author Matthias Müller
 * 
 * @see Document
 * @see FeatureProvider#getIndexer()
 * @see DocumentProvider
 */
public interface SearchProvider {

	static final int MAX_HITS = 10000;

	/**
	 * Performs the actual search
	 * 
	 * @param env
	 *                      the current {@link Environment}
	 * @param site
	 *                      the current {@link Site}
	 * @param application
	 *                      the current {@link Application}
	 * @param directory
	 *                      the {@link Directory} representing the current {@link Site}'s Lucene index.<br>
	 *                      Note that it only makes sense to use this directory for searching if the {@link Application}
	 *                      also contributes some {@link Document}s to the {@link Site}'s global index by using
	 *                      {@link FeatureProvider#getIndexer()} or implementing {@link DocumentProvider}. Otherwise,
	 *                      the {@link Application} might have it's own Lucene index or even get the {@link Document}s
	 *                      from somewhere else (for example from the database or filesystem).
	 * @param term
	 *                      the search term
	 * @param language
	 *                      the language for the search
	 * @param parseFields
	 *                      the fields to search
	 * @param analyzer
	 *                      the {@link Analyzer} to use
	 * @param highlightWith
	 *                      a tagname to highlight fragments with, e.g {@code span} or {@code div}
	 * @param parameters
	 *                      some custom parameters for this search
	 * 
	 * @return an {@link Iterable}&lt;{@link Document}&gt; containing the found {@link Document}s
	 * 
	 * @throws IOException
	 *                     if an error occurs while searching
	 */
	Iterable<Document> doSearch(Environment env, Site site, Application application, Directory directory, String term,
			String language, String[] parseFields, Analyzer analyzer, String highlightWith,
			Map<String, String> parameters) throws IOException;

}
