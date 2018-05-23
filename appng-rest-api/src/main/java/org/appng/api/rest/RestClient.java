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
import org.appng.api.rest.model.Action;
import org.appng.api.rest.model.Datasource;
import org.appng.api.rest.model.Link;
import org.appng.api.rest.model.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * A simple client for the appNG REST API, open for extension. 
 */
public class RestClient {

	protected RestTemplate restTemplate;
	protected String cookie;
	protected String url;

	public RestClient(String url) {
		this.restTemplate = new RestTemplate(Arrays.asList(new MappingJackson2HttpMessageConverter()));
		this.url = url;
	}

	public ResponseEntity<Datasource> datasource(String application, String id) throws URISyntaxException {
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeader(), HttpMethod.GET,
				new URI(url + "/" + application + "/rest/datasource/" + id));
		ResponseEntity<Datasource> datasource = restTemplate.exchange(httpEntity.getUrl(), httpEntity.getMethod(),
				httpEntity, Datasource.class);
		setCookies(datasource);

		return datasource;
	}

	public ResponseEntity<Action> getAction(String application, String eventId, String actionId,
			String... pathVariables) throws URISyntaxException {
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeader(), HttpMethod.GET,
				getActionURL(application, eventId, actionId, pathVariables));
		ResponseEntity<Action> action = restTemplate.exchange(httpEntity.getUrl(), httpEntity.getMethod(), httpEntity,
				Action.class);
		setCookies(action);
		return action;
	}

	public ResponseEntity<Action> getAction(Link link) throws URISyntaxException {
		String[] pathSegments = link.getTarget().split("/");
		URI uri = new URI(url + "/" + StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), "/"));
		RequestEntity<?> httpEntity = new RequestEntity<>(getHeader(), HttpMethod.GET, uri);
		ResponseEntity<Action> action = restTemplate.exchange(httpEntity.getUrl(), httpEntity.getMethod(), httpEntity,
				Action.class);
		setCookies(action);
		return action;
	}
	public ResponseEntity<Action> performAction(String uc01Payment, Action data, Link link) throws URISyntaxException {
		String[] pathSegments = link.getTarget().split("/");
		URI uri = new URI(url + "/" + StringUtils.join(Arrays.copyOfRange(pathSegments, 3, pathSegments.length), "/"));
		addFormAction(data);
		RequestEntity<Action> httpEntity = new RequestEntity<>(data, getHeader(), HttpMethod.POST,
				uri);
		ResponseEntity<Action> action = restTemplate.exchange(httpEntity.getUrl(), httpEntity.getMethod(), httpEntity,
				Action.class);
		setCookies(action);
		return action;
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
		return new URI(String.format("%s/%s/rest/action/%s/%s" + StringUtils.repeat("%s", pathVariables.length), url,
				application, eventId, actionId, pathVariables));
	}

	public ResponseEntity<Action> performAction(String application, Action data, String... pathVariables)
			throws URISyntaxException {
		addFormAction(data);
		RequestEntity<Action> httpEntity = new RequestEntity<>(data, getHeader(), HttpMethod.POST,
				getActionURL(application, data.getEventId(), data.getId(), pathVariables));
		ResponseEntity<Action> action = restTemplate.exchange(httpEntity.getUrl(), httpEntity.getMethod(), httpEntity,
				Action.class);
		setCookies(action);
		return action;
	}

	protected HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		if (StringUtils.isNotBlank(cookie)) {
			headers.set(HttpHeaders.COOKIE, cookie);
		}
		return headers;
	}

}
