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
package org.appng.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;

import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Utility-class for retrieving {@link Site}s by name,host or {@link ServletRequest} and also creating a {@link Path}
 * -object based on a {@link ServletRequest}.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
public class RequestUtil {

	private static final String SERVER_LOCAL_NAME = "SERVER_LOCAL_NAME";

	/**
	 * Retrieves a {@link Site} by its host-identifier.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param servletRequest
	 *            the current {@link ServletRequest}
	 * @return the {@link Site}, if any
	 * 
	 * @see #getHostIdentifier(ServletRequest, Environment)
	 * @see #getSiteByHost(Environment, String)
	 */
	public static Site getSite(Environment env, ServletRequest servletRequest) {
		if (null == servletRequest || null == env) {
			return null;
		}
		String hostIdentifier = getHostIdentifier(servletRequest, env);
		return getSiteByHost(env, hostIdentifier);
	}

	/**
	 * Retrieves a {@link Site} by its host.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param host
	 *            the host of the {@link Site}
	 * @return the {@link Site}, if any
	 * 
	 * @see Site#getHost()
	 */
	public static Site getSiteByHost(Environment env, String host) {
		Map<String, Site> sites = getSiteMap(env);
		if (null != sites) {
			for (Site site : sites.values()) {
				if (host.equals(site.getHost())) {
					return site;
				}
			}
		}
		return null;
	}

	/**
	 * Retrieves a {@link Site} by its name.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param name
	 *            the name of the {@link Site}
	 * @return the {@link Site}, if any
	 * 
	 * @see Site#getName()
	 */
	public static Site getSiteByName(Environment env, String name) {
		Map<String, Site> sites = getSiteMap(env);
		Site site = null;
		if (null != sites) {
			site = sites.get(name);
		}
		return site;
	}

	/**
	 * Retrieves a {@link Site} by its name, waiting up to
	 * {@code Platform.Property#MAX_WAIT_TIME} milliseconds until it's state is {@code SiteState#STARTED}.
	 * 
	 * @param env  the current {@link Environment}
	 * @param name the name of the {@link Site}
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

		long waited = 0;
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		int waitTime = platformProperties.getInteger(Platform.Property.WAIT_TIME, 1000);
		int maxWaitTime = platformProperties.getInteger(Platform.Property.MAX_WAIT_TIME, 30000);

		while (waited < maxWaitTime
				&& (site = getSiteByName(env, name)).hasState(SiteState.STOPPING, SiteState.STOPPED)) {
			try {
				Thread.sleep(waitTime);
				waited += waitTime;
			} catch (InterruptedException e) {
				LOGGER.error("error while waiting for site to be started", e);
			}
			LOGGER.info("site '{}' is currently in state {}, waited {}ms", site, site.getState(), waited);
		}

		while (waited < maxWaitTime && (site = getSiteByName(env, name)).hasState(SiteState.STARTING)) {
			try {
				Thread.sleep(waitTime);
				waited += waitTime;
			} catch (InterruptedException e) {
				LOGGER.error("error while waiting for site to be started", e);
			}
			LOGGER.info("site '{}' is currently being started, waited {}ms", site, waited);
		}
		return getSiteByName(env, name);
	}

	private static Map<String, Site> getSiteMap(Environment env) {
		Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		return Collections.unmodifiableMap(siteMap);
	}

	/**
	 * Returns an immutable {@link Set} containing all the {@link Site} names.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @return the {@link Site} names
	 */
	public static Set<String> getSiteNames(Environment env) {
		Map<String, Site> sites = getSiteMap(env);
		if (null != sites) {
			return Collections.unmodifiableSet(sites.keySet());
		}
		return Collections.emptySet();
	}

	/**
	 * Creates and returns a {@link PathInfo}-object based upon the given parameters.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param site
	 *            the current {@link Site}
	 * @param servletPath
	 *            the current servlet-path
	 * @return a {@link PathInfo}-object
	 */
	public static PathInfo getPathInfo(Environment env, Site site, String servletPath) {
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		Properties activeSiteProperties = site.getProperties();

		LOGGER.trace("found site '{}' for request '{}'", site.getName(), servletPath);

		String repoPath = platformProperties.getString(Platform.Property.REPOSITORY_PATH);
		String extension = platformProperties.getString(Platform.Property.JSP_FILE_TYPE);

		Properties siteProperties = site.getProperties();
		String guiPath = siteProperties.getString(SiteProperties.MANAGER_PATH);
		String servicePath = siteProperties.getString(SiteProperties.SERVICE_PATH);
		List<String> blobDirectories = activeSiteProperties.getList(SiteProperties.ASSETS_DIR, ";");
		List<String> documentDirectories = activeSiteProperties.getList(SiteProperties.DOCUMENT_DIR, ";");

		if (blobDirectories.isEmpty()) {
			blobDirectories = siteProperties.getList(SiteProperties.ASSETS_DIR, ";");
		}
		if (documentDirectories.isEmpty()) {
			documentDirectories = siteProperties.getList(SiteProperties.DOCUMENT_DIR, ";");
		}

		return new PathInfo(site.getHost(), site.getDomain(), site.getName(), servletPath, guiPath, servicePath,
				blobDirectories, documentDirectories, repoPath, extension);
	}

	/**
	 * Retrieves the host-identifier for the given {@link ServletRequest}, using the given {@link Environment} to
	 * retrieve the {@link VHostMode} used by appNG.
	 * 
	 * @param request
	 *            the {@link ServletRequest}
	 * @param env
	 *            an {@link Environment}
	 * @return
	 *         <ul>
	 *         <li>the IP-address, if {@link VHostMode#IP_BASED} is used (see {@link ServletRequest#getLocalAddr()})
	 *         <li>the value of the request-header {@code SERVER_LOCAL_NAME}, if present. This header has to be added by
	 *         the webserver of choice (usually <a href="http://httpd.apache.org/">Apache httpd</a>), in case a
	 *         {@link Site} needs to be accessible from a domain that is different from the one configured by
	 *         {@link Site#getDomain()}.
	 *         <li>the lower-cased server name, otherwise (see {@link ServletRequest#getServerName()})
	 *         </ul>
	 */
	public static String getHostIdentifier(ServletRequest request, Environment env) {
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		VHostMode vHostMode = VHostMode.valueOf(platformProperties.getString(Platform.Property.VHOST_MODE));
		String hostIdentifier;
		LOGGER.trace("hostmode: {}", vHostMode);
		if (vHostMode.equals(VHostMode.IP_BASED)) {
			hostIdentifier = request.getLocalAddr();
		} else {
			// APPNG-200
			String serverLocalName = (String) request.getAttribute(SERVER_LOCAL_NAME);
			if (serverLocalName != null) {
				hostIdentifier = serverLocalName;
			} else {
				hostIdentifier = request.getServerName().toLowerCase();
			}
		}
		LOGGER.trace("hostIdentifier: {}", hostIdentifier);
		return hostIdentifier;
	}

	private RequestUtil() {
	}

}
