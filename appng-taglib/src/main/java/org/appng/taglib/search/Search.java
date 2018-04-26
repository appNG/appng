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
package org.appng.taglib.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.search.Document;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.search.SearchProvider;
import org.appng.search.json.Part;
import org.appng.search.json.Result;
import org.appng.search.searcher.SearchFormatter;
import org.appng.search.searcher.StandardSearcher;
import org.appng.taglib.MultiSiteSupport;
import org.appng.taglib.ParameterOwner;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StopWatch;

/**
 * This class represents a Search Tag used in JSP. A {@link Search} can contain multiple {@link SearchPart}s that use
 * different {@link Application}s.
 * <p/>
 * <b>Attributes (defaults in brackets):</b>
 * <ul>
 * <li>format - one of {@code xml} or {@code json}</li>
 * <li>parts (false) - whether the resulting XML/JSON should be split in parts</li>
 * <li>highlight (span) - the x(ht)ml-tag used to highlight the search term within the search results.
 * </ul>
 * <p/>
 * <b>Parameters:</b> The following parameters (&lt;appNG:param>) are supported (defaults in brackets):
 * <ul>
 * <li>pageSize ({@value org.appng.search.searcher.SearchFormatter#DEFAULT_PAGESIZE})<br/>
 * the page size to use</li>
 * <li>pageSizeParam ({@value org.appng.search.searcher.SearchFormatter#DEFAULT_PAGESIZE})<br/>
 * the name of the request parameter that contains the page-size</li>
 * <li>pageParam ({@value org.appng.search.searcher.SearchFormatter#DEFAULT_PAGE_PARAM})<br/>
 * the name of the request parameter that contains the current page</li>
 * <li>queryParam ({@value org.appng.search.searcher.SearchFormatter#DEFAULT_QUERY_PARAM})<br/>
 * the name of the request parameter that contains the search term</li>
 * <li>maxTextLength (150)<br/>
 * the maximum length of a search result text</li>
 * <li>dateFormat ({@value org.appng.search.searcher.SearchFormatter#DEFAULT_DATE_PATTERN})<br/>
 * the date pattern used to format dates</li>
 * <li>fillWith (...)<br/>
 * the placeholder used when the search result text is being stripped</li>
 * <li>xsl (<none>)<br/>
 * the path to the XSLT stylesheet to use when format is XML</li>
 * <li>pretty (false)<br/>
 * if the XML/JSON output should be formatted prettily</li>
 * </ul>
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;appNG:search parts="false" format="json" highlight="span">
 *	&lt;appNG:param name="queryParam">term&lt;/appNG:param>
 *	&lt;appNG:searchPart application="global" language="de" title="Search Results" fields="title,contents" analyzerClass="org.apache.lucene.analysis.de.GermanAnalyzer"/>
 * &lt;/appNG:search>
 * </pre>
 * 
 * @see SearchPart
 * 
 * @author Matthias Müller
 */
public class Search extends BodyTagSupport implements ParameterOwner {

	private static final Logger log = LoggerFactory.getLogger(Search.class);

	private static final String PARAM_FILL_WITH = "fillWith";
	private static final String PARAM_PAGE_SIZE_PARAM = "pageSizeParam";
	private static final String PARAM_PAGE_SIZE = "pageSize";
	private static final String PARAM_PAGE_PARAM = "pageParam";
	private static final String PARAM_XSL = "xsl";
	private static final String PARAM_DATE_FORMAT = "dateFormat";
	private static final String PARAM_MAX_TEXT_LENGTH = "maxTextLength";
	private static final String PARAM_QUERY_PARAM = "queryParam";
	private static final String PARAM_PRETTY = "pretty";

	private String format;
	private boolean useParts;
	private String highlight;
	private List<SearchPart> parts = new ArrayList<SearchPart>();
	private Map<String, String> parameters = new HashMap<String, String>();

	@Override
	public int doEndTag() throws JspException {
		ServletRequest servletRequest = pageContext.getRequest();
		String queryParamName = getParam(PARAM_QUERY_PARAM, SearchFormatter.DEFAULT_QUERY_PARAM);
		String queryParam = StringEscapeUtils.unescapeHtml4(servletRequest.getParameter(queryParamName));

		if (StringUtils.isNotBlank(queryParam) && !parts.isEmpty()) {
			log.debug("term is {}", queryParam);
			Environment env = DefaultEnvironment.get(pageContext);
			Site site = RequestUtil.getSite(env, servletRequest);
			Properties siteProperties = site.getProperties();
			String siteRootDir = siteProperties.getString(SiteProperties.SITE_ROOT_DIR);
			String seIndex = siteRootDir + siteProperties.getString(SiteProperties.INDEX_DIR);
			File indexDir = new File(seIndex);
			try (Directory directory = FSDirectory.open(indexDir.toPath())) {
				StopWatch sw = new StopWatch();
				sw.start();

				ApplicationContext ctx = env.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);

				List<Part> results = new ArrayList<Part>();

				Integer maxTextLength = Integer.parseInt(getParam(PARAM_MAX_TEXT_LENGTH, "150"));
				String fillWith = getParam(PARAM_FILL_WITH, "...");
				for (SearchPart part : parts) {
					Part result = processPart(env, (HttpServletRequest) servletRequest, site, part, queryParam,
							directory, maxTextLength, fillWith);
					if (null != result) {
						results.add(result);
					}
				}
				DocumentBuilderFactory dbf = ctx.getBean(DocumentBuilderFactory.class);
				TransformerFactory tf = ctx.getBean(TransformerFactory.class);
				SearchFormatter searchFormatter = new SearchFormatter(dbf, tf);

				boolean pretty = "true".equalsIgnoreCase(servletRequest.getParameter(PARAM_PRETTY))
						|| "true".equalsIgnoreCase(getParam(PARAM_PRETTY, null));
				String dateFormat = getParam(PARAM_DATE_FORMAT, SearchFormatter.DEFAULT_DATE_PATTERN);
				String pageParamName = getParam(PARAM_PAGE_PARAM, SearchFormatter.DEFAULT_PAGE_PARAM);
				String pageParam = servletRequest.getParameter(pageParamName);
				if (StringUtils.isNotBlank(pageParam)) {
					searchFormatter.setPage(Integer.valueOf(pageParam));
				}
				String pageSizeParamName = getParam(PARAM_PAGE_SIZE_PARAM, SearchFormatter.DEFAULT_PAGE_SIZE_PARAM);
				String pageSizeParam = servletRequest.getParameter(pageSizeParamName);
				if (StringUtils.isBlank(pageSizeParam)) {
					pageSizeParam = getParam(PARAM_PAGE_SIZE, null);
				}
				if (StringUtils.isNotBlank(pageSizeParam)) {
					searchFormatter.setPageSize(Integer.valueOf(pageSizeParam));
				}

				searchFormatter.setPretty(pretty);
				searchFormatter.setDateFormat(dateFormat);
				searchFormatter.setFormat(format);
				searchFormatter.setPageParamName(pageParamName);
				searchFormatter.setPageSizeParamName(pageSizeParamName);
				searchFormatter.setQueryParam(queryParam);
				searchFormatter.setQueryParamName(queryParamName);
				searchFormatter.setUseParts(useParts);
				searchFormatter.setParts(results);
				searchFormatter.setDoXsl(!"false".equalsIgnoreCase(servletRequest.getParameter(PARAM_XSL)));
				String xsl = getParam(PARAM_XSL, null);
				if (StringUtils.isNotEmpty(xsl)) {
					searchFormatter.setXslStylesheet(site.readFile(xsl));
				}
				sw.stop();
				searchFormatter.setTime(sw.getTotalTimeMillis());
				searchFormatter.write(pageContext.getOut());

			} catch (IOException e) {
				log.error("error in doStartTag()", e);
			}
		} else {
			log.debug("no term given or empty parts");
		}
		clear();
		return super.doEndTag();
	}

	protected Part processPart(Environment env, HttpServletRequest servletRequest, Site site, SearchPart part,
			String term, Directory directory, Integer maxTextLength, String fillWith) throws JspException {

		String applicationName = part.getApplication();
		SearchProvider searchProvider = null;
		Application application = null;
		Site executingSite = null;
		if ("global".equals(applicationName)) {
			searchProvider = new StandardSearcher();
			executingSite = site;
		} else {
			MultiSiteSupport multiSiteSupport = new MultiSiteSupport();
			multiSiteSupport.process(env, applicationName, part.getMethod(), servletRequest);
			application = multiSiteSupport.getApplicationProvider();
			executingSite = multiSiteSupport.getExecutingSite();
			if (null != application) {
				searchProvider = application.getBean(part.getMethod(), SearchProvider.class);
			} else {
				log.warn("application {} not found for site {}", applicationName, executingSite.getName());
			}
		}

		if (null != searchProvider) {
			try {
				String[] parseFields = StringUtils.split(part.getFields(), ',');
				String language = part.getLanguage();

				Map<String, String> parameters = part.getParameters();

				log.info("processing {} with term '{}' and parameters {}", searchProvider.getClass().getName(), term,
						parameters);
				Iterable<Document> doSearch = searchProvider.doSearch(env, executingSite, application, directory, term,
						language, parseFields, part.getAnalyzer(), getHighlight(), parameters);
				Part resultPart = new Part(part.getTitle());

				for (Document d : doSearch) {
					Result r = Result.fromDocument(d);
					r.setText(cleanText(r.getText(), maxTextLength, fillWith));
					resultPart.getData().add(r);
				}
				return resultPart;
			} catch (IOException e) {
				log.error("error performing doSearch() for " + searchProvider.getClass().getName(), e);
			} catch (ReflectiveOperationException e) {
				log.error("error creating analyzer " + part.getAnalyzerClass() + " for "
						+ searchProvider.getClass().getName(), e);
			}
		} else {
			log.warn("no SearchProvider named {} found for application {}", part.getMethod(), applicationName);
		}
		return null;
	}

	private String getParam(String name, String defaultValue) {
		return parameters.containsKey(name) ? parameters.get(name) : defaultValue;
	}

	public String cleanText(String description, Integer maxTextLength, String fillWith) {
		if (null != description) {
			if (maxTextLength > 0) {
				int spaceIdx = description.indexOf(" ", maxTextLength - 3);
				if (spaceIdx > 0) {
					return Jsoup.clean(description.substring(0, spaceIdx) + fillWith, Whitelist.none());
				}
			}
			return Jsoup.clean(description, Whitelist.none());
		}
		return null;
	}

	private void clear() {
		format = null;
		highlight = null;
		useParts = true;
		parameters.clear();
		parts.clear();
	}

	public void addPart(SearchPart p) {
		parts.add(p);
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public boolean isParts() {
		return useParts;
	}

	public void setParts(boolean useParts) {
		this.useParts = useParts;
	}

	public String getHighlight() {
		return highlight;
	}

	public void setHighlight(String highlight) {
		this.highlight = highlight;
	}

	public void addParameter(String name, String value) {
		parameters.put(name, value);
	}

	protected List<SearchPart> getParts() {
		return parts;
	}

}
