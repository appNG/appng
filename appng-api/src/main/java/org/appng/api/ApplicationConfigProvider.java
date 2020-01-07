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
package org.appng.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.appng.api.model.Application;
import org.appng.api.model.Resource;
import org.appng.api.model.Resources;
import org.appng.xml.MarshallService;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.PageDefinition;

/**
 * Provides the basic configuration of a {@link Application}, which is the {@link ApplicationInfo} parsed from the
 * {@code application-info.xml} and all defined {@link PageDefinition}s, {@link Event}s, {@link Action}s,
 * {@link Datasource}s and also the {@link ApplicationRootConfig}.
 * 
 * @author Matthias Müller
 * 
 */
public interface ApplicationConfigProvider extends Closeable {

	/**
	 * Clones this {@link ApplicationConfigProvider}, which is necessary because the
	 * 
	 * @param marshallService
	 *            a {@link MarshallService} to read the configuration files, only needed if appNG is in development-mode
	 * @return a new ApplicationConfigProvider
	 * @throws InvalidConfigurationException
	 *             if there is some configuration error inside the application
	 */
	ApplicationConfigProvider cloneConfig(MarshallService marshallService) throws InvalidConfigurationException;

	/**
	 * Returns the {@link ApplicationRootConfig}.
	 * 
	 * @return the {@link ApplicationRootConfig}
	 */
	ApplicationRootConfig getApplicationRootConfig();

	/**
	 * Returns the id of the default-page, which is the {@link PageDefinition} with the type '{@code index}'.
	 * 
	 * @return the id of the default-page
	 */
	String getDefaultPage();

	/**
	 * Returns the {@link PageDefinition} with the given id, if present.
	 * 
	 * @param id
	 *            the id of the {@link PageDefinition} to get
	 * @return the {@link PageDefinition} with the given id, or {@code null} if no such {@link PageDefinition} exists
	 */
	PageDefinition getPage(String id);

	/**
	 * Returns a map of all {@link PageDefinition}s, using the id as the key.
	 * 
	 * @return a map of all {@link PageDefinition}s
	 */
	Map<String, PageDefinition> getPages();

	/**
	 * Returns the {@link Event} with the given id, if present.
	 * 
	 * @param id
	 *            the id of the {@link Event} to get
	 * @return the {@link Event} with the given id, or {@code null} if no such {@link Event} exists
	 */
	Event getEvent(String id);

	/**
	 * Returns the {@link Datasource} with the given id, if present.
	 * 
	 * @param id
	 *            the id of the {@link Datasource} to get
	 * @return the {@link Datasource} with the given id, or {@code null} if no such {@link Datasource} exists
	 */
	Datasource getDatasource(String id);

	/**
	 * Returns the {@link Action} with the given {@code actionId}, which belongs to the {@link Event} with the given
	 * {@code eventId}.
	 * 
	 * @param eventId
	 *            the id of the {@link Event}
	 * @param actionId
	 *            the id of the {@link Action}
	 * @return the {@link Action}, or {@code null} if there is no such {@link Event} or {@link Action}
	 */
	Action getAction(String eventId, String actionId);

	/**
	 * Returns a map of all {@link Action}s for the {@link Event} with the given id, using the id as the key.
	 * 
	 * @param eventId
	 *            the id of the {@link Event} to get the {@link Action}s for
	 * @return a map of all {@link Action}s for the given {@link Event}, or {@code null} if there is no such event
	 */
	Map<String, Action> getActions(String eventId);

	/**
	 * Returns a map of all {@link Datasource}s, using the id as the key.
	 * 
	 * @return a map of all {@link Datasource}s
	 */
	Map<String, Datasource> getDataSources();

	/**
	 * Returns the name of the {@link Resource} where the {@link Event} with the given id was defined.
	 * 
	 * @param eventId
	 *            the id of the {@link Event}
	 * @return the name of the {@link Resource} where the {@link Event} with the given id was defined, or {@code null}
	 *         if there is no such {@link Event}
	 */
	String getResourceNameForEvent(String eventId);

	/**
	 * Returns the name of the {@link Resource} where the {@link PageDefinition} with the given id was defined.
	 * 
	 * @param pageId
	 *            the id of the {@link PageDefinition}
	 * @return the name of the {@link Resource} where the {@link PageDefinition} with the given id was defined, or
	 *         {@code null} if there is no such {@link PageDefinition}
	 */
	String getResourceNameForPage(String pageId);

	/**
	 * Returns the name of the {@link Resource} where the {@link Datasource} with the given id was defined.
	 * 
	 * @param dataSourceId
	 *            the id of the {@link Datasource}
	 * @return the name of the {@link Resource} where the {@link Datasource} with the given id was defined, or
	 *         {@code null} if there is no such {@link Datasource}
	 */
	String getResourceNameForDataSource(String dataSourceId);

	/**
	 * Returns the name of the {@link Resource} where the {@link ApplicationRootConfig} was defined.
	 * 
	 * @return the name of the {@link Resource} where the {@link ApplicationRootConfig} was defined.
	 */
	String getResourceNameForApplicationRootConfig();

	/**
	 * Returns the {@link ApplicationInfo} for the {@link Application}, parsed from {@code application-info.xml}
	 * 
	 * @return the {@link ApplicationInfo}
	 */
	ApplicationInfo getApplicationInfo();

	Resources getResources();

	void close() throws IOException;

	/**
	 * Returns all ids from all known {@link Event}s
	 * 
	 * @return
	 */
	Set<String> getEventIds();

}
