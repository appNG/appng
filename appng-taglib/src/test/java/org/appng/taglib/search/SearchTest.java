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
package org.appng.taglib.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.VHostMode;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.search.Document;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

public class SearchTest extends Search {

	private static final String LOCALHOST = "localhost";

	@Mock
	private PageContext pageContext;

	@Mock
	private HttpServletRequest servletRequest;

	@Mock
	private HttpServletResponse servletResponse;

	@Mock
	private ServletContext servletContext;

	@Mock
	private HttpSession session;

	@Mock
	private Properties platformProperties;

	@Mock
	private Site site;

	@Mock
	private Properties siteProperties;

	@Mock
	private JspWriter jspWriter;

	@Test
	public void testSearch() throws Exception {
		MockitoAnnotations.openMocks(this);
		Mockito.when(pageContext.getRequest()).thenReturn(servletRequest);
		Mockito.when(pageContext.getResponse()).thenReturn(servletResponse);
		Mockito.when(pageContext.getServletContext()).thenReturn(servletContext);
		Mockito.when(pageContext.getSession()).thenReturn(session);
		Mockito.when(pageContext.getOut()).thenReturn(jspWriter);
		final List<String> result = new ArrayList<>();
		Answer<Void> mockWriter = new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) throws Throwable {
				result.add((String) invocation.getArguments()[0]);
				return null;
			}
		};
		Mockito.doAnswer(mockWriter).when(jspWriter).print(Mockito.anyString());
		Mockito.doAnswer(mockWriter).when(jspWriter).write(Mockito.anyString());
		ConcurrentMap<String, Object> platformEnv = new ConcurrentHashMap<>();
		Mockito.when(servletContext.getAttribute(Scope.PLATFORM.name())).thenReturn(platformEnv);
		Mockito.when(platformProperties.getString(Platform.Property.VHOST_MODE))
				.thenReturn(VHostMode.NAME_BASED.name());
		platformEnv.put(Platform.Environment.PLATFORM_CONFIG, platformProperties);

		Map<String, Site> siteMap = new HashMap<>();
		siteMap.put(LOCALHOST, site);
		platformEnv.put("sites", siteMap);
		
		Mockito.when(site.getHost()).thenReturn(LOCALHOST);
		Mockito.when(site.getProperties()).thenReturn(siteProperties);

		Mockito.when(servletRequest.getServerName()).thenReturn(LOCALHOST);
		Mockito.when(servletRequest.getServletContext()).thenReturn(servletContext);
		Mockito.when(servletRequest.getSession()).thenReturn(session);
		Mockito.when(servletRequest.getParameter("xsl")).thenReturn("false");
		Mockito.when(servletRequest.getParameter("q")).thenReturn("Hitchhiker");
		Mockito.when(servletRequest.getServletPath()).thenReturn("/repository/site/www/de/index.jsp");

		
		Mockito.when(siteProperties.getString(SiteProperties.INDEX_CONFIG))
		.thenReturn("/de;de;GermanAnalyzer|/en;en;EnglishAnalyzer");
		Mockito.when(siteProperties.getString(SiteProperties.SITE_ROOT_DIR)).thenReturn("");
		Mockito.when(siteProperties.getString(SiteProperties.INDEX_DIR)).thenReturn("target/index");
		
		setPageContext(pageContext);
		setFormat("json");
		setParts(true);
		SearchPart globalPart = new SearchPart();
		globalPart.setApplication("global");
		globalPart.setLanguage("en");
		globalPart.setFields(Document.FIELD_TITLE);
		globalPart.setAnalyzerClass(EnglishAnalyzer.class.getName());
		globalPart.addParameter("excludeTypes", "com.foo.Bar");
		globalPart.setParent(this);
		globalPart.doEndTag();
		Assert.assertNull(globalPart.getAnalyzerClass());
		Assert.assertNull(globalPart.getApplication());
		Assert.assertNull(globalPart.getFields());
		Assert.assertNull(globalPart.getLanguage());
		Assert.assertNull(globalPart.getMethod());
		Assert.assertNull(globalPart.getTitle());
		Assert.assertTrue(globalPart.getParameters().isEmpty());

		String siteRootDir = siteProperties.getString(SiteProperties.SITE_ROOT_DIR);
		String seIndex = siteRootDir + siteProperties.getString(SiteProperties.INDEX_DIR);
		File indexDir = new File(seIndex);
		indexDir.mkdirs();
		Directory directory = FSDirectory.open(indexDir.toPath());
		Analyzer analyzer = new EnglishAnalyzer();
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter = new IndexWriter(directory, conf);

		indexWriter.deleteDocuments(new MatchAllDocsQuery());
		List<? extends IndexableField> doc1 = Arrays.asList(
				new TextField(Document.FIELD_TITLE, "A Hitchhiker", Store.YES),
				new StringField(Document.FIELD_LANGUAGE, "en", Store.YES));
		indexWriter.addDocument(doc1);
		List<? extends IndexableField> doc2 = Arrays.asList(
				new TextField(Document.FIELD_TITLE, "The Hitchhiker's Guide to the Galaxy", Store.YES),
				new StringField(Document.FIELD_LANGUAGE, "de", Store.YES));
		indexWriter.addDocument(doc2);
		List<? extends IndexableField> doc3 = Arrays.asList(
				new StringField(Document.FIELD_TYPE, "com.foo.Bar", Store.YES),
				new StringField(Document.FIELD_LANGUAGE, "en", Store.YES));
		indexWriter.addDocument(doc3);
		indexWriter.commit();
		indexWriter.close();

		ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
		Mockito.when(ctx.getBean(DocumentBuilderFactory.class)).thenReturn(DocumentBuilderFactory.newInstance());
		Mockito.when(ctx.getBean(TransformerFactory.class)).thenReturn(TransformerFactory.newInstance());
		platformEnv.put(Platform.Environment.CORE_PLATFORM_CONTEXT, ctx);

		doEndTag();
		Assert.assertEquals(
				"[{\"data\":[{\"title\":\"A Hitchhiker\",\"language\":\"en\",\"score\":0.31782177,\"fields\":[]}]}]",
				result.get(0));
		Assert.assertNull(getFormat());
		Assert.assertNull(getHighlight());
		Assert.assertTrue(getParts().isEmpty());
	}
}
