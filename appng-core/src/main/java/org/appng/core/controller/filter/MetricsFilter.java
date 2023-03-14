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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.Scope;
import org.appng.api.support.environment.EnvironmentKeys;
import org.springframework.web.filter.OncePerRequestFilter;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;

/**
 * A filter providing some prometheus metrics.
 */
@Slf4j
public class MetricsFilter extends OncePerRequestFilter {

	private static final String SEPARATOR = "::";
	private static String PREFIX = "org.appng.metrics";
	public static String EVENT_ID = PREFIX + "metrics_event_id";
	public static String DATASOURCE_ID = PREFIX + "metrics_datasource_id";
	public static String ACTION_ID = PREFIX + "metrics_action_id";
	public static String SERVICE_TYPE = PREFIX + "serviceType";
	public static String SERVICE_NAME = PREFIX + "serviceName";
	private static final Map<String, Histogram> METRICS = new ConcurrentHashMap<>();
	public static final String REGISTRY = "CollectorRegistry";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long start = System.currentTimeMillis();
		chain.doFilter(request, response);
		getHistogramm(request).observeWithExemplar(System.currentTimeMillis() - start);
	}

	private Histogram getHistogramm(HttpServletRequest servletRequest) {
		Environment env = EnvironmentFilter.environment();
		Path path = env.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO);

		String site = path.getSiteName();
		String application = path.getApplicationName();
		String actionId = (String) servletRequest.getAttribute(ACTION_ID);
		String eventId = (String) servletRequest.getAttribute(EVENT_ID);
		String datasourceId = (String) servletRequest.getAttribute(DATASOURCE_ID);

		StringBuilder key = buildMetricsKey(site, application);
		if (StringUtils.isNotBlank(actionId)) {
			key.append(eventId).append(SEPARATOR).append(actionId);
		} else if (StringUtils.isNotBlank(datasourceId)) {
			key.append(datasourceId);
		} else {
			String serviceType = (String) servletRequest.getAttribute(SERVICE_TYPE);
			String serviceName = (String) servletRequest.getAttribute(SERVICE_NAME);
			key.append(serviceType);
			if (StringUtils.isNotBlank(serviceName)) {
				key.append(SEPARATOR).append(serviceName);
			}
		}

		String metricsKey = Collector.sanitizeMetricName(key.toString());
		if (!METRICS.containsKey(metricsKey)) {
			CollectorRegistry registry = env.getAttribute(Scope.SITE, REGISTRY);
			if (null == registry) {
				registry = getRegistry(env);
			}
			METRICS.put(metricsKey, Histogram.build().name(metricsKey)
					.help(metricsKey.replaceAll(SEPARATOR, StringUtils.SPACE)).register(registry));
			LOGGER.debug("Created new histogramm: {}", metricsKey);
		}
		return METRICS.get(metricsKey);
	}

	public static StringBuilder buildMetricsKey(String site, String application) {
		return new StringBuilder().append(site).append(SEPARATOR).append(application).append(SEPARATOR);
	}

	public static synchronized CollectorRegistry getRegistry(Environment env) {
		CollectorRegistry registry = new CollectorRegistry(true);
		env.setAttribute(Scope.SITE, REGISTRY, registry);
		return registry;
	}

}