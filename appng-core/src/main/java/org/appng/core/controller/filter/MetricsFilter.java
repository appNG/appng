/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.core.controller.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.messaging.Messaging;
import org.appng.api.support.environment.DefaultEnvironment;
import org.springframework.web.filter.OncePerRequestFilter;

import io.prometheus.client.Histogram;
import io.prometheus.client.Histogram.Timer;
import lombok.extern.slf4j.Slf4j;

/**
 * A filter providing some prometheus metrics.
 */
@Slf4j
public class MetricsFilter extends OncePerRequestFilter {

	private Histogram metrics;
	private String nodeId;

	private static String PREFIX = "org.appng.metrics";
	public static String SITE = PREFIX + "site";
	public static String APPLICATION = PREFIX + "application";
	public static String EVENT_ID = PREFIX + "metrics_event_id";
	public static String DATASOURCE_ID = PREFIX + "metrics_datasource_id";
	public static String ACTION_ID = PREFIX + "metrics_action_id";
	public static String SERVICE_TYPE = PREFIX + "serviceType";

	@Override
	protected void initFilterBean() throws ServletException {
		this.metrics = Histogram.build().name("appng_metrics").help("appNG Metrics").register();
		this.nodeId = Messaging.getNodeId(DefaultEnvironment.getGlobal());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Timer timer = metrics.startTimer();
		chain.doFilter(request, response);
		commitTimer(request, response, timer);
	}

	private void commitTimer(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Timer timer) {
		if (null != timer) {
			Map<String, String> labels = new HashMap<>();
			addIfNotNull(labels, "site", (String) servletRequest.getAttribute(SITE));
			addIfNotNull(labels, "application", (String) servletRequest.getAttribute(APPLICATION));
			addIfNotNull(labels, "method", servletRequest.getMethod());
			addIfNotNull(labels, "content-type", servletResponse.getContentType());
			addIfNotNull(labels, "sessionId", servletRequest.getRequestedSessionId());
			addIfNotNull(labels, "nodeId", nodeId);
			addIfNotNull(labels, "servletPath", servletRequest.getServletPath());
			addIfNotNull(labels, "actionId", (String) servletRequest.getAttribute(ACTION_ID));
			addIfNotNull(labels, "eventId", (String) servletRequest.getAttribute(EVENT_ID));
			addIfNotNull(labels, "datasourceId", (String) servletRequest.getAttribute(DATASOURCE_ID));
			addIfNotNull(labels, "serviceType", (String) servletRequest.getAttribute(SERVICE_TYPE));
			double duration = timer.observeDurationWithExemplar(labels);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("{} ms with labels: {}", duration, labels);
			}
		}
	}

	private void addIfNotNull(Map<String, String> labels, String name, String value) {
		if (null != value) {
			labels.put(name, value);
		}
	}

}
