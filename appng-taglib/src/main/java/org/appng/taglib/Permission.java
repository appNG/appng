/*
 * Copyright 2011-2017 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.appng.api.Environment;
import org.appng.api.PermissionProcessor;
import org.appng.api.model.Application;
import org.appng.api.model.Subject;
import org.appng.api.support.DefaultPermissionProcessor;
import org.appng.api.support.DummyPermissionProcessor;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.SiteImpl;
import org.appng.core.model.ApplicationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes the taglet's body if the current {@link Subject} has the required {@link org.appng.api.model.Permission} of
 * the given {@link Application}.
 * <p/>
 * <b>Usage:</b>
 * 
 * <pre>
 * &lt;appNG:permission application="foobar" permission="do.something">Do it!&lt;/appNG:permission>
 * </pre>
 * 
 * @author Matthias Herlitzius
 */
public class Permission extends TagSupport {

	private static final long serialVersionUID = 1L;
	private static Logger log = LoggerFactory.getLogger(Permission.class);
	private String application;
	private String permission;

	@Override
	public int doStartTag() throws JspException {
		try {
			Environment env = DefaultEnvironment.get(pageContext);
			HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
			MultiSiteSupport multiSiteSupport = processMultiSiteSupport(env, servletRequest);
			SiteImpl executingSite = multiSiteSupport.getExecutingSite();
			ApplicationProvider applicationProvider = multiSiteSupport.getApplicationProvider();
			Subject subject = env.getSubject();
			PermissionProcessor permissionProcessor = null;
			Boolean permissionsEnabled = applicationProvider.getProperties().getBoolean("permissionsEnabled",
					Boolean.TRUE);
			if (permissionsEnabled) {
				permissionProcessor = new DefaultPermissionProcessor(subject, executingSite, applicationProvider);
			} else {
				permissionProcessor = new DummyPermissionProcessor(subject, executingSite, applicationProvider);
			}
			boolean hasPermission = permissionProcessor.hasPermission(permission);
			if (hasPermission) {
				log.debug("subject has the permission: {}", permission);
				return EVAL_BODY_INCLUDE;
			}
		} catch (IllegalStateException e) {
			log.debug("session {} is invalid,", pageContext.getSession().getId());
		}
		log.debug("subject does not have the permission: {}", permission);
		return SKIP_BODY;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	protected MultiSiteSupport processMultiSiteSupport(Environment env, HttpServletRequest servletRequest)
			throws JspException {
		MultiSiteSupport multiSiteSupport = new MultiSiteSupport();
		multiSiteSupport.process(env, application, servletRequest);
		return multiSiteSupport;
	}

}
