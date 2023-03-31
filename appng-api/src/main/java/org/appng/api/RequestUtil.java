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
import org.springframework.http.HttpHeaders;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility-class for mapping requests to {@link Site}s. Also creates a {@link Path} object based on a
 * {@link ServletRequest}.
 *
 * <p>
 * <strong>Mapping logic</strong>
 * </p>
 *
 * <p>
 * <ul>
 * <li>If the platform is running in {@link VHostMode#IP_BASED}:
 * <p>
 * The value of {@link ServletRequest#getLocalAddr()} is compared against the values returned by {@link Site#getHost()}
 * and {@link Site#getHostAliases()} of each site. If a site matches, it is returned.
 * </p>
 * </li>
 * <li>If the platform is running in {@link VHostMode#NAME_BASED}:
 * <p>
 * If the {@link ServletRequest} contains the {@code SERVER_LOCAL_NAME} attribute or the header {@code X_APPNG_SITE}, it
 * is assumed that an upstream reverse-proxy has taken care of site-mapping already and the respective attribute
 * contains a site name ({@link Site#getName()}). If a site with this name exists, it is returned.
 * {@link Site#getHost()} and {@link Site#getHostAliases()} are ignored.
 * </p>
 * <p>
 * If the mentioned attributes are not present, the value of {@link ServletRequest#getServerName()} (corresponds to the
 * {@code HOST} header) is compared against the values returned by {@link Site#getHost()} and
 * {@link Site#getHostAliases()} of each site. If a site matches, it is returned.
 * </p>
 * </li>
 * </ul>
 * 
 * @author Matthias Müller
 * @author Dirk Heuvels
 */
@Slf4j
public class RequestUtil {

	static final String SERVER_LOCAL_NAME = "SERVER_LOCAL_NAME";
	static final String X_APPNG_SITE = "X-appNG-site";

	private enum MatchScope {
		MATCH_SITE_NAME, MATCH_SITE_HOSTS, UNDEFINED
	}

	/**
	 * Class to describe the request's name attribute used for site-mapping.
	 */
	private static class RequestIdentifier {
		public String name;
		public MatchScope matchScope;

		RequestIdentifier() {
			this.matchScope = MatchScope.UNDEFINED;
		}

		RequestIdentifier(String name, MatchScope matchScope) {
			this.name = name;
			this.matchScope = matchScope;
		}
	}

	/**
	 * Retrieves a {@link Site} using the mapping logic described in the class documentation above.
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param servletRequest
	 *            the current {@link ServletRequest}
	 * 
	 * @return the {@link Site} or null, if no site matched
	 * 
	 */
	public static Site getSite(Environment env, ServletRequest servletRequest) {
		if (null == servletRequest || null == env)
			return null;
		RequestIdentifier reqId = getRequestIdentifier(env, servletRequest);
		switch (reqId.matchScope) {
		case MATCH_SITE_NAME:
			return getSiteByName(env, reqId.name);
		case MATCH_SITE_HOSTS:
			return getSiteByHost(env, reqId.name);
		default:
			return null;
		}
	}

	/**
	 * Retrieves a {@link Site} by its hostnames (primary and aliases).
	 * 
	 * @param env
	 *            the current {@link Environment}
	 * @param host
	 *            the name to be compared against the {@link Site}'s hostnames
	 * 
	 * @return the {@link Site} or null if no site matches
	 * 
	 * @see Site#getHost()
	 * @see Site#getHostAliases()
	 *
	 */
	public static Site getSiteByHost(Environment env, String host) {
		if (null == host || host.isEmpty())
			return null;
		for (Site site : getSiteMap(env).values()) {
			if (host.equals(site.getHost()) || site.getHostAliases().contains(host))
				return site;
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
	 *            the current {@link Environment}
	 * @param name
	 *            the name of the {@link Site}
	 * 
	 * @return the {@link Site}, if any
	 * 
	 * @see #getSiteByName(Environment, String)
	 * @see Site#hasState(SiteState...)
	 */
	public static Site waitForSite(Environment env, String name) {
		Site site = getSiteByName(env, name);
		if (null == site || site.hasState(SiteState.STARTED, SiteState.SUSPENDED, SiteState.INACTIVE, SiteState.DELETED)) {
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
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("site '{}' is currently in state {}, waited {}ms", name, site.getState(), waited);
			}
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
	 *            the current {@link Environment}
	 * @param site
	 *            the current {@link Site}
	 * @param servletPath
	 *            the current servlet-path
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

	/** @deprecated use {@link #getRequestIdentifier(Environment, ServletRequest)} */
	@Deprecated
	public static String getHostIdentifier(ServletRequest request, Environment env) {
		return getSiteName(env, request);
	}

	/** @deprecated use {@link #getRequestIdentifier(Environment, ServletRequest)} */
	@Deprecated
	public static String getSiteName(Environment env, ServletRequest request) {
		RequestIdentifier reqId = getRequestIdentifier(env, request);
		return reqId.name;
	}

	/**
	 * Retrieves a name according to the mapping logic described in the class documentation above together with the
	 * matching scope to be applied.
	 * 
	 * @param env
	 *            an {@link Environment}
	 * @param request
	 *            the {@link ServletRequest}
	 * 
	 * @return {@link RequestUtil.RequestIdentifier} object
	 */
	public static RequestIdentifier getRequestIdentifier(Environment env, ServletRequest request) {
		Properties platformProperties = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		VHostMode vHostMode = VHostMode.valueOf(platformProperties.getString(Platform.Property.VHOST_MODE));

		RequestIdentifier reqId = new RequestIdentifier();
		if (VHostMode.IP_BASED.equals(vHostMode)) {
			reqId = new RequestIdentifier(request.getLocalAddr(), MatchScope.MATCH_SITE_HOSTS);
			LOGGER.trace("Using '{}' from 'ServletRequest.getLocalAddr()' for site mapping", reqId.name);
		} else {
			String attrVal = null;
			attrVal = StringUtils.trimToNull((String) request.getAttribute(SERVER_LOCAL_NAME));
			if (null != attrVal) {
				reqId = new RequestIdentifier(attrVal, MatchScope.MATCH_SITE_NAME);
				LOGGER.trace("Using '{}' from request attribute '{}' for site mapping", attrVal, SERVER_LOCAL_NAME);
			} else {
				attrVal = StringUtils.trimToNull(((HttpServletRequest) request).getHeader(X_APPNG_SITE));
				if (null != attrVal) {
					reqId = new RequestIdentifier(attrVal, MatchScope.MATCH_SITE_NAME);
					LOGGER.trace("Using '{}' from request header '{}' for site mapping", attrVal, X_APPNG_SITE);
				} else {
					reqId = new RequestIdentifier(request.getServerName(), MatchScope.MATCH_SITE_HOSTS);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace(
								"Using '{}' from 'request.getServerName()' for site mapping (HOST header was '{}')",
								reqId.name, ((HttpServletRequest) request).getHeader(HttpHeaders.HOST));
					}
				}
			}
		}
		return reqId;
	}

	private RequestUtil() {
	}

}
