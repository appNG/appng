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
package org.appng.search.searcher;

import org.apache.lucene.analysis.Analyzer;

/**
 * Interface for processing a search term, i.e. manipulating it in some kind of way.
 * 
 */
public interface SearchTermProcessor {

	/**
	 * Processes the search term.
	 * 
	 * @param term
	 *            the term to process
	 * @return the processed term
	 */
	public String getSearchTerm(String term);

	/**
	 * Gets the {@link Analyzer} used while processing
	 * 
	 * @return the {@link Analyzer}
	 */
	public Analyzer getAnalyzer();
}
