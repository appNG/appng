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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.http.HttpServletRequest;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.Messaging;
import org.appng.api.model.Properties;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.RepositoryCacheFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class Home extends ControllerBase implements InitializingBean, DisposableBean {

	static final String AUTHORIZED = "authorized";
	static final String ROOT = "/";
	ExecutorService executor;

	@RequestMapping(value = ROOT, method = RequestMethod.POST)
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

	@RequestMapping(value = ROOT, method = RequestMethod.GET)
	public ResponseEntity<org.appng.appngizer.model.xml.Home> welcome() {
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

	public void afterPropertiesSet() throws Exception {
		java.util.Properties props = configurer.getProps();
		if (null == getDatabaseStatus()) {
			logger().info("database is not initialized, must initialize first");
			databaseService.initDatabase(props);
		}
		Environment env = DefaultEnvironment.get(context);
		Properties platformConfig = initPlatform(props, env);
		initMessaging(env);
		RepositoryCacheFactory.init(platformConfig);
	}

	protected void initMessaging(Environment env) {
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		ThreadFactory threadFactory = tfb.setDaemon(true).setNameFormat("appng-messaging").build();
		executor = Executors.newSingleThreadExecutor(threadFactory);

		// TODO decide which events can be handled by appNGizer and create an EventHandler for them

		EventHandler<Event> defaultHandler = new EventHandler<Event>() {

			public void onEvent(Event event, Environment environment, org.appng.api.model.Site site)
					throws InvalidConfigurationException, BusinessException {
				logger().info("received: {}", event);
			}

			public Class<Event> getEventClass() {
				return Event.class;
			}
		};

		String nodeId = Messaging.getNodeId(env) + "_appNGizer";
		Messaging.createMessageSender(env, executor, nodeId, defaultHandler, null);
	}

	protected Properties initPlatform(java.util.Properties defaultOverrides, Environment env) {
		String rootPath = (String) context.getAttribute(AppNGizer.APPNG_HOME);
		Properties platformConfig = coreService.initPlatformConfig(defaultOverrides, rootPath, false, true, true);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG, platformConfig);

		Map<String, org.appng.api.model.Site> siteMap = new HashMap<>();
		for (SiteImpl site : getCoreService().getSites()) {
			if (site.isActive()) {
				SiteImpl s = getCoreService().getSite(site.getId());
				SiteClassLoader siteClassLoader = new SiteClassLoader(site.getName());
				s.setSiteClassLoader(siteClassLoader);
				siteMap.put(site.getName(), s);
			}
		}
		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);
		return platformConfig;
	}

	public void destroy() throws Exception {
		List<Runnable> shutdownNow = executor.shutdownNow();
		for (Runnable runnable : shutdownNow) {
			logger().info(runnable.toString());
		}
	}

}
