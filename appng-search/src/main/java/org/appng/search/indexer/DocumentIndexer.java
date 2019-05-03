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
package org.appng.search.indexer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.appng.api.observe.Observable.Event;
import org.appng.api.search.Consumer;
import org.appng.api.search.Document;
import org.appng.api.search.DocumentEvent;
import org.appng.api.search.DocumentProducer;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link Consumer} of {@link DocumentEvent}s produced by a {@link DocumentProducer}. Therefore it holds a queue of
 * {@link DocumentProducer}s, whose {@link DocumentEvent}s are being consumed with a timeout calling
 * {@link DocumentProducer#get(long)}. This means if a {@link DocumentProducer} doesn't produce a new {@link Document}
 * within the given timeout, the producer is being discarded and the next one is taken.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class DocumentIndexer extends Consumer<DocumentEvent, DocumentProducer> implements Runnable {

	private static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
	public static final Event CLEAR_INDEX = new Event("clear-index");

	private static final FastDateFormat DATEFORMAT = FastDateFormat.getInstance(YYYY_MM_DD_HH_MM_SS);
	private File indexDir;
	private Long timeout;

	/**
	 * Creates a new {@code DocumentIndexer}.
	 * 
	 * @param indexDir
	 *            the directory to read/save the Lucene index from/to
	 * @param timeout
	 *            the timeout in milliseconds when retrieving the next {@link DocumentEvent} from a
	 *            {@link DocumentProducer}
	 */
	public DocumentIndexer(int queueSize, File indexDir, Long timeout) {
		super(queueSize);
		this.indexDir = indexDir;
		this.timeout = timeout;
	}

	public DocumentIndexer(File indexDir, Long timeout) {
		this.indexDir = indexDir;
		this.timeout = timeout;
	}

	/**
	 * Starts this {@code DocumentIndexer}, running forever (until interrupted).
	 */
	public void run() {
		Directory directory = null;
		IndexWriter indexWriter = null;
		IndexReader reader = null;
		IndexSearcher searcher = null;
		Analyzer analyzer = null;
		boolean needsRollback = false;
		try {
			while (true) {
				DocumentProducer producer = get();
				Constructor<? extends Analyzer> constructor = producer.getAnalyzerClass().getConstructor();
				analyzer = constructor.newInstance();
				IndexWriterConfig config = new IndexWriterConfig(analyzer);
				directory = FSDirectory.open(indexDir.toPath());
				indexWriter = new IndexWriter(directory, config);
				needsRollback = true;
				LOGGER.debug("opened IndexWriter#{} with Analyzer {}", indexWriter.hashCode(), analyzer.getClass());
				reader = DirectoryReader.open(indexWriter);
				searcher = new IndexSearcher(reader);
				int before = indexWriter.numDocs();
				DocumentEvent documentEvent = null;
				int created = 0;
				int updated = 0;
				int deleted = 0;
				while (null != (documentEvent = producer.get(timeout))) {
					Event event = documentEvent.getEvent();
					if (CLEAR_INDEX.equals(event)) {
						indexWriter.deleteAll();
						LOGGER.info("clearing index at {}", indexDir.getAbsolutePath());
					} else {

						long start = System.currentTimeMillis();

						Document document = documentEvent.getDocument();
						org.apache.lucene.document.Document luceneDocument = getDocument(document);

						TermQuery idQuery = new TermQuery(new Term(Document.FIELD_ID, document.getId()));
						TermQuery typeQuery = new TermQuery(new Term(Document.FIELD_TYPE, document.getType()));

						BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
						queryBuilder.add(idQuery, Occur.MUST);
						queryBuilder.add(typeQuery, Occur.MUST);
						BooleanQuery query = queryBuilder.build();
						String queryString = query.toString();
						TopDocs search = searcher.search(query, 10);
						int found = search.totalHits;
						if (found > 0) {
							indexWriter.deleteDocuments(query);
							LOGGER.debug("deleting {} existing document(s) for query {}", found, queryString);
						}

						if (Document.CREATE.equals(event)) {
							indexWriter.addDocument(luceneDocument);
							LOGGER.debug("creating document {}", queryString);
							created++;
						} else if (Document.UPDATE.equals(event)) {
							indexWriter.addDocument(luceneDocument);
							LOGGER.debug("updating document {}", queryString);
							updated++;
						} else if (Document.DELETE.equals(event)) {
							deleted++;
						}

						long duration = System.currentTimeMillis() - start;
						LOGGER.debug("[{}ms] {}, query: {}", duration, event, queryString);
					}
				}
				indexWriter.commit();
				needsRollback = false;
				LOGGER.info("comitted IndexWriter#{}", indexWriter.hashCode());
				int after = indexWriter.numDocs();
				int overall = created + updated + deleted;
				String mssg = "done with DocumentProducer '{}' which offered {} events (CREATE: {}, UPDATE: {}, DELETE: {}). The index now contains {} documents (was {} before)";
				LOGGER.info(mssg, producer.getName(), overall, created, updated, deleted, after, before);
				LOGGER.debug("comitted IndexWriter#{}, containing {} documents (before: {}) directory: {}",
						indexWriter.hashCode(), after, before, indexDir.getAbsolutePath());
				close(indexWriter, reader, directory);
			}
		} catch (IOException ioe) {
			LOGGER.error("an I/O error occured", ioe);
		} catch (InterruptedException ie) {
			LOGGER.error("thread was interrupted", ie);
		} catch (Exception e) {
			LOGGER.error("unexpected error", e);
		} finally {
			if (null != indexWriter && needsRollback) {
				try {
					LOGGER.info("rolling back changes on IndexWriter#{}", indexWriter.hashCode());
					indexWriter.rollback();
					LOGGER.info("rolling back on IndexWriter#{} successfull", indexWriter.hashCode());
				} catch (IOException e) {
					LOGGER.info("error rolling back changes on IndexWriter#{}", +indexWriter.hashCode());
				}
			}
			close(indexWriter, reader, directory);
		}
	}

	private void close(Closeable... closeables) {
		for (Closeable closeable : closeables) {
			if (null != closeable) {
				try {
					closeable.close();
					LOGGER.debug("closed {}", closeable);
				} catch (IOException e) {
					LOGGER.debug(String.format("error closing %s", closeable), e);
				} finally {
					closeable = null;
				}
			}
		}
	}

	private org.apache.lucene.document.Document getDocument(Document document) {
		org.apache.lucene.document.Document indexDoc = new org.apache.lucene.document.Document();
		addStringField(indexDoc, Document.FIELD_ID, document.getId(), Field.Store.YES);
		addTextField(indexDoc, Document.FIELD_TITLE, document.getName(), Field.Store.YES);
		addStringField(indexDoc, Document.FIELD_PATH, document.getPath(), Field.Store.YES);
		addTextField(indexDoc, Document.FIELD_TEASER, document.getDescription(), Field.Store.YES);
		Date dueDate = document.getDate();
		if (null != dueDate) {
			addStringField(indexDoc, Document.FIELD_DATE, DATEFORMAT.format(dueDate), Field.Store.YES);
		}
		addTextField(indexDoc, Document.FIELD_CONTENT, document.getContent(), Field.Store.YES);
		addStringField(indexDoc, Document.FIELD_TYPE, document.getType(), Field.Store.YES);
		addStringField(indexDoc, Document.FIELD_LANGUAGE, document.getLanguage(), Field.Store.YES);

		if (document.getAdditionalFields() != null) {
			for (IndexableField field : document.getAdditionalFields()) {
				indexDoc.add(field);
			}
		}
		return indexDoc;
	}

	private void addTextField(org.apache.lucene.document.Document indexDoc, String name, String value, Store store) {
		if (null != value) {
			indexDoc.add(new TextField(name, value, store));
		}
	}

	private void addStringField(org.apache.lucene.document.Document indexDoc, String name, String value, Store store) {
		if (null != value) {
			indexDoc.add(new StringField(name, value, store));
		}
	}

}
