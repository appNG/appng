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
package org.appng.api.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.rest.model.Action;
import org.appng.api.rest.model.Datasource;
import org.appng.api.rest.model.ErrorModel;
import org.appng.api.rest.model.Link;
import org.appng.api.rest.model.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * A simple client for the appNG REST API, open for extension.
 * 
 * <strong>Note that this class is NOT thread-safe!</strong>
 */
@Slf4j
public class RestClient {

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
	 *            the URL pointing to a {@link Site}'s service URL ({@code /service/<site-name>})
	 */
	public RestClient(String url) {
		this(url, new HashMap<>());
	}

	/**
	 * Creates a new {@link RestClient}, using an existing cookie. This cookie should be retrieved from another client
	 * that performed a login action.
	 * 
	 * @param url
	 *            the URL pointing to a {@link Site}'s service URL ({@code /service/<site-name>})
	 * @param cookies
	 *            the cookie to use
	 * 
	 * @see RestClient#getCookies()
	 */
	public RestClient(String url, Map<String, String> cookies) {
		this.url = url;
		this.cookies = cookies;
		this.restTemplate = new RestTemplate(Arrays.asList(new MappingJackson2HttpMessageConverter()));
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
	 *            the name of the {@link Application}
	 * @param id
	 *            the ID of the {@link Datasource}
	 * @return the {@link Datasource} wrapped in a {@link RestResponseEntity}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public RestResponseEntity<Datasource> datasource(String application, String id) throws URISyntaxException {
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeaders(), HttpMethod.GET,
				new URI(url + "/" + application + "/rest/datasource/" + id));
		return send(httpEntity, Datasource.class);
	}

	/**
	 * Retrieves the {@link Datasource}.
	 * 
	 * @param application
	 *            the name of the {@link Application}
	 * @param id
	 *            the ID of the {@link Datasource}
	 * @param parameters
	 *            some additional parameters
	 * @return the {@link Datasource} wrapped in a {@link RestResponseEntity}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public RestResponseEntity<Datasource> datasource(String application, String id,
			MultiValueMap<String, String> parameters) throws URISyntaxException {
		StringBuilder uriBuilder = new StringBuilder(url + "/" + application + "/rest/datasource/" + id + "?");
		parameters.keySet().forEach(key -> {
			parameters.get(key).forEach(value -> {
				uriBuilder.append(key).append("=").append(value).append("&");
			});
		});
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeaders(), HttpMethod.GET, new URI(uriBuilder.toString()));
		return send(httpEntity, Datasource.class);
	}

	/**
	 * Retrieves the {@link Action}.
	 * 
	 * @param application
	 *            the name of the {@link Application}
	 * @param eventId
	 *            the event-ID of the {@link Action}
	 * @param actionId
	 *            the ID of the {@link Action}
	 * @param pathVariables
	 *            some additional path variables
	 * @return the (unprocessed) {@link Action} wrapped in a {@link RestResponseEntity}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public RestResponseEntity<Action> getAction(String application, String eventId, String actionId,
			String... pathVariables) throws URISyntaxException {
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeaders(), HttpMethod.GET,
				getActionURL(application, eventId, actionId, pathVariables));
		return send(httpEntity, Action.class);
	}

	private <T> RestResponseEntity<T> send(RequestEntity<?> httpEntity, Class<T> type) {
		if (log.isDebugEnabled() && httpEntity.getBody() != null) {
			doLog("OUT", httpEntity.getBody(), null);
		}
		try {
			ResponseEntity<T> exchange = restTemplate.exchange(httpEntity.getUrl(), httpEntity.getMethod(), httpEntity,
					type);
			setCookies(exchange);
			if (log.isDebugEnabled() && exchange.getBody() != null
					&& exchange.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)) {
				doLog("IN", exchange.getBody(), exchange.getStatusCode());
			}
			return RestResponseEntity.of(exchange);
		} catch (HttpServerErrorException e) {
			ErrorModel errorModel = null;
			try {
				String bodyAsString = e.getResponseBodyAsString();
				if (StringUtils.isNotBlank(bodyAsString)) {
					errorModel = objectMapper.readerFor(ErrorModel.class).readValue(bodyAsString);
				}
			} catch (IOException ioe) {
				log.error("could not read error from response", e);
			}
			if (null == errorModel) {
				errorModel = new ErrorModel();
				errorModel.setCode(e.getStatusCode().value());
				errorModel.setMessage(e.getMessage());
			}
			return new RestResponseEntity<>(errorModel, e.getResponseHeaders(), e.getStatusCode());
		}
	}

	private void doLog(String prefix, Object body, HttpStatus httpStatus) {
		try {
			log.debug("{}: {} {}", prefix, (null != httpStatus ? " " + httpStatus.value() : ""),
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
		} catch (JsonProcessingException e) {
			log.error("error parsing JSON body", e);
		}
	}

	/**
	 * Retrieves the {@link Action} represented by the {@link Link}
	 * 
	 * @param link
	 *            the {@link Link} representing the {@link Action}'s URI
	 * @return the (unprocessed) {@link Action} wrapped in a {@link RestResponseEntity}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public RestResponseEntity<Action> getAction(Link link) throws URISyntaxException {
		String[] pathSegments = link.getTarget().split("/");
		String servicePath = StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), "/");
		URI uri = new URI(url + "/" + servicePath);
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeaders(), HttpMethod.GET, uri);
		return send(httpEntity, Action.class);
	}

	/**
	 * Performs an {@link Action}, the URI is defined by the {@link Link}.
	 * 
	 * @param data
	 *            the {@link Action}-data to send
	 * @param link
	 *            the {@link Link}
	 * @return a {@link RestResponseEntity} wrapping the resulting {@link Action}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public RestResponseEntity<Action> performAction(Action data, Link link) throws URISyntaxException {
		String[] pathSegments = link.getTarget().split("/");
		URI uri = new URI(url + "/" + StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), "/"));
		addFormAction(data);
		RequestEntity<Action> httpEntity = new RequestEntity<>(data, getHeaders(), HttpMethod.POST, uri);
		return send(httpEntity, Action.class);
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
				log.debug("received cookie: {}={}", name, value);
			}
		}
	}

	protected URI getActionURL(String application, String eventId, String actionId, String[] pathVariables)
			throws URISyntaxException {
		String uriString = String.format("%s/%s/rest/action/%s/%s/%s", url, application, eventId, actionId,
				StringUtils.join(pathVariables, "/"));
		return new URI(uriString);
	}

	/**
	 * Performs an {@link Action}.
	 * 
	 * @param application
	 *            the name of the {@link Application}
	 * @param data
	 *            the {@link Action}-data to send
	 * @param pathVariables
	 *            some additional path variables
	 * @return a {@link RestResponseEntity} wrapping the resulting {@link Action}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public RestResponseEntity<Action> performAction(String application, Action data, String... pathVariables)
			throws URISyntaxException {
		addFormAction(data);
		RequestEntity<Action> httpEntity = new RequestEntity<>(data, getHeaders(), HttpMethod.POST,
				getActionURL(application, data.getEventId(), data.getId(), pathVariables));
		return send(httpEntity, Action.class);
	}

	protected HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		if (!cookies.isEmpty()) {
			cookies.keySet().forEach(k -> {
				String cookie = cookies.get(k);
				headers.add(HttpHeaders.COOKIE, k + "=" + cookie);
				log.debug("sent cookie: {}={}", k, cookies.get(k));
			});
		}
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON_UTF8, MediaType.APPLICATION_JSON));
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
	 *            the ink to there resource
	 * @return the response
	 * @throws URISyntaxException
	 *             if something is wrong with the link
	 */
	public RestResponseEntity<byte[]> getBinaryData(Link link) throws URISyntaxException {
		String[] pathSegments = link.getTarget().split("/");
		URI uri = new URI(url + "/" + StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), "/"));
		RequestEntity<Action> httpEntity = new RequestEntity<>(getHeaders(), HttpMethod.GET, uri);
		return send(httpEntity, byte[].class);
	}

}
