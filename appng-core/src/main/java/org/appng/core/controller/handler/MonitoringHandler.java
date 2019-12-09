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
package org.appng.core.controller.handler;

import static org.appng.api.support.environment.EnvironmentKeys.JAR_INFO_MAP;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.appng.api.Environment;
import org.appng.api.PathInfo;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.EnvironmentKeys;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A {@link RequestHandler} that exposes some health information for the site.
 * <p>
 * Supports the following operations:
 * <ul>
 * <li>{@code /health}<br/>
 * Shows the {@link Site}'s status, including its {@link Application}s and {@link Properties}.</li>
 * <li>{@code /health/system}<br/>
 * Shows the system's properties as returned by {@link System#getProperties()}.</li>
 * <li>{@code /health}/environment<br/>
 * Shows the system's environment as returned by {@link System#getenv()}.</li>
 * <li>{@code /health/platform}<br/>
 * Shows a list of all JAR files used by the platform.</li>
 * <li>{@code /health/jars}<br/>
 * Shows a list of all JAR files used by {@link Site}s {@link SiteClassLoader}.</li></li>
 * </ul>
 * </p>
 * 
 * @see    Platform.Property#MONITORING_PATH
 * @author Matthias Müller
 */
public class MonitoringHandler implements RequestHandler {

	private static final String MONITORING_PASSWORD = "monitoringPassword";
	private static final String BASIC_REALM = "Basic realm=\"appNG Health Monitoring\"";
	private static final String MONITORING_USER = "monitoring";
	private ObjectWriter writer;

	public MonitoringHandler() {
		writer = new ObjectMapper().setSerializationInclusion(Include.NON_ABSENT)
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).registerModule(new JavaTimeModule())
				.writerWithDefaultPrettyPrinter();
	}

	public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Environment env,
			Site site, PathInfo pathInfo) throws ServletException, IOException {
		servletResponse.addHeader(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM);
		if (isAuthenticated(env, servletRequest)) {
			String pathsegment = pathInfo.getElementAt(2);
			Object result = null;
			if (null == pathsegment) {
				result = getSiteInfo(site);
			} else if ("system".equals(pathsegment)) {
				result = new TreeMap<>(System.getProperties());
			} else if ("environment".equals(pathsegment)) {
				result = new TreeMap<>(System.getenv());
			} else if ("jars".equals(pathsegment)) {
				result = env.getAttribute(Scope.PLATFORM, site.getName() + "." + EnvironmentKeys.JAR_INFO_MAP);
			} else if ("platform".equals(pathsegment)) {
				result = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG + "." + JAR_INFO_MAP);
			}
			servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
			writer.writeValue(servletResponse.getOutputStream(), result);
		} else {
			servletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
		}
	}

	private Object getSiteInfo(Site site) {
		Map<String, ApplicationInfo> applicationInfos = new HashMap<>();
		if (site.getState().equals(SiteState.STARTED)) {
			for (Application a : site.getApplications()) {
				List<Jar> jars = null;
				if (null != a.getResources()) {
					jars = a.getResources().getResources(ResourceType.JAR).stream()
							.map(j -> new Jar(j.getCachedFile().getAbsolutePath(),
									OffsetDateTime.ofInstant(Instant.ofEpochMilli(j.getCachedFile().lastModified()),
											TimeZone.getDefault().toZoneId())))
							.collect(Collectors.toList());
				}
				DataSource ds = a.getBean(DataSource.class);
				Connection connection = null;
				if (null != ds && ds instanceof HikariDataSource) {
					HikariDataSource hkds = HikariDataSource.class.cast(ds);
					connection = new Connection(hkds.getDataSourceProperties().getProperty("url"),
							hkds.getDataSourceProperties().getProperty("user"), hkds.getMaximumPoolSize());
				}
				ApplicationInfo appInfo = new ApplicationInfo(a.getPackageVersion(), a.getDescription(), a.isHidden(),
						a.isPrivileged(), a.isFileBased(), connection, jars);
				applicationInfos.put(a.getName(), appInfo);
			}
		}
		Long uptime = null;
		OffsetDateTime startup = null;
		if (null != site.getStartupTime()) {
			startup = OffsetDateTime.ofInstant(site.getStartupTime().toInstant(), ZoneId.systemDefault());
			uptime = Duration.between(startup.toLocalDateTime(), LocalDateTime.now()).getSeconds();
		}
		java.util.Properties plainProperties = site.getProperties().getPlainProperties();
		Map<Object, Object> typedProperties = new TreeMap<>();
		for (Entry<Object, Object> entry : plainProperties.entrySet()) {
			String value = (String) entry.getValue();
			String key = (String) entry.getKey();
			if (value.matches("^\\d+$")) {
				typedProperties.put(key, Integer.valueOf(value));
			} else if (value.toLowerCase().matches("^\\d+\\.\\d+$")) {
				typedProperties.put(key, Double.valueOf(value));
			} else if (value.toLowerCase().matches("^true|false$")) {
				typedProperties.put(key, Boolean.valueOf(value).booleanValue());
			} else {
				typedProperties.put(key, value);
			}
		}

		return new SiteInfo(site.getName(), site.getState(), site.getHost(), site.getDomain(), startup, uptime,
				applicationInfos, typedProperties);
	}

	private boolean isAuthenticated(Environment env, HttpServletRequest servletRequest) {
		Properties platformCfg = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String sharedSecret = platformCfg.getString(Platform.Property.SHARED_SECRET);
		String password = platformCfg.getString(MONITORING_PASSWORD, sharedSecret);
		String actualAuth = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
		String expectedAuth = Base64.getEncoder().encodeToString((MONITORING_USER + ":" + password).getBytes());
		return null != actualAuth && actualAuth.equals("Basic " + expectedAuth);
	}

	@Data
	@AllArgsConstructor
	class SiteInfo {
		String name;
		SiteState state;
		String host;
		String domain;
		OffsetDateTime startupTime;
		Long uptimeSeconds;
		Map<String, ApplicationInfo> applications;
		Map<Object, Object> props;
	}

	@Data
	@AllArgsConstructor
	class ApplicationInfo {
		String version;
		String description;
		boolean hidden;
		boolean privileged;
		boolean filebased;
		Connection connection;
		List<Jar> jars;
	}

	@Data
	@AllArgsConstructor
	class Connection {
		String url;
		String user;
		Integer maxPoolSize;
	}

	@Data
	@AllArgsConstructor
	class Jar {
		String name;
		OffsetDateTime lastModified;
	}
}
