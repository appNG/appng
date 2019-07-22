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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.appng.search.json.FilterConfig;
import org.appng.search.json.FilterData;
import org.appng.search.json.FilterItem;
import org.appng.search.json.Json;
import org.appng.search.json.Page;
import org.appng.search.json.Part;
import org.appng.search.json.Result;
import org.appng.search.json.Results;
import org.appng.search.json.Search;
import org.appng.search.json.SearchFilter;
import org.w3c.dom.Document;

/**
 * A {@link SearchFormatter} is responsible for formatting the {@link Result}s of a search, which are grouped into
 * several {@link Part}s. It supports XML and JSON as output formats, optionally applying pagination.
 * 
 * @author Matthias Müller
 * 
 * @see Part
 * @see Result
 *
 */
public class SearchFormatter {

	public static final int DEFAULT_PAGE = 0;
	public static final String DEFAULT_PAGE_PARAM = "page";
	public static final int DEFAULT_PAGESIZE = 25;
	public static final String DEFAULT_PAGE_SIZE_PARAM = "pageSize";
	public static final String DEFAULT_QUERY_PARAM = "q";
	public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
	public static final String FORMAT_JSON = "json";
	public static final String FORMAT_XML = "xml";

	private int page = DEFAULT_PAGE;
	private String pageParamName = DEFAULT_PAGE_PARAM;
	private int pageSize = DEFAULT_PAGESIZE;
	private String pageSizeParamName = DEFAULT_PAGE_SIZE_PARAM;
	private String query;
	private String queryParamName = DEFAULT_QUERY_PARAM;
	private boolean pretty = false;
	private String format = FORMAT_JSON;
	private boolean useParts = true;
	private boolean doXsl = true;
	private long time = 0L;
	private String dateFormat = DEFAULT_DATE_PATTERN;
	private File xslStylesheet = null;
	private static final String XML_COMMENT_OPEN = "<!-- ";
	private static final String XML_COMMENT_CLOSE = "-->";
	private List<Part> parts = new ArrayList<>();

	private DocumentBuilderFactory documentBuilderFactory;
	private TransformerFactory transformerFactory;
	private static JAXBContext jaxbContext;

	static {
		try {
			jaxbContext = JAXBContext.newInstance("org.appng.search.json");
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		}
	}

	public SearchFormatter(DocumentBuilderFactory documentBuilderFactory, TransformerFactory transformerFactory) {
		this.documentBuilderFactory = documentBuilderFactory;
		this.transformerFactory = transformerFactory;
	}

	public void write(OutputStream out) throws IOException {
		StringWriter writer = new StringWriter();
		write(writer);
		out.write(writer.toString().getBytes("UTF-8"));
	}

	public void write(Writer writer) throws IOException {
		DateFormat sdf = new SimpleDateFormat(dateFormat);
		if (FORMAT_JSON.equalsIgnoreCase(format)) {
			Object jsonResult;
			Json json = new Json(sdf, pretty);
			if (useParts) {
				jsonResult = parts;
			} else {
				jsonResult = paginate(getSortedDocs());
			}
			writer.write(json.toJson(jsonResult));
		} else if (FORMAT_XML.equalsIgnoreCase(format)) {
			StringWriter tempWriter = new StringWriter();
			processXML(tempWriter, getSortedDocs(), sdf);
			writer.write(tempWriter.toString());
		} else {
			throw new IOException(String.format("Invalid format: %s", format));
		}
	}

	protected List<Result> getSortedDocs() {
		List<Result> sortedDocs = new ArrayList<>();
		for (Part part : parts) {
			sortedDocs.addAll(part.getData());
		}
		Collections.sort(sortedDocs);
		return sortedDocs;
	}

	protected void processXML(Writer writer, List<Result> sortedDocs, DateFormat dateFormat) throws IOException {
		Search<SearchFilter> paginate = paginate(sortedDocs);
		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setAdapter(DateAdapter.class, new DateAdapter(dateFormat));
			Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
			marshaller.marshal(paginate, document);
			Transformer transformer;

			if (doXsl && null != xslStylesheet && xslStylesheet.isFile()) {
				transformer = transformerFactory.newTemplates(new StreamSource(xslStylesheet)).newTransformer();
			} else {
				transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "title fragment text");
				transformer.setOutputProperty(OutputKeys.INDENT, pretty ? "yes" : "no");
			}
			if (!doXsl) {
				writer.write(XML_COMMENT_OPEN);
			}
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			if (!doXsl) {
				writer.write(XML_COMMENT_CLOSE);
			}
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerException e) {
			throw new IOException(e);
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

	protected Search<SearchFilter> paginate(List<Result> sortedDocs) {
		Search<SearchFilter> searchResult = new Search<SearchFilter>();
		Results results = new Results();
		results.setTime(time);
		searchResult.setResults(results);
		int startIdx = page * pageSize;
		int numberOfItems = sortedDocs.size();
		if (startIdx > numberOfItems) {
			startIdx = 0;
		}
		int toIndex = startIdx + pageSize;
		if (toIndex > numberOfItems) {
			toIndex = numberOfItems;
		}
		List<Result> resultList = sortedDocs.subList(startIdx, toIndex);
		results.getData().addAll(resultList);

		Integer pages = numberOfItems / pageSize + ((numberOfItems % pageSize > 0) ? 1 : 0);

		Page pagination = new Page();
		pagination.setNumberOfElements(sortedDocs.size());
		pagination.setNumberOfPages(pages);
		pagination.setPage(page);
		pagination.setPageSize(pageSize);
		pagination.setPageParam(pageParamName);
		pagination.setPageSizeParam(pageSizeParamName);
		searchResult.getResults().setPagination(pagination);

		final FilterItem searchFilter = new FilterItem();
		FilterConfig config = new FilterConfig();

		config.setName(queryParamName);
		searchFilter.setConfig(config);
		FilterData filterData = new FilterData();
		filterData.setValue(query);
		filterData.setActive(true);
		searchFilter.getData().add(filterData);

		SearchFilter filter = new SearchFilter(searchFilter);
		searchResult.setFilter(filter);

		return searchResult;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public String getPageParamName() {
		return pageParamName;
	}

	public void setPageParamName(String pageParamName) {
		this.pageParamName = pageParamName;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public String getPageSizeParamName() {
		return pageSizeParamName;
	}

	public void setPageSizeParamName(String pageSizeParamName) {
		this.pageSizeParamName = pageSizeParamName;
	}

	public String getQueryParamName() {
		return queryParamName;
	}

	public void setQueryParamName(String queryParamName) {
		this.queryParamName = queryParamName;
	}

	public boolean isPretty() {
		return pretty;
	}

	public void setPretty(boolean pretty) {
		this.pretty = pretty;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public boolean isUseParts() {
		return useParts;
	}

	public void setUseParts(boolean useParts) {
		this.useParts = useParts;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public List<Part> getParts() {
		return parts;
	}

	public void setParts(List<Part> parts) {
		this.parts = parts;
	}

	public String getQueryParam() {
		return query;
	}

	public void setQueryParam(String queryParam) {
		this.query = queryParam;
	}

	public File getXslStylesheet() {
		return xslStylesheet;
	}

	public void setXslStylesheet(File xslStylesheet) {
		this.xslStylesheet = xslStylesheet;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public boolean isDoXsl() {
		return doXsl;
	}

	public void setDoXsl(boolean doXsl) {
		this.doXsl = doXsl;
	}

}
