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
package org.appng.taglib;

import static org.appng.api.Scope.PLATFORM;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.RequestUtil;
import org.appng.api.model.Site;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.ApplicationProvider;
import org.appng.core.service.CoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Utility class to retrieve the calling and the executing {@link Site} and the right {@link ApplicationProvider} that
 * is responsible for handling a taglet-call.
 * 
 * @author Matthias Müller
 * @author Matthias Herlitzius
 * 
 * @see SiteApplication#getGrantedSites()
 */
public class MultiSiteSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultiSiteSupport.class);
	private SiteImpl callingSite;
	private SiteImpl executingSite;
	private ApplicationProvider applicationProvider;

	public void process(Environment env, String application, HttpServletRequest servletRequest) throws JspException {
		process(env, application, null, servletRequest);
	}

	public void process(Environment env, String application, String method, HttpServletRequest servletRequest)
			throws JspException {
		callingSite = (SiteImpl) RequestUtil.getSite(env, servletRequest);
		executingSite = callingSite;

		applicationProvider = (ApplicationProvider) callingSite.getSiteApplication(application);
		if (null == applicationProvider) {
			LOGGER.debug("application '{}' not found for site '{}', trying in granting sites", application,
					callingSite.getName());
			ApplicationContext ctx = env.getAttribute(PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
			Site site = ctx.getBean(CoreService.class).getGrantingSite(callingSite.getName(), application);
			if (null != site) {
				LOGGER.debug("site '{}' is granting site '{}' access to application '{}'", site.getName(),
						callingSite.getName(), application);
				executingSite = (SiteImpl) RequestUtil.getSiteByName(env, site.getName());
				if (null != executingSite) {
					applicationProvider = (ApplicationProvider) ((SiteImpl) executingSite)
							.getSiteApplication(application);
					if (null == applicationProvider) {
						throw new JspException(
								String.format(
										"the application '%s' is not available for granting site '%s', check logs to see why it didn't start.",
										application, site.getName()));
					}
				} else {
					throw new JspException(String.format(
							"the granting site '%s' is not running, so application '%s' can not be called!",
							site.getName(), application));
				}
			} else {
				throw new JspException(String.format("no application '%s' for site '%s'", application,
						callingSite.getName()));
			}
		}
		if (null != method && !applicationProvider.containsBean(method)) {
			throw new JspException(String.format("no method '%s' for application '%s' in site '%s'", method,
					application, callingSite.getName()));
		}
	}

	public SiteImpl getCallingSite() {
		return callingSite;
	}

	public SiteImpl getExecutingSite() {
		return executingSite;
	}

	public ApplicationProvider getApplicationProvider() {
		return applicationProvider;
	}

}
