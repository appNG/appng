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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.rest.model.Action;
import org.appng.api.rest.model.Datasource;
import org.appng.api.rest.model.Link;
import org.appng.api.rest.model.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * A simple client for the appNG REST API, open for extension.
 */
@Slf4j
public class RestClient {

	protected RestTemplate restTemplate;
	protected String cookie;
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
		this.restTemplate = new RestTemplate(Arrays.asList(new MappingJackson2HttpMessageConverter()));
		this.objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_ABSENT);
		this.url = url;
	}

	/**
	 * Creates a new {@link RestClient}, using an existing cookie. This cookie should be retrieved from another client
	 * that performed a login action.
	 * 
	 * @param url
	 *            the URL pointing to a {@link Site}'s service URL ({@code /service/<site-name>})
	 * @param cookie
	 *            the cookie to use
	 * 
	 * @see RestClient#getCookie()
	 */
	public RestClient(String url, String cookie) {
		this(url);
		this.cookie = cookie;
	}

	/**
	 * Retrieves the {@link Datasource}.
	 * 
	 * @param application
	 *            the name of the {@link Application}
	 * @param id
	 *            the ID of the {@link Datasource}
	 * @return the {@link Datasource}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public ResponseEntity<Datasource> datasource(String application, String id) throws URISyntaxException {
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeaders(), HttpMethod.GET,
				new URI(url + "/" + application + "/rest/datasource/" + id));
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
	 * @return the (unprocessed) {@link Action}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public ResponseEntity<Action> getAction(String application, String eventId, String actionId,
			String... pathVariables) throws URISyntaxException {
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeaders(), HttpMethod.GET,
				getActionURL(application, eventId, actionId, pathVariables));
		ResponseEntity<Action> action = send(httpEntity, Action.class);
		return action;
	}

	private <T> ResponseEntity<T> send(RequestEntity<?> httpEntity, Class<T> type) {
		if (log.isDebugEnabled() && httpEntity.getBody() != null) {
			doLog("OUT", httpEntity.getBody(), null);
		}
		ResponseEntity<T> exchange = restTemplate.exchange(httpEntity.getUrl(), httpEntity.getMethod(), httpEntity,
				type);
		if (log.isDebugEnabled() && exchange.getBody() != null) {
			doLog("IN", exchange.getBody(), exchange.getStatusCode());
		}
		setCookies(exchange);
		return exchange;
	}

	private void doLog(String prefix, Object body, HttpStatus httpStatus) {
		try {
			log.debug("{}: {} {}", prefix, (null != httpStatus ? " " + httpStatus.value() : ""),
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
		} catch (JsonProcessingException e) {
			//
		}
	}

	/**
	 * Retrieves the {@link Action} represented by the {@link Link}
	 * 
	 * @param link
	 *            the {@link Link} representing the {@link Action}'s URI
	 * @return the (unprocessed) {@link Action}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public ResponseEntity<Action> getAction(Link link) throws URISyntaxException {
		String[] pathSegments = link.getTarget().split("/");
		URI uri = new URI(url + "/" + StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), "/"));
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
	 * @return a {@link ResponseEntity} wrapping the resulting {@link Action}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public ResponseEntity<Action> performAction(Action data, Link link) throws URISyntaxException {
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
		List<String> cookies = entity.getHeaders().get(HttpHeaders.SET_COOKIE);
		if (null != cookies) {
			cookie = cookies.stream().collect(Collectors.joining(";"));
		}
	}

	protected URI getActionURL(String application, String eventId, String actionId, String[] pathVariables)
			throws URISyntaxException {
		return new URI(String.format("%s/%s/rest/action/%s/%s" + StringUtils.repeat("/%s", pathVariables.length), url,
				application, eventId, actionId, pathVariables));
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
	 * @return a {@link ResponseEntity} wrapping the resulting {@link Action}
	 * @throws URISyntaxException
	 *             if something is wrong with the URI
	 */
	public ResponseEntity<Action> performAction(String application, Action data, String... pathVariables)
			throws URISyntaxException {
		addFormAction(data);
		RequestEntity<Action> httpEntity = new RequestEntity<>(data, getHeaders(), HttpMethod.POST,
				getActionURL(application, data.getEventId(), data.getId(), pathVariables));
		return send(httpEntity, Action.class);
	}

	protected HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		if (StringUtils.isNotBlank(cookie)) {
			headers.set(HttpHeaders.COOKIE, cookie);
		}
		return headers;
	}

	/**
	 * Returns the current cookie or this client
	 * 
	 * @return he cookie
	 */
	public String getCookie() {
		return cookie;
	}

}
