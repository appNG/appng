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

import org.appng.api.model.Application;
import org.appng.api.model.Site;

/**
 * Provides methods to startup and shutdown a {@link Application}. A {@link Application} <b>is NOT forced to provide an
 * implementation of this</b>, it's completely optional. {@link Application}s which need to allocate/free some resources on
 * startup/shutdown should use this interface. In each {@link Application} there can only be <b>one</b> bean implementing
 * {@link ApplicationController}.
 * 
 * @author Matthias Müller
 * 
 */
public interface ApplicationController {

	/**
	 * This method is called when a {@link Site} is being (re)loaded. A {@link Application} is supposed to initialize itself
	 * within this method.
	 * 
	 * @param site
	 *            the {@link Site} to which the {@link Application} is assigned to
	 * @param application
	 *            the {@link Application} which is assigned to the {@link Site}
	 * @param environment
	 *            the current {@link Environment}
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	boolean start(Site site, Application application, Environment environment);

	/**
	 * This method is called when a {@link Site} is being shutdown. A {@link Application} is supposed to shutdown itself
	 * within this method (free resources etc.).
	 * 
	 * @param site
	 *            the {@link Site} to which the {@link Application} is assigned to
	 * @param application
	 *            the {@link Application} which is assigned to the {@link Site}
	 * @param environment
	 *            the current {@link Environment}
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	boolean shutdown(Site site, Application application, Environment environment);

	/**
	 * This method is called immediately after removing a {@link Application} from a {@link Site}, which has not been
	 * reloaded at this point. The purpose is, similar to {@link #shutdown(Site, Application, Environment)}, to free
	 * resources.
	 * 
	 * @param site
	 *            the {@link Site} from which the {@link Application} is being removed
	 * @param application
	 *            the {@link Application} which is being removed from the {@link Site}
	 * @param environment
	 *            the current {@link Environment}
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	boolean removeSite(Site site, Application application, Environment environment);

	/**
	 * This method is called immediately after adding a {@link Application} from a {@link Site}, which has not been reloaded
	 * at this point.
	 * 
	 * @param site
	 *            the {@link Site} to which the {@link Application} was assigned to
	 * @param application
	 *            the {@link Application} which was assigned to the {@link Site}
	 * @param environment
	 *            the current {@link Environment}
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	boolean addSite(Site site, Application application, Environment environment);

}
