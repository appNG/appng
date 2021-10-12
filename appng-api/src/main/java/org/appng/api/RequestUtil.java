/*
 * Copyright 2011-2021 the original author or authors.
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
package org.appng.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility-class for retrieving {@link Site}s by name,host or {@link ServletRequest} and also creating a {@link Path}
 * -object based on a {@link ServletRequest}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class RequestUtil {

	private static final String SERVER_LOCAL_NAME = "SERVER_LOCAL_NAME";
	private static final String X_APPNG_SITE = "X-appNG-site";

	/**
	 * Retrieves a {@link Site} by its name.
	 * 
	 * @param env
	 *                       the current {@link Environment}
	 * @param servletRequest
	 *                       the current {@link ServletRequest}
	 * 
	 * @return the {@link Site}, if any
	 * 
	 * @see #getSiteName(Environment, ServletRequest)
	 */
	public static Site getSite(Environment env, ServletRequest servletRequest) {
		if (null == servletRequest || null == env) {
			return null;
		}
		String siteName = getSiteName(env, servletRequest);
		return getSiteByName(env, siteName);
	}
	
	/**
	 * Retrieves a {@link Site} by its host.
	 * 
	 * @param env
	 *             the current {@link Environment}
	 * @param host
	 *             the host of the {@link Site}
	 * 
	 * @return the {@link Site}, if any
	 * 
	 * @see Site#getHost()
	 *
	 * @deprecated use {@link #getSiteByName(Environment, String)
	 */
	@Deprecated
	public static Site getSiteByHost(Environment env, String host) {
		return getSiteMap(env).values().stream().filter(s -> host.equals(s.getHost())).findFirst().orElse(null);
	}

	/**
	 * Retrieves a {@link Site} by its name.
	 * 
	 * @param env
	 *             the current {@link Environment}
	 * @param name
	 *             the name of the {@link Site}
	 * 
	 * @return the {@link Site}, if any
	 * 
	 * @see Site#getName()
	 */
	public static Site getSiteByName(Environment env, String name) {
		return getSiteMap(env).get(name);
	}

	/**
	 * Retrieves a {@link Site} by its name, waiting up to {@code Platform.Property#MAX_WAIT_TIME} milliseconds until
	 * it's state is {@code SiteState#STARTED}.
	 * 
	 * @param env
	 *             the current {@link Environment}
	 * @param name
	 *             the name of the {@link Site}
	 * 
	 * @return the {@link Site}, if any
	 * 
	 * @see #getSiteByName(Environment, String)
	 * @see Site#hasState(SiteState...)
	 */
	public static Site waitForSite(Environment env, String name) {
		Site site = getSiteByName(env, name);
		if (null == site || site.hasState(SiteState.STARTED)) {
			return site;
		}

		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		int waitTime = platformProperties.getInteger(Platform.Property.WAIT_TIME, 1000);
		int maxWaitTime = platformProperties.getInteger(Platform.Property.MAX_WAIT_TIME, 30000);

		long waited = 0;
		while ((site = getSiteByName(env, name)) != null && waited < maxWaitTime && !site.hasState(SiteState.STARTED)) {
			try {
				TimeUnit.MILLISECONDS.sleep(waitTime);
				waited += waitTime;
			} catch (InterruptedException e) {
				LOGGER.error("error while waiting for site " + name, e);
				Thread.currentThread().interrupt();
			}
			LOGGER.info("site '{}' is currently in state {}, waited {}ms", name, site.getState(), waited);
		}

		return getSiteByName(env, name);
	}

	private static Map<String, Site> getSiteMap(Environment env) {
		Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		return Collections.unmodifiableMap(siteMap == null ? new HashMap<>() : siteMap);
	}

	/**
	 * Returns an immutable {@link Set} containing all the {@link Site} names.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * 
	 * @return the {@link Site} names
	 */
	public static Set<String> getSiteNames(Environment env) {
		return Collections.unmodifiableSet(getSiteMap(env).keySet());
	}

	/**
	 * Creates and returns a {@link PathInfo}-object based upon the given parameters.
	 * 
	 * @param env
	 *                    the current {@link Environment}
	 * @param site
	 *                    the current {@link Site}
	 * @param servletPath
	 *                    the current servlet-path
	 * 
	 * @return a {@link PathInfo}-object
	 */
	public static PathInfo getPathInfo(Environment env, Site site, String servletPath) {
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);

		LOGGER.trace("found site '{}' for request '{}'", site.getName(), servletPath);

		String repoPath = platformProperties.getString(Platform.Property.REPOSITORY_PATH);
		String monitoringPath = platformProperties.getString(Platform.Property.MONITORING_PATH);
		String extension = platformProperties.getString(Platform.Property.JSP_FILE_TYPE);

		Properties siteProperties = site.getProperties();
		String guiPath = siteProperties.getString(SiteProperties.MANAGER_PATH);
		String servicePath = siteProperties.getString(SiteProperties.SERVICE_PATH);
		List<String> blobDirectories = siteProperties.getList(SiteProperties.ASSETS_DIR, ";");
		List<String> documentDirectories = siteProperties.getList(SiteProperties.DOCUMENT_DIR, ";");

		return new PathInfo(site.getHost(), site.getDomain(), site.getName(), servletPath, guiPath, servicePath,
				blobDirectories, documentDirectories, repoPath, monitoringPath, extension);
	}

	/** @deprecated use {@link #getSiteName(Environment, ServletRequest)} */
	@Deprecated
	public static String getHostIdentifier(ServletRequest request, Environment env) {
		return getSiteName(env, request);
	}

	/**
	 * Retrieves a {@link Site}'s name for the given {@link ServletRequest}, using the given {@link Environment} to
	 * retrieve the {@link VHostMode}.
	 * 
	 * @param env
	 *                an {@link Environment}
	 * @param request
	 *                the {@link ServletRequest}
	 * 
	 * @return
	 *         <ul>
	 *         <li>the IP-address, if {@link VHostMode#IP_BASED} is used (see {@link ServletRequest#getLocalAddr()})
	 *         <li>the value of the request-<strong>attribute</strong> {@value #SERVER_LOCAL_NAME}, if present.
	 *         <p>
	 *         This header has to be added by the webserver of choice (usually <a href="http://httpd.apache.org/">Apache
	 *         httpd</a>), in case a {@link Site} needs to be accessible from a domain that is different from the one
	 *         configured by {@link Site#getDomain()}.
	 *         </p>
	 *         <li>the value of the request-<strong>header</strong> {@value #X_APPNG_SITE}, if present.
	 *         <li>the lower-cased server name, otherwise (see {@link ServletRequest#getServerName()})
	 *         </ul>
	 */
	public static String getSiteName(Environment env, ServletRequest request) {
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		VHostMode vHostMode = VHostMode.valueOf(platformProperties.getString(Platform.Property.VHOST_MODE));
		String siteName;
		if (VHostMode.NAME_BASED.equals(vHostMode)) {
			siteName = StringUtils.trimToNull((String) request.getAttribute(SERVER_LOCAL_NAME));
			if (null == siteName) {
				siteName = StringUtils.trimToNull(((HttpServletRequest) request).getHeader(X_APPNG_SITE));
			}
			if (null == siteName) {
				siteName = request.getServerName();
			}
		} else {
			siteName = request.getLocalAddr();
		}
		LOGGER.trace("site name: {}", siteName);
		return siteName;
	}

	private RequestUtil() {
	}

}
