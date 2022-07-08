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
package org.appng.api.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.rest.model.Action;
import org.appng.api.rest.model.Datasource;
import org.appng.api.rest.model.ErrorModel;
import org.appng.api.rest.model.Link;
import org.appng.api.rest.model.Parameter;
import org.appng.api.rest.model.Sort.OrderEnum;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A simple client for the appNG REST API, open for extension. <strong>Note that this class is NOT thread-safe!</strong>
 */
@Slf4j
public class RestClient {

	private static final String PATH_SEPARATOR = "/";
	protected RestTemplate restTemplate;
	protected Map<String, String> cookies;
	protected String url;
	private ObjectMapper objectMapper;

	/**
	 * Creates a new {@link RestClient}.
	 * <p>
	 * Note that you probably need to perform a login action before you can use this client.
	 * </p>
	 * 
	 * @param url
	 *            the URL pointing to a site's service URL ({@code /service/<site-name>})
	 */
	public RestClient(String url) {
		this(url, new HashMap<>());
	}

	/**
	 * Creates a new {@link RestClient}, using an existing cookie. This cookie should be retrieved from another client
	 * that performed a login action.
	 * 
	 * @param url
	 *                the URL pointing to a site's service URL ({@code /service/<site-name>})
	 * @param cookies
	 *                the cookie to use
	 * 
	 * @see RestClient#getCookies()
	 */
	public RestClient(String url, Map<String, String> cookies) {
		this.url = url;
		this.cookies = cookies;
		this.restTemplate = new RestTemplate(
				Arrays.asList(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter(),
						new MappingJackson2HttpMessageConverter(), new ResourceHttpMessageConverter()));
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			protected boolean hasError(HttpStatus statusCode) {
				return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
			}

		});
		this.objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_ABSENT);
	}

	/**
	 * Retrieves the {@link Datasource}.
	 * 
	 * @param application
	 *                    the name of the application
	 * @param id
	 *                    the ID of the {@link Datasource}
	 * 
	 * @return the {@link Datasource} wrapped in a {@link RestResponseEntity}
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the URI
	 */
	public RestResponseEntity<Datasource> datasource(String application, String id) throws URISyntaxException {
		return datasource(application, id, (Pageable) null);
	}

	/**
	 * Retrieves the {@link Datasource}.
	 * 
	 * @param application
	 *                    the name of the application
	 * @param id
	 *                    the ID of the {@link Datasource}
	 * @param pageable
	 *                    a {@link Pageable} (optional)
	 * 
	 * @return the {@link Datasource} wrapped in a {@link RestResponseEntity}
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the URI
	 */
	public RestResponseEntity<Datasource> datasource(String application, String id, Pageable pageable)
			throws URISyntaxException {
		return datasource(application, id, pageable, null);
	}

	/**
	 * Retrieves the {@link Datasource}.
	 * 
	 * @param application
	 *                    the name of the application
	 * @param id
	 *                    the ID of the {@link Datasource}
	 * @param parameters
	 *                    some additional parameters
	 * 
	 * @return the {@link Datasource} wrapped in a {@link RestResponseEntity}
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the URI
	 */
	public RestResponseEntity<Datasource> datasource(String application, String id,
			MultiValueMap<String, String> parameters) throws URISyntaxException {
		return datasource(application, id, null, parameters);
	}

	/**
	 * Retrieves the {@link Datasource}.
	 * 
	 * @param application
	 *                    the name of the application
	 * @param id
	 *                    the ID of the {@link Datasource}
	 * @param pageable
	 *                    a {@link Pageable} (optional)
	 * @param parameters
	 *                    some additional parameters
	 * 
	 * @return the {@link Datasource} wrapped in a {@link RestResponseEntity}
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the URI
	 */
	public RestResponseEntity<Datasource> datasource(String application, String id, Pageable pageable,
			MultiValueMap<String, String> parameters) throws URISyntaxException {
		StringBuilder uriBuilder = new StringBuilder(url).append(PATH_SEPARATOR).append(application);
		uriBuilder.append("/rest/datasource/").append(id).append("?");
		if (null != pageable) {
			uriBuilder.append("sort").append(StringUtils.capitalize(id)).append("=").append(pageable.getSortQuery())
					.append("&");
		}
		if (null != parameters) {
			parameters.keySet().forEach(key -> {
				parameters.get(key).forEach(value -> {
					uriBuilder.append(key).append("=").append(value).append("&");
				});
			});
		}
		return exchange(new URI(uriBuilder.toString()), null, HttpMethod.GET, Datasource.class);
	}

	/**
	 * Retrieves the {@link Action}.
	 * 
	 * @param application
	 *                      the name of the application
	 * @param eventId
	 *                      the event-ID of the {@link Action}
	 * @param actionId
	 *                      the ID of the {@link Action}
	 * @param pathVariables
	 *                      some additional path variables
	 * 
	 * @return the (unprocessed) {@link Action} wrapped in a {@link RestResponseEntity}
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the URI
	 */
	public RestResponseEntity<Action> getAction(String application, String eventId, String actionId,
			String... pathVariables) throws URISyntaxException {
		URI actionURL = getActionURL(application, eventId, actionId, pathVariables);
		return exchange(actionURL, null, HttpMethod.GET, Action.class);
	}

	private void doLog(String prefix, Object body, HttpStatus httpStatus) {
		if (LOGGER.isDebugEnabled()) {
			String content = StringUtils.EMPTY;
			if (null != body) {
				Class<?> bodyType = body.getClass();
				if (!(bodyType.isPrimitive() || bodyType.isArray())
						&& bodyType.getPackage().getName().startsWith("org.appng.api.rest.model")) {
					try {
						content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
					} catch (JsonProcessingException e) {
						LOGGER.error("error parsing JSON body", e);
					}
				} else {
					content = body.toString();
				}
			}
			Object status = null != httpStatus ? " " + httpStatus.value() : "";
			LOGGER.debug("{}: {} {}", prefix, status, content);
		}
	}

	/**
	 * Retrieves the {@link Action} represented by the {@link Link}
	 * 
	 * @param link
	 *             the {@link Link} representing the {@link Action}'s URI
	 * 
	 * @return the (unprocessed) {@link Action} wrapped in a {@link RestResponseEntity}
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the URI
	 */
	public RestResponseEntity<Action> getAction(Link link) throws URISyntaxException {
		String[] pathSegments = link.getTarget().split(PATH_SEPARATOR);
		String servicePath = StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), PATH_SEPARATOR);
		URI uri = new URI(url + PATH_SEPARATOR + servicePath);
		return exchange(uri, null, HttpMethod.GET, Action.class);
	}

	/**
	 * Performs an {@link Action}, the URI is defined by the {@link Link}.
	 * 
	 * @param data
	 *             the {@link Action}-data to send
	 * @param link
	 *             the {@link Link}
	 * 
	 * @return a {@link RestResponseEntity} wrapping the resulting {@link Action}
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the URI
	 */
	public RestResponseEntity<Action> performAction(Action data, Link link) throws URISyntaxException {
		String[] pathSegments = link.getTarget().split(PATH_SEPARATOR);
		URI uri = new URI(url + PATH_SEPARATOR
				+ StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), PATH_SEPARATOR));
		addFormAction(data);
		return exchange(uri, data, HttpMethod.POST, Action.class);
	}

	private void addFormAction(Action data) {
		Parameter formAction = new Parameter();
		formAction.setName("form_action");
		formAction.setValue(data.getId());
		data.addParametersItem(formAction);
	}

	protected void setCookies(ResponseEntity<?> entity) {
		List<String> setCookies = entity.getHeaders().get(HttpHeaders.SET_COOKIE);
		if (null != setCookies) {
			for (String c : setCookies) {
				int valueStart = c.indexOf('=');
				String name = c.substring(0, valueStart);
				int end = c.indexOf(';');
				String value = c.substring(valueStart + 1, end < 0 ? c.length() : end);
				cookies.put(name, value);
				LOGGER.debug("received cookie: {}={}", name, value);
			}
		}
	}

	protected URI getActionURL(String application, String eventId, String actionId, String[] pathVariables)
			throws URISyntaxException {
		String uriString = String.format("%s/%s/rest/action/%s/%s/%s", url, application, eventId, actionId,
				StringUtils.join(pathVariables, PATH_SEPARATOR));
		return new URI(uriString);
	}

	/**
	 * Performs an {@link Action}.
	 * 
	 * @param application
	 *                      the name of the application
	 * @param data
	 *                      the {@link Action}-data to send
	 * @param pathVariables
	 *                      some additional path variables
	 * 
	 * @return a {@link RestResponseEntity} wrapping the resulting {@link Action}
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the URI
	 */
	public RestResponseEntity<Action> performAction(String application, Action data, String... pathVariables)
			throws URISyntaxException {
		addFormAction(data);
		URI actionURL = getActionURL(application, data.getEventId(), data.getId(), pathVariables);
		return exchange(actionURL, data, HttpMethod.POST, Action.class);
	}

	protected HttpHeaders getHeaders(boolean acceptAnyType) {
		HttpHeaders headers = new HttpHeaders();
		if (!cookies.isEmpty()) {
			cookies.keySet().forEach(k -> {
				String cookie = cookies.get(k);
				headers.add(HttpHeaders.COOKIE, k + "=" + cookie);
				LOGGER.debug("sent cookie: {}={}", k, cookies.get(k));
			});
		}
		headers.setContentType(MediaType.APPLICATION_JSON);
		List<MediaType> acceptableMediaTypes;
		if (acceptAnyType) {
			acceptableMediaTypes = Arrays.asList(MediaType.ALL);
		} else {
			acceptableMediaTypes = Arrays.asList(MediaType.APPLICATION_JSON);
		}
		headers.setAccept(acceptableMediaTypes);
		headers.set(HttpHeaders.USER_AGENT, "appNG Rest Client");
		return headers;
	}

	/**
	 * Returns the current cookies or this client
	 * 
	 * @return the cookie's map
	 */
	public Map<String, String> getCookies() {
		return cookies;
	}

	/**
	 * Returns the resource represented by the link as binary data
	 * 
	 * @param link
	 *             the ink to there resource
	 * 
	 * @return the resource
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the link
	 */
	public RestResponseEntity<byte[]> getBinaryData(Link link) throws URISyntaxException {
		String path = getRelativePathFromLink(link);
		return getResource(path, byte[].class);
	}

	private String getRelativePathFromLink(Link link) {
		String[] pathSegments = link.getTarget().split(PATH_SEPARATOR);
		String path = PATH_SEPARATOR
				+ StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), PATH_SEPARATOR);
		return path;
	}

	/**
	 * Returns the resource represented by the relative path
	 * 
	 * @param relativePath
	 *                     the relative path, e.g. <code>/application/rest/downloads/4711</code>
	 * 
	 * @return the resource
	 * 
	 * @throws URISyntaxException
	 *                            if something is wrong with the path
	 */
	public RestResponseEntity<byte[]> getBinaryData(String relativePath) throws URISyntaxException {
		return getResource(relativePath, byte[].class);
	}

	/**
	 * Retrieves a REST-Resource with a given HTTP method.<br/>
	 * Example:
	 * 
	 * <pre>
	 * ResponseEntity<Integer> result = restClient.retrieveResource("/application/rest/calculator/add/47/11", null, Integer.class, HttpMethod.GET)
	 * </pre>
	 * 
	 * @param path
	 *                   the relative path to the resource, starting with the application's name
	 * @param body
	 *                   the request body (optional)
	 * @param returnType
	 *                   the type of the response
	 * @param method
	 *                   the {@link HttpMethod} to use
	 * 
	 * @return the {@link RestResponseEntity}
	 * 
	 * @throws URISyntaxException
	 */
	public <OUT, IN> RestResponseEntity<IN> exchange(String path, OUT body, Class<IN> returnType, HttpMethod method)
			throws URISyntaxException {
		return exchange(new URI(url + path), body, method, returnType);
	}

	protected <IN, OUT> RestResponseEntity<IN> exchange(URI uri, OUT body, HttpMethod method, Class<IN> returnType) {
		return exchange(uri, body, method, returnType, false);
	}

	protected <IN, OUT> RestResponseEntity<IN> exchange(URI uri, OUT body, HttpMethod method, Class<IN> returnType,
			boolean acceptAnyType) {
		if (LOGGER.isDebugEnabled() && body != null) {
			doLog("OUT", body, null);
		}
		try {
			RequestEntity<OUT> out = new RequestEntity<>(body, getHeaders(acceptAnyType), method, uri);
			ResponseEntity<IN> in = restTemplate.exchange(out, returnType);
			setCookies(in);
			if (LOGGER.isDebugEnabled() && in.getBody() != null) {
				doLog("IN", in.getBody(), in.getStatusCode());
			}
			return RestResponseEntity.of(in);
		} catch (HttpServerErrorException e) {
			ErrorModel errorModel = null;
			try {
				String bodyAsString = e.getResponseBodyAsString();
				if (StringUtils.isNotBlank(bodyAsString)) {
					errorModel = objectMapper.readerFor(ErrorModel.class).readValue(bodyAsString);
				}
			} catch (IOException ioe) {
				LOGGER.error("could not read error from response", e);
			}
			if (null == errorModel) {
				errorModel = new ErrorModel();
				errorModel.setCode(e.getStatusCode().value());
				errorModel.setMessage(e.getMessage());
			}
			return new RestResponseEntity<>(errorModel, e.getResponseHeaders(), e.getStatusCode());
		}
	}

	/**
	 * Retrieves a REST-Resource with HTTP GET.<br/>
	 * Example:
	 * 
	 * <pre>
	 * ResponseEntity<Integer> result = restClient.retrieveResource("/application/rest/calculator/add/47/11", null, Integer.class, HttpMethod.GET)
	 * </pre>
	 * 
	 * @param path
	 *                   the relative path to the resource, starting with the application's name
	 * @param returnType
	 *                   the type of the response
	 * 
	 * @return the {@link RestResponseEntity}
	 * 
	 * @throws URISyntaxException
	 */
	public <IN> RestResponseEntity<IN> getResource(String path, Class<IN> returnType) throws URISyntaxException {
		return exchange(new URI(url + path), null, HttpMethod.GET, returnType, true);
	}

	/**
	 * Wraps paging and sorting
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Pageable {
		private int page = 0;
		private int pageSize = 10;
		private Map<String, OrderEnum> fieldSorts = new HashMap<>();
		private boolean reset = false;

		/**
		 * Creates a new pageable without any sorting
		 * 
		 * @param page
		 *                 the zero-indexed page number
		 * @param pageSize
		 *                 the size of a page
		 **/
		public Pageable(int page, int pageSize) {
			this(page, pageSize, false);
		}

		/**
		 * Creates a new pageable without any sorting
		 * 
		 * @param page
		 *                 the zero-indexed page number
		 * @param pageSize
		 *                 the size of a page
		 * @param reset
		 *                 set to {@code true} to reset current sort criteria
		 **/
		public Pageable(int page, int pageSize, boolean reset) {
			this.page = page;
			this.pageSize = pageSize;
			this.reset = reset;
		}

		/**
		 * Creates a new pageable
		 * 
		 * @param page
		 *                 the zero-indexed page number
		 * @param pageSize
		 *                 the size of a page
		 * @param field
		 *                 the field to sort
		 * @param order
		 *                 the direction to sort
		 **/
		public Pageable(int page, int pageSize, String field, OrderEnum order) {
			this(page, pageSize, field, order, false);
		}

		/**
		 * Creates a new pageable
		 * 
		 * @param page
		 *                 the zero-indexed page number
		 * @param pageSize
		 *                 the size of a page
		 * @param field
		 *                 the field to sort
		 * @param order
		 *                 the direction to sort
		 * @param reset
		 *                 set to {@code true} to reset current sort criteria
		 */
		public Pageable(int page, int pageSize, String field, OrderEnum order, boolean reset) {
			setPage(page);
			setPageSize(pageSize);
			addSort(field, order);
			this.reset = reset;
		}

		/**
		 * Adds a sort criteria for the given field.
		 * 
		 * @param field
		 *                  the field to sort
		 * @param direction
		 *                  the direction to sort
		 * 
		 * @return this {@link Pageable}
		 */
		public Pageable addSort(String field, OrderEnum direction) {
			fieldSorts.put(field, direction);
			return this;
		}

		/**
		 * Creates a query string (matrix-parameter style) containing all the sort criteria
		 * 
		 * @return the query string
		 */
		public String getSortQuery() {
			StringBuilder sortBuilder = new StringBuilder();
			sortBuilder.append("page:").append(page).append(";");
			sortBuilder.append("pageSize:").append(pageSize).append(";");
			if (fieldSorts != null) {
				for (String field : fieldSorts.keySet()) {
					sortBuilder.append(field).append(":").append(fieldSorts.get(field).getValue()).append(";");
				}
			}
			if (reset) {
				sortBuilder.append("reset");
			}
			return sortBuilder.toString();
		}

	}
}
