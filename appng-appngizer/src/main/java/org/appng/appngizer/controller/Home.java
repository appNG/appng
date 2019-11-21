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
package org.appng.appngizer.controller;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.messaging.HazelcastReceiver;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.CacheProvider;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.service.PropertySupport;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class Home extends ControllerBase implements InitializingBean {

	private static final String HAZELCAST_CONFIG = "../appNGizer/WEB-INF/conf/hazelcast-client.xml";
	static final String AUTHORIZED = "authorized";
	static final String ROOT = "/";

	@PostMapping(value = ROOT)
	public ResponseEntity<org.appng.appngizer.model.xml.Home> login(@RequestBody String sharedSecret,
			HttpServletRequest request) {
		String platformSecret = getSharedSecret();
		if (!platformSecret.equals(sharedSecret)) {
			LOGGER.info("invalid shared secret for session {}", session.getId());
			return reply(HttpStatus.FORBIDDEN);
		}
		session.setAttribute(AUTHORIZED, true);
		LOGGER.info("session {} has been authorized (user-agent: {})", session.getId(),
				request.getHeader(HttpHeaders.USER_AGENT));
		return welcome();
	}

	@GetMapping(value = ROOT)
	public ResponseEntity<org.appng.appngizer.model.xml.Home> welcome() {
		initMessaging();
		String appngVersion = (String) context.getAttribute(AppNGizer.APPNG_VERSION);
		boolean dbInitialized = getDatabaseStatus() != null;
		org.appng.appngizer.model.Home entity = new org.appng.appngizer.model.Home(appngVersion, dbInitialized,
				getUriBuilder());
		entity.applyUriComponents(getUriBuilder());
		return ok(entity);
	}

	Logger logger() {
		return LOGGER;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		java.util.Properties props = configurer.getProps();
		PropertyImpl receiverClass = coreService
				.getProperty(PropertySupport.PREFIX_PLATFORM + Platform.Property.MESSAGING_RECEIVER);
		if (null != receiverClass && HazelcastReceiver.class.getName().equals(receiverClass.getString())) {
			String cacheConfig = PropertySupport.PREFIX_PLATFORM + Platform.Property.CACHE_CONFIG;
			props.put(cacheConfig, HAZELCAST_CONFIG);
			logger().info("Detected {}, using configuration {} for {}", receiverClass.getString(), HAZELCAST_CONFIG,
					cacheConfig);
		}
		MigrationInfo databaseStatus = getDatabaseStatus();
		if (null == databaseStatus) {
			logger().info("database is not initialized, must initialize first");
			databaseService.initDatabase(props);
		} else {
			logger().info("Database is at version {} ({}).", databaseStatus.getVersion().getVersion(),
					databaseStatus.getDescription());
		}
		Environment env = DefaultEnvironment.get(context);
		Properties platformConfig = initPlatform(props, env);
		RepositoryCacheFactory.init(platformConfig);
	}

	protected Properties initPlatform(java.util.Properties defaultOverrides, Environment env) {
		String rootPath = (String) context.getAttribute(AppNGizer.APPNG_HOME);
		Properties platformConfig = coreService.initPlatformConfig(defaultOverrides, rootPath, false, false, true);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG, platformConfig);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, new HashMap<>());
		CacheProvider cacheProvider = new CacheProvider(platformConfig);
		for (SiteImpl s : getCoreService().getSites()) {
			updateSiteMap(env, cacheProvider, s.getName(), s.isActive());
		}
		return platformConfig;
	}

}
