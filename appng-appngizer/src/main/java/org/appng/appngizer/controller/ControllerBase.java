/*
 * Copyright 2011-2020 the original author or authors.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.Messaging;
import org.appng.api.model.Properties;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.appngizer.model.xml.Nameable;
import org.appng.core.controller.messaging.SiteStateEvent;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.CacheProvider;
import org.appng.core.service.CoreService;
import org.appng.core.service.DatabaseService;
import org.appng.core.service.TemplateService;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public abstract class ControllerBase implements DisposableBean {

	@Autowired
	ServletContext context;

	@Autowired
	HttpSession session;

	@Autowired
	CoreService coreService;

	@Autowired
	TemplateService templateService;

	@Autowired
	DatabaseService databaseService;

	@Autowired
	AppNGizerConfigurer configurer;

	@Autowired
	ApplicationContext appCtx;

	static volatile boolean messagingInitialized = false;

	ExecutorService executor;

	protected synchronized void initMessaging() {
		if (!messagingInitialized) {
			DefaultEnvironment env = DefaultEnvironment.get(context);
			ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
			ThreadFactory threadFactory = tfb.setDaemon(true).setNameFormat("appng-messaging").build();
			executor = Executors.newSingleThreadExecutor(threadFactory);

			EventHandler<Event> defaultHandler = new EventHandler<Event>() {
				public void onEvent(Event event, Environment environment, org.appng.api.model.Site site)
						throws InvalidConfigurationException, BusinessException {
					logger().info("received: {}", event);
				}

				public Class<Event> getEventClass() {
					return Event.class;
				}
			};

			EventHandler<SiteStateEvent> siteStateHandler = new EventHandler<SiteStateEvent>() {
				public void onEvent(SiteStateEvent event, Environment environment, org.appng.api.model.Site site)
						throws InvalidConfigurationException, BusinessException {
					SiteState state = event.getState();
					Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
					updateSiteMap(env, new CacheProvider(platformConfig), event.getSiteName(),
							SiteState.STARTED.equals(state));
				}

				public Class<SiteStateEvent> getEventClass() {
					return SiteStateEvent.class;
				}
			};

			String nodeId = Messaging.getNodeId(env) + "_appNGizer";
			Messaging.createMessageSender(env, executor, nodeId, defaultHandler, Arrays.asList(siteStateHandler));
			messagingInitialized = true;
		}
	}

	protected void updateSiteMap(Environment env, CacheProvider cacheProvider, String siteName, boolean doAdd) {
		Map<String, org.appng.api.model.Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		if (doAdd) {
			SiteImpl site = getCoreService().getSiteByName(siteName);
			List<URL> jarUrls = new ArrayList<URL>();
			site.getSiteApplications().stream().filter(SiteApplication::isActive).forEach(a -> {
				File platformCache = cacheProvider.getPlatformCache(site, a.getApplication());
				File jarFolder = new File(platformCache, ResourceType.JAR.getFolder());
				for (String file : jarFolder.list((d, n) -> n.endsWith(".jar"))) {
					try {
						jarUrls.add(new File(jarFolder, file).toPath().toUri().toURL());
					} catch (MalformedURLException e) {
						logger().error("error adding jar", e);
					}
				}
			});

			SiteClassLoader siteClassLoader = new SiteClassLoader(jarUrls.toArray(new URL[0]),
					getClass().getClassLoader(), site.getName());
			site.setSiteClassLoader(siteClassLoader);
			site.setState(SiteState.STARTED);
			siteMap.put(site.getName(), site);
			logger().info("Site {} is {}", site.getName(), site.getState());
		} else {
			siteMap.remove(siteName);
		}
		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);
	}

	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(BusinessException.class)
	public void onBusinessException(HttpServletRequest request, BusinessException e) {
		String message = String.format("%s error while processing [%s] %s", request.getSession().getId(),
				request.getMethod(), request.getServletPath());
		logger().error(message, e);
	}

	abstract Logger logger();

	CoreService getCoreService() {
		return coreService;
	}

	TemplateService getTemplateService() {
		return templateService;
	}

	ApplicationImpl getApplicationByName(String name) {
		return (ApplicationImpl) getCoreService().findApplicationByName(name);
	}

	SiteImpl getSiteByName(String name) {
		return getCoreService().getSiteByName(name);
	}

	SiteApplication getSiteApplication(String site, String application) {
		return getCoreService().getSiteApplicationWithGrantedSites(site, application);
	}

	boolean nameChanged(Nameable nameable, String name) {
		return !nameable.getName().equals(name);
	}

	UriComponentsBuilder getUriBuilder() {
		return ServletUriComponentsBuilder.fromCurrentContextPath();
	}

	<T> ResponseEntity<T> ok(T entity) {
		return new ResponseEntity<T>(entity, HttpStatus.OK);
	}

	<T> ResponseEntity<T> notFound() {
		return reply(HttpStatus.NOT_FOUND);
	}

	<T> ResponseEntity<T> seeOther(URI location) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(location);
		return reply(httpHeaders, HttpStatus.SEE_OTHER);
	}

	<T> ResponseEntity<T> movedPermanently(URI location) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(location);
		return reply(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
	}

	<T> ResponseEntity<T> created(T entity) {
		return reply(entity, HttpStatus.CREATED);
	}

	<T> ResponseEntity<T> conflict() {
		return reply(HttpStatus.CONFLICT);
	}

	<T> ResponseEntity<T> internalServerError() {
		return reply(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	<T> ResponseEntity<T> noContent(HttpHeaders headers) {
		return reply(headers, HttpStatus.NO_CONTENT);
	}

	<T> ResponseEntity<T> reply(HttpStatus status) {
		return new ResponseEntity<T>(status);
	}

	<T> ResponseEntity<T> reply(HttpHeaders headers, HttpStatus status) {
		return new ResponseEntity<T>(headers, status);
	}

	<T> ResponseEntity<T> reply(T entity, HttpStatus status) {
		return new ResponseEntity<T>(entity, status);
	}

	protected MigrationInfo getDatabaseStatus() {
		DatabaseConnection platformConnection = databaseService.getPlatformConnection(configurer.getProps());
		return databaseService.statusComplete(platformConnection).current();
	}

	public String getSharedSecret() {
		Properties platformCfg = getCoreService().getPlatformProperties();
		return platformCfg.getString(Platform.Property.SHARED_SECRET);
	}

	public void destroy() throws Exception {
		if (null != executor) {
			List<Runnable> shutdownNow = executor.shutdownNow();
			for (Runnable runnable : shutdownNow) {
				logger().info("Shut down {}", runnable.toString());
			}
		}
	}
}
