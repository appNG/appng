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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.appng.api.Environment;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.search.Consumer;
import org.appng.api.search.Document;
import org.appng.api.search.DocumentEvent;
import org.appng.api.search.DocumentProducer;
import org.appng.search.json.Part;
import org.appng.search.json.Result;
import org.appng.search.searcher.SearchFormatter;
import org.appng.search.searcher.StandardSearcher;
import org.appng.testsupport.validation.WritingJsonValidator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StandardSearcherTest {

	@Mock
	private Environment env;
	@Mock
	private Site site;
	@Mock
	private Application application;
	@Mock
	private Properties siteProps;

	private File indexDir = new File("target/index");

	private Directory directory;

	private StandardSearcher standardSearcher = new StandardSearcher();

	private static final String[] PARSE_FIELDS = new String[] { "title", "contents" };

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);
		directory = FSDirectory.open(indexDir.toPath());
		Mockito.when(site.getProperties()).thenReturn(siteProps);
		Mockito.when(site.getName()).thenReturn("manager");
		String configString = "/en;en;" + EnglishAnalyzer.class.getName() + "|/de;de;" + GermanAnalyzer.class.getName();
		Mockito.when(siteProps.getString(SiteProperties.INDEX_CONFIG)).thenReturn(configString);
		Mockito.when(siteProps.getString(SiteProperties.SITE_ROOT_DIR)).thenReturn("");
		Mockito.when(siteProps.getString(SiteProperties.WWW_DIR)).thenReturn("pages");
		Mockito.when(siteProps.getString(SiteProperties.TAG_PREFIX)).thenReturn("appNG");
		Mockito.when(siteProps.getString(SiteProperties.INDEX_DIR)).thenReturn("index");
		Mockito.when(siteProps.getInteger(SiteProperties.INDEX_FILE_SYSTEM_QUEUE_SIZE)).thenReturn(1000);
		Mockito.when(siteProps.getInteger(SiteProperties.INDEX_TIMEOUT, 5000)).thenReturn(10000);
		Mockito.when(siteProps.getList(SiteProperties.INDEX_FILETYPES, ",")).thenReturn(Arrays.asList("jsp", "txt"));
	}

	@Test
	public void testIndex() throws InterruptedException, TimeoutException, IOException {
		final AtomicInteger count = new AtomicInteger(0);
		final AtomicBoolean done = new AtomicBoolean(false);
		Consumer<DocumentEvent, DocumentProducer> documentIndexer = new DocumentIndexer(indexDir, 2000L) {

			@Override
			public void putWithTimeout(DocumentProducer element, long timeoutMillis)
					throws InterruptedException, TimeoutException {
				count.incrementAndGet();
				super.putWithTimeout(element, timeoutMillis);
			}

			@Override
			public DocumentProducer get() throws InterruptedException {
				DocumentProducer documentProducer = super.get();
				done.set(0 == count.decrementAndGet());
				return documentProducer;
			}
		};

		Thread indexThread = new Thread((Runnable) documentIndexer, "documentIndexer");
		indexThread.start();
		new GlobalIndexer(documentIndexer).doIndex(site, "jsp");
		while (!done.get()) {
			Thread.sleep(250);
		}

		File indexFolder = new File("target/index");
		FilenameFilter segmentsFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("segments");
			}
		};
		File[] segments;
		while ((segments = indexFolder.listFiles(segmentsFilter)) == null || segments.length == 0) {
			Thread.sleep(250);
		}
		Directory dir = FSDirectory.open(indexFolder.toPath());
		DirectoryReader reader;
		while ((reader = DirectoryReader.open(dir)).numDocs() < 5) {
			Thread.sleep(250);
			reader.close();
		}
		reader.close();
	}

	@Test
	public void testSearchEn() throws IOException, URISyntaxException {
		Iterable<Document> doSearch = standardSearcher.doSearch(env, site, application, directory, "Hitchhiker", "en",
				PARSE_FIELDS, new EnglishAnalyzer(), "span", new HashMap<>());
		validate("Hitchhiker", doSearch, "search_result_en.json", "search-en");
	}

	@Test
	public void testSearchEnFoo() throws IOException, URISyntaxException {
		Iterable<Document> doSearch = standardSearcher.doSearch(env, site, application, directory, "foo", "en",
				PARSE_FIELDS, new EnglishAnalyzer(), "span", new HashMap<>());
		validate("foo", doSearch, "search_result_en_foo.json", "search-en");
	}

	@Test
	public void testSearchEnNoResult() throws IOException, URISyntaxException {
		Iterable<Document> doSearch = standardSearcher.doSearch(env, site, application, directory, "ACME", "en",
				PARSE_FIELDS, new EnglishAnalyzer(), "span", new HashMap<>());
		validate("ACME", doSearch, "search_result_en_no_result.json", "search-en");
	}

	@Test
	public void testSearchDe() throws IOException, URISyntaxException {
		Iterable<Document> doSearch = standardSearcher.doSearch(env, site, application, directory, "Anhalter", "de",
				PARSE_FIELDS, new GermanAnalyzer(), "span", new HashMap<>());
		SearchFormatter searchFormatter = validate("Anhalter", doSearch, "search_result_de.json", "search-de");
		ClassLoader classLoader = getClass().getClassLoader();
		File xslStylesheet = new File(classLoader.getResource("transform.xsl").toURI());
		searchFormatter.setXslStylesheet(xslStylesheet);
		StringWriter html = new StringWriter();
		searchFormatter.write(html);
		compare("search_result_de.html", html.toString(), classLoader);

		searchFormatter.setDoXsl(false);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		searchFormatter.write(out);
		Assert.assertTrue(out.toString(StandardCharsets.UTF_8).startsWith("<!--"));
		Assert.assertTrue(out.toString(StandardCharsets.UTF_8).endsWith("-->"));
		searchFormatter.setDoXsl(true);
	}

	protected SearchFormatter validate(String query, Iterable<Document> doSearch, String controlPath, String partName)
			throws IOException, URISyntaxException {
		Part part = new Part(partName);
		Iterator<Document> docs = doSearch.iterator();
		while (docs.hasNext()) {
			part.getData().add(Result.fromDocument(docs.next()));
		}

		SearchFormatter searchFormatter = new SearchFormatter(DocumentBuilderFactory.newInstance(),
				TransformerFactory.newInstance());
		searchFormatter.setPretty(true);
		searchFormatter.setQueryParam(query);
		searchFormatter.getParts().add(part);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		searchFormatter.write(out);

		ClassLoader classLoader = getClass().getClassLoader();
		compare(controlPath, replaceDate(out), classLoader);

		searchFormatter.setFormat(SearchFormatter.FORMAT_XML);
		out = new ByteArrayOutputStream();
		searchFormatter.write(out);
		compare(controlPath.replace(".json", ".xml"), replaceDate(out), classLoader);
		return searchFormatter;
	}

	private String replaceDate(ByteArrayOutputStream out) {
		String result = out.toString(StandardCharsets.UTF_8).replaceAll("\\d{4}-\\d{2}-\\d{2}", "2015-03-27");
		result = result.replaceAll("[0-9]+\\.[0-9]+", "1.0");
		return result;
	}

	protected void compare(String controlPath, String result, ClassLoader classLoader)
			throws URISyntaxException, IOException {
		File controlFile = new File(classLoader.getResource(controlPath).toURI());
		String control = FileUtils.readFileToString(controlFile, StandardCharsets.UTF_8);
		Assert.assertEquals(WritingJsonValidator.normalizeLines(control), WritingJsonValidator.normalizeLines(result));
	}
}
