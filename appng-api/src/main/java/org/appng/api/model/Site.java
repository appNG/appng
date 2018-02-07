/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.api.model;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.appng.api.Environment;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.messaging.Event;

/**
 * 
 * A {@link Site} is the highest level entry-point to the platform. Besides a unique name, it needs to have a domain and
 * a host. A site can use any of the several {@link Application}s which have been deployed to the platform.
 * 
 * @author Matthias Müller
 */
public interface Site extends Named<Integer> {

	/**
	 * The different states a site can have
	 */
	enum SiteState {
		STARTING, STARTED, STOPPING, STOPPED, INACTIVE, DELETED;
	}

	/**
	 * Returns the {@link Application}s which are currently assigned to this {@link Site}.
	 * 
	 * @return the {@link Application}s currently assigned to this {@link Site}
	 */
	Set<Application> getApplications();

	/**
	 * Returns the {@link Application} with the given name, if such a {@link Application} is assigned to this
	 * {@link Site}.
	 * 
	 * @param name
	 *            the name of the {@link Application}
	 * @return the {@link Application}, if such a {@link Application} is assigned to this {@link Site}, {@code null}
	 *         otherwise
	 */
	Application getApplication(String name);

	/**
	 * Checks whether the {@link Application} with the given name is assigned to this {@link Site}.
	 * 
	 * @param name
	 *            the name of the {@link Application}
	 * @return {@code true} if the {@link Application} with the given name is assigned to this {@link Site},
	 *         {@code false} otherwise
	 */
	boolean hasApplication(String name);

	/**
	 * Returns the host for this site
	 * 
	 * @return the host
	 */
	String getHost();

	/**
	 * Returns the domain for this {@link Site}.
	 * 
	 * @return the domain
	 */
	String getDomain();

	/**
	 * Returns the {@link Properties} for this {@link Site}.
	 * 
	 * @return the {@link Properties} for this {@link Site}
	 * @see org.appng.api.SiteProperties
	 */
	Properties getProperties();

	/**
	 * Returns the {@link URLClassLoader} for this {@link Site}, which contains all the jars provided by the
	 * {@link Application}s that are assigned to this
	 * 
	 * {@link Site}.
	 * 
	 * @return the {@link URLClassLoader}
	 */
	URLClassLoader getSiteClassLoader();

	/**
	 * Returns {@code true} if this {@link Site} is active, which means it is being loaded at platform startup.
	 * 
	 * @return {@code true} if this {@link Site} is active, {@code false} otherwise
	 */
	boolean isActive();

	/**
	 * Returns {@code true} if a JSP-repository folder has been created for this {@link Site}.
	 * 
	 * @return {@code true} if a JSP-repository folder has been created
	 */
	boolean isCreateRepository();

	/**
	 * sends a redirect with HTTP-Status 301 (moved permanently)
	 * 
	 * @see #sendRedirect(Environment, String, Integer)
	 */
	void sendRedirect(Environment env, String target);

	/**
	 * redirects the request to the given target. If the target <b>not</b> starts with a slash ("/"), the target has to
	 * be a relative path to a certain {@link Application} of this site (like "application/page"). If the target
	 * <b>does</b> start with a slash, it is expected to be a target relative to the domain of this site.
	 * 
	 * @param env
	 *            the actual {@link Environment}
	 * @param target
	 *            the redirect target
	 * @param statusCode
	 *            -the HTTP status code to send. Use constants from {@link HttpServletRequest}
	 */
	void sendRedirect(Environment env, String target, Integer statusCode);

	/**
	 * Returns the {@link File} defined by the given path, which is located relative to the site's repository-folder.
	 * 
	 * @param relativePath
	 *            the relative path of the file
	 * @return the {@link File}, if such a path exists
	 */
	File readFile(String relativePath);

	/**
	 * Returns a set containing a {@link Named}-instance for each {@link Group} that exists within the platform.
	 * 
	 * @return the {@link Named} groups
	 */
	Set<Named<Integer>> getGroups();

	/**
	 * Returns the {@link PasswordPolicy} used when creating {@link Subject}s for this {@link Site}.
	 * 
	 * @return the {@link PasswordPolicy}
	 */
	PasswordPolicy getPasswordPolicy();

	/**
	 * Returns the time at which the site has been loaded during platform-startup.
	 * 
	 * @return the time, or {@code null} if the site has (yet) not been loaded.
	 */
	Date getStartupTime();

	/**
	 * If clustering is enabled, sends an {@link Event} to other appNG nodes
	 * 
	 * @param event
	 *            the event to send
	 * @return {@code true} if the event was sent successfully
	 */
	boolean sendEvent(Event event);
	
	SiteState getState();
	
	boolean hasState(SiteState... states);

}
