/*
 * Copyright 2011-2017 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.appng.api.Environment;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.search.Document;
import org.appng.search.SearchProvider;
import org.appng.search.indexer.SimpleDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardSearcher implements SearchProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(StandardSearcher.class);
	private static final String PARAM_EXCLUDE_TYPES = "excludeTypes";
	private static final String PARAM_TERM_TRANSFORM = "termTransform";
	private static final String DEFAULT_TERM_TRANSFORM = "term term*";

	public Iterable<Document> doSearch(Environment env, Site site, Application application, Directory directory,
			String term, String language, String[] parseFields, Analyzer analyzer, String highlightWith,
			Map<String, String> parameters) throws IOException {
		List<Document> docs = new ArrayList<Document>();

		IndexReader reader = null;
		try {
			reader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			String[] excludeTypes = StringUtils.split(parameters.get(PARAM_EXCLUDE_TYPES), ',');
			String searchTermTransform = parameters.get(PARAM_TERM_TRANSFORM);
			if (StringUtils.isBlank(searchTermTransform)) {
				searchTermTransform = DEFAULT_TERM_TRANSFORM;
			}

			String transformedTerm = getSearchTerm(term, searchTermTransform);
			Query query = getQuery(parseFields, transformedTerm, analyzer, language, excludeTypes);

			TopScoreDocCollector collector = TopScoreDocCollector.create(100);
			searcher.search(query, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			for (ScoreDoc scoreDoc : hits) {
				org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
				SimpleDocument simpleDoc = SimpleDocument.extract(doc, scoreDoc.doc, scoreDoc.score);
				simpleDoc.setFragment(Document.FIELD_CONTENT, analyzer, query, highlightWith);
				docs.add(simpleDoc);
			}
			LOGGER.info("{} results returned from query: {}", docs.size(), query);
		} catch (ParseException e) {
			LOGGER.error("error performing search", e);
		} finally {
			if (null != reader) {
				reader.close();
			}
		}

		return docs;
	}

	protected Query getQuery(String[] parseFields, String searchTerm, Analyzer analyzer, String language,
			String[] excludeTypes) throws ParseException {
		BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
		if (null != language) {
			Query langQuery = new TermQuery(new Term(Document.FIELD_LANGUAGE, language));
			booleanQuery.add(langQuery, Occur.MUST);
		}

		Query query = new MultiFieldQueryParser(parseFields, analyzer).parse(searchTerm);
		booleanQuery.add(query, Occur.MUST);

		if (null != excludeTypes) {
			for (String type : excludeTypes) {
				Term tq = new Term(Document.FIELD_TYPE, type.trim());
				booleanQuery.add(new TermQuery(tq), Occur.MUST_NOT);
			}
		}
		return booleanQuery.build();
	}

	public String getSearchTerm(String searchTerm, String searchTermTransform) {
		searchTerm = QueryParser.escape(StringUtils.trim(searchTerm));
		if (StringUtils.isNotBlank(searchTermTransform)) {
			searchTerm = searchTermTransform.replaceAll("term", searchTerm.replace("\\", "\\\\"));
		}
		return searchTerm;
	}

}
