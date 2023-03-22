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
import java.util.concurrent.ConcurrentMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Path;
import org.appng.api.Scope;
import org.appng.api.support.environment.DefaultEnvironment;
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

	private static final double[] BUCKET_THRESHOLS = new double[] { 0.1, 0.25, 0.5, 1, 2, 3, 5, 8, 30 };
	private static final String SEPARATOR = "::";
	private static String PREFIX = "org.appng.metrics";
	public static String EVENT_ID = PREFIX + "event_id";
	public static String DATASOURCE_ID = PREFIX + "datasource_id";
	public static String ACTION_ID = PREFIX + "action_id";
	public static String SERVICE_TYPE = PREFIX + "serviceType";
	public static String SERVICE_NAME = PREFIX + "serviceName";
	private static final ConcurrentMap<String, Histogram> METRICS = new ConcurrentHashMap<>();
	private static final String METRICS_REGISTRY = "metricsRegistry";

	@Override
	public void setServletContext(ServletContext servletContext) {
		super.setServletContext(servletContext);
		DefaultEnvironment.getGlobal().setAttribute(Scope.PLATFORM, METRICS_REGISTRY,
				new ConcurrentHashMap<String, CollectorRegistry>());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long start = System.currentTimeMillis();
		chain.doFilter(request, response);
		observe(request, (System.currentTimeMillis() - start) / 1000.0);
	}

	private void observe(HttpServletRequest servletRequest, double duration) {
		Environment env = EnvironmentFilter.environment();
		Path path = env.getAttribute(Scope.REQUEST, EnvironmentKeys.PATH_INFO);
		if (null != path) {
			String site = path.getSiteName();
			String application = path.getApplicationName();
			String actionId = (String) servletRequest.getAttribute(ACTION_ID);
			String eventId = (String) servletRequest.getAttribute(EVENT_ID);
			String datasourceId = (String) servletRequest.getAttribute(DATASOURCE_ID);
			String serviceType = (String) servletRequest.getAttribute(SERVICE_TYPE);
			String serviceName = (String) servletRequest.getAttribute(SERVICE_NAME);

			StringBuilder key = new StringBuilder().append(site).append(SEPARATOR);
			if (StringUtils.isNotBlank(application)) {
				key.append(application).append(SEPARATOR);
			}
			key.append(serviceType);

			if (StringUtils.isNotBlank(actionId)) {
				key.append(SEPARATOR).append("act").append(SEPARATOR).append(eventId).append(SEPARATOR)
						.append(actionId);
			} else if (StringUtils.isNotBlank(datasourceId)) {
				key.append(SEPARATOR).append("dat").append(SEPARATOR).append(datasourceId);
			} else if (StringUtils.isNotBlank(serviceName)) {
				key.append(SEPARATOR).append(serviceName);
			}

			String metricsKey = Collector.sanitizeMetricName(key.toString());
			if (!METRICS.containsKey(metricsKey)) {
				CollectorRegistry registry = getRegistry(env, site);
				METRICS.put(metricsKey, Histogram.build().name(metricsKey).buckets(BUCKET_THRESHOLS)
						.help(metricsKey.replaceAll(SEPARATOR, StringUtils.SPACE)).register(registry));
			}
			METRICS.get(metricsKey).observeWithExemplar(duration);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Observed {} with {}s", metricsKey, duration);
			}
		}
	}

	public static CollectorRegistry getRegistry(Environment env, String site) {
		Map<String, CollectorRegistry> registries = DefaultEnvironment.getGlobal().getAttribute(Scope.PLATFORM,
				METRICS_REGISTRY);
		if (!registries.containsKey(site)) {
			registries.put(site, new CollectorRegistry(true));
		}
		return registries.get(site);
	}

}
