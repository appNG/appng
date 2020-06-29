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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
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
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.appngizer.model.xml.Nameable;
import org.appng.core.controller.handler.MonitoringHandler.SiteInfo;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
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
			ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
			ThreadFactory threadFactory = tfb.setDaemon(true).setNameFormat("appNGizer-messaging").build();
			executor = Executors.newSingleThreadExecutor(threadFactory);

			Properties platformCfg = coreService.getPlatformProperties();
			String monitoringPath = platformCfg.getString(Platform.Property.MONITORING_PATH);
			Boolean trustAllCertificates = platformCfg.getBoolean(AppNGizer.TRUST_ALL_CERTIFICATES, true);

			Callable<Void> waitForAppNG = () -> {
				List<SiteImpl> sites = coreService.getSites();
				Optional<SiteImpl> activeSite = sites.stream().filter(SiteImpl::isActive).findFirst();

				if (activeSite.isPresent()) {
					RestTemplate restTemplate = getRestTemplate(trustAllCertificates);
					DefaultEnvironment env = DefaultEnvironment.get(context);

					HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
					while (!HttpStatus.OK.equals(statusCode)) {
						ResponseEntity<SiteInfo> healthResponse = checkSiteHealth(platformCfg, env, monitoringPath,
								activeSite.get(), restTemplate);
						if (null != healthResponse) {
							statusCode = healthResponse.getStatusCode();
						}
						TimeUnit.SECONDS.sleep(5);
					}

					doInit(env);
				}
				return null;
			};

			executor.submit(waitForAppNG);
		}
	}

	private RestTemplate getRestTemplate(Boolean trustAllCertificates) throws GeneralSecurityException {
		if (trustAllCertificates) {
			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
			CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
			ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
			return new RestTemplate(requestFactory);
		}
		return new RestTemplate();
	}

	private ResponseEntity<SiteInfo> checkSiteHealth(Properties platformCfg, Environment env, String monitoringPath,
			Site site, RestTemplate restTemplate) {
		String healthMonitor = site.getDomain() + monitoringPath;
		try {
			HttpHeaders headers = new HttpHeaders();
			String password = platformCfg.getString("monitoringPassword",
					platformCfg.getString(Platform.Property.SHARED_SECRET));
			String encodedAuth = Base64.getEncoder()
					.encodeToString((String.format("monitoring:%s", password)).getBytes());
			headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
			RequestEntity<Void> healthRequest = new RequestEntity<Void>(null, headers, HttpMethod.GET,
					new URI(healthMonitor));
			ResponseEntity<SiteInfo> healthResponse = restTemplate.exchange(healthRequest, SiteInfo.class);
			SiteInfo siteInfo = healthResponse.getBody();
			logger().info("Health status from {} for site {} returned {}", healthRequest.getUrl(), siteInfo.getName(),
					siteInfo.getState());
			return healthResponse;
		} catch (Exception e) {
			logger().warn("Failed retrieving health status from {}: {} ({})", healthMonitor,
					e.getClass().getSimpleName(), e.getMessage());
		}
		return null;
	}

	private void doInit(DefaultEnvironment env) {
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
				updateSiteMap(env, new CacheProvider(platformConfig), event.getSiteName(), state);
			}

			public Class<SiteStateEvent> getEventClass() {
				return SiteStateEvent.class;
			}
		};

		String nodeId = Messaging.getNodeId(env) + "_appNGizer";
		Messaging.createMessageSender(env, executor, nodeId, defaultHandler, Arrays.asList(siteStateHandler));
		messagingInitialized = true;
	}

	protected void updateSiteMap(Environment env, CacheProvider cacheProvider, String siteName, SiteState state) {
		Map<String, org.appng.api.model.Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		SiteImpl site = (SiteImpl) siteMap.get(siteName);
		if (SiteState.DELETED.equals(state)) {
			siteMap.remove(siteName);
			closeClassLoader(site);
			site = null;
		} else if (null == site) {
			site = getCoreService().getSiteByName(siteName);
		}

		if (null != site) {
			if (null == site.getSiteClassLoader() || SiteState.STARTING.equals(state)) {
				closeClassLoader(site);
				site.setSiteClassLoader(buildSiteClassLoader(cacheProvider, site));
			}

			site.setState(state);
			siteMap.put(site.getName(), site);
			logger().info("Site {} is {}", site.getName(), site.getState());
		}

		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);
	}

	private void closeClassLoader(SiteImpl site) {
		if (null != site) {
			try {
				SiteClassLoader siteClassLoader = site.getSiteClassLoader();
				if (null != siteClassLoader) {
					siteClassLoader.close();
				}
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private SiteClassLoader buildSiteClassLoader(CacheProvider cacheProvider, SiteImpl site) {
		List<URL> jarUrls = new ArrayList<URL>();
		site.getSiteApplications().stream().filter(SiteApplication::isActive).forEach(a -> {
			File platformCache = cacheProvider.getPlatformCache(site, a.getApplication());
			File jarFolder = new File(platformCache, ResourceType.JAR.getFolder());
			if (jarFolder.exists()) {
				for (String file : jarFolder.list((d, n) -> n.endsWith(".jar"))) {
					try {
						jarUrls.add(new File(jarFolder, file).toPath().toUri().toURL());
					} catch (MalformedURLException e) {
						logger().error("error adding jar", e);
					}
				}
			}
		});
		return new SiteClassLoader(jarUrls.toArray(new URL[0]), getClass().getClassLoader(), site.getName());
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
