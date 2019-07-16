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
package org.appng.api.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.appng.api.ApplicationConfigProvider;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.xml.MarshallService;
import org.appng.xml.application.ApplicationInfo;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.ApplicationRootConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.Datasources;
import org.appng.xml.platform.Event;
import org.appng.xml.platform.Events;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.Pages;
import org.appng.xml.platform.Param;
import org.appng.xml.platform.Platform;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Default {@link ApplicationConfigProvider}-implementation.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class ApplicationConfigProviderImpl implements ApplicationConfigProvider {

	private static final String RESOURCE_MAP_KEY_EVENT = "event:";
	private static final String RESOURCE_MAP_KEY_PAGE = "page:";
	private static final String RESOURCE_MAP_KEY_DATASOURCE = "datasource:";
	private static final String RESOURCE_MAP_KEY_APPLICATION_ROOT_CONFIG = "applicationRootConfig";

	protected ActionMap actionMap;
	protected DataSourceMap datasourceMap;
	protected PageMap pageMap;
	protected EventMap eventMap;
	protected Map<String, String> resourceMap;

	protected List<Object[]> descendantDatasources = new ArrayList<>();

	protected ConfigValidator validator;

	protected ApplicationRootConfig rootConfig;
	private Set<String> sessionParams;
	private Collection<Resource> xmlFiles;

	private String defaultPage;
	private boolean devMode;
	private boolean loaded = false;
	private byte[] data;

	private String applicationName;

	private ApplicationInfo applicationInfo;

	private Resources resources;

	private ApplicationConfigProviderImpl(MarshallService marshallService, String applicationName,
			Collection<Resource> applicationResources, ApplicationInfo applicationInfo, Resources resources,
			Boolean devMode) throws InvalidConfigurationException {
		this.xmlFiles = applicationResources;
		this.applicationName = applicationName;
		this.applicationInfo = applicationInfo;
		this.validator = new ConfigValidator(this);
		this.resourceMap = new HashMap<>();
		this.resources = resources;
		setDevMode(devMode);
		loadConfig(marshallService);
	}

	private ApplicationConfigProviderImpl(String applicationName, boolean devMode, byte[] data)
			throws IOException, ClassNotFoundException {
		this.applicationName = applicationName;
		this.data = ArrayUtils.clone(data);
		this.devMode = devMode;
		this.loaded = true;
		readData();
	}

	public ApplicationConfigProviderImpl(MarshallService marshallService, String applicationName,
			Resources applicationResources, boolean devMode) throws InvalidConfigurationException {
		this.resources = applicationResources;
		this.xmlFiles = applicationResources.getResources(ResourceType.XML);
		this.applicationName = applicationName;
		this.applicationInfo = applicationResources.getApplicationInfo();
		this.validator = new ConfigValidator(this);
		this.resourceMap = new HashMap<>();
		setDevMode(devMode);
		loadConfig(marshallService);
	}

	private void setDevMode(boolean devMode) {
		this.devMode = devMode;
	}

	public Map<String, Datasource> getDataSources() {
		return datasourceMap;
	}

	private synchronized void loadConfig(MarshallService marshallService) throws InvalidConfigurationException {
		long start = System.currentTimeMillis();
		try {
			clear();
			if (!loaded || devMode) {
				readResources(marshallService);
			} else {
				readData();
			}
		} catch (Exception e) {
			LOGGER.error("error while reading configuration", e);
		}
		long duration = System.currentTimeMillis() - start;
		LOGGER.debug("loading config for application {} took {}ms", applicationName, duration);
	}

	private void readResources(MarshallService marshallService) throws IOException, InvalidConfigurationException {
		for (Resource applicationResource : xmlFiles) {
			String name = applicationResource.getName();
			InputStream inputStream = null;
			try {
				if (devMode) {
					inputStream = new FileInputStream(applicationResource.getCachedFile());
				} else {
					inputStream = new ByteArrayInputStream(applicationResource.getBytes());
				}
				Object object = marshallService.unmarshall(inputStream);
				readConfig(name, object);
			} catch (JAXBException e) {
				LOGGER.error(String.format("error while unmarshalling %s", name), e);
			} finally {
				if (null != inputStream) {
					inputStream.close();
				}
			}
		}
		processInheritance(marshallService);
		writeData();
		if (devMode) {
			validate();
		}
		loaded = true;
	}

	private void validate() throws MalformedURLException, InvalidConfigurationException, IOException {
		URLClassLoader classLoader;
		Set<Resource> jars = resources.getResources(ResourceType.JAR);
		URL[] urls = new URL[jars.size()];
		int i = 0;
		for (Resource resource : jars) {
			urls[i++] = resource.getCachedFile().toURI().toURL();
		}
		classLoader = new URLClassLoader(urls, getClass().getClassLoader());
		try {
			validator.validate(applicationName, classLoader);
		} finally {
			if (null != classLoader) {
				classLoader.close();
				classLoader = null;
			}
		}
	}

	private void processInheritance(MarshallService marshallService) {

		// we have to face the fact that there could be a chain of inheritance. We have to loop over the list of
		// descendants until all have been processed
		while (!descendantDatasources.isEmpty()) {
			int actualSize = descendantDatasources.size();
			List<Object[]> descendantsWithoutAncestor = new ArrayList<>();
			for (Object[] descendantDef : descendantDatasources) {
				Datasource ds = (Datasource) descendantDef[0];
				Datasource ancestor = getDatasource(DatasourceInheritanceHelper.getAncestorId(ds.getId()));
				if (null != ancestor) {
					Datasource descendant = DatasourceInheritanceHelper.inherit(ds, ancestor, marshallService);
					if (null != descendant) {
						addDataSource(descendant, (String) descendantDef[1]);
					} else {
						LOGGER.error("inheritance did not create an new datasource instance");
					}
				} else {
					// This can happen if the ancestor also inherits from another datasource. As long as this
					// inheritance is not processed, the ancestor cannot be found by its final id
					descendantsWithoutAncestor.add(descendantDef);
				}

			}

			// reinitialize the list with the left over descendants where the inheritance could not be processed yet.
			descendantDatasources = descendantsWithoutAncestor;

			// the size must decrease on each iteration if not, we have an descendant where the inheritance can not be
			// processed (maybe because of a typo). Thus the abort condition for the main loop will never be fulfilled
			// (the list will never be empty) so we have to handle this case explicit and write some errors in the
			// log-file
			if (actualSize <= descendantDatasources.size()) {
				for (Object[] ds : descendantDatasources) {
					LOGGER.error("Cannot process inheritance for {} maybe the ancestor is not defined.",
							((Datasource) ds[0]).getId());
				}
				descendantDatasources.clear();
				return;
			}
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void readData() throws IOException, ClassNotFoundException {
		Object o = null;
		try (
				ByteArrayInputStream bais = new ByteArrayInputStream(data);
				ObjectInputStream is = new ObjectInputStream(bais)) {
			while ((o = is.readObject()) != null) {
				if (o instanceof PageMap) {
					this.pageMap = (PageMap) o;
				} else if (o instanceof ActionMap) {
					this.actionMap = (ActionMap) o;
				} else if (o instanceof DataSourceMap) {
					this.datasourceMap = (DataSourceMap) o;
				} else if (o instanceof EventMap) {
					this.eventMap = (EventMap) o;
				} else if (o instanceof ApplicationRootConfig) {
					this.rootConfig = (ApplicationRootConfig) o;
				} else if (o instanceof String) {
					this.defaultPage = (String) o;
				} else if (o instanceof Set) {
					this.sessionParams = (Set) o;
				} else if (o instanceof Map) {
					this.resourceMap = (Map) o;
				}
			}
		} catch (EOFException e) {
			// ObjectInputStream seems to work like this...
		} catch (IOException e) {
			throw e;
		}
	}

	private void writeData() throws IOException {
		try (
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ObjectOutputStream outputStream = new ObjectOutputStream(out)) {
			data = null;
			outputStream.writeObject(pageMap);
			outputStream.writeObject(actionMap);
			outputStream.writeObject(datasourceMap);
			outputStream.writeObject(eventMap);
			outputStream.writeObject(rootConfig);
			if (null != defaultPage) {
				outputStream.writeObject(defaultPage);
			}
			if (null != sessionParams) {
				outputStream.writeObject(sessionParams);
			}
			out.close();
			outputStream.close();
			data = out.toByteArray();
			LOGGER.debug("wrote {} bytes of data for application {}", data.length, applicationName);
		} catch (IOException e) {
			throw e;
		}
	}

	private void addDataSource(Datasource ds, String resourceName) {
		Datasource oldVal = datasourceMap.put(ds.getId(), ds);
		resourceMap.put(RESOURCE_MAP_KEY_DATASOURCE + ds.getId(), resourceName);
		if (null != oldVal) {
			LOGGER.warn("overriding previously defined datasource '{}'", ds.getId());
		}
		LOGGER.trace("added datasource '{}'", ds.getId());
	}

	// there should be only one application root config. Therefore no ID is needed.
	private void addApplicationRootConfig(String resourceName) {
		resourceMap.put(RESOURCE_MAP_KEY_APPLICATION_ROOT_CONFIG, resourceName);
	}

	private void addEvent(Event e, String resourceName) {
		Event oldVal = eventMap.put(e.getId(), e);
		resourceMap.put(RESOURCE_MAP_KEY_EVENT + e.getId(), resourceName);
		Map<String, Action> actions = new HashMap<>();
		for (Action action : e.getActions()) {
			actions.put(action.getId(), action);
		}
		actionMap.put(e.getId(), actions);
		if (null != oldVal) {
			LOGGER.warn("overriding previously defined event '{}'", e.getId());
		}
		LOGGER.trace("added event '{}'", e.getId());
	}

	private void addPage(PageDefinition p, String resourceName) {
		PageDefinition oldVal = pageMap.put(p.getId(), p);
		resourceMap.put(RESOURCE_MAP_KEY_PAGE + p.getId(), resourceName);
		if (null != oldVal) {
			LOGGER.warn("overriding previously defined page '{}'", p.getId());
		}
		if ("index".equals(p.getType())) {
			this.defaultPage = p.getId();
			LOGGER.trace("added default page '{}'", p.getId());
		} else {
			LOGGER.trace("added page '{}'", p.getId());
		}
	}

	protected synchronized void clear() {
		this.sessionParams = new HashSet<>();
		this.pageMap = new PageMap();
		this.datasourceMap = new DataSourceMap();
		this.eventMap = new EventMap();
		this.actionMap = new ActionMap();
		this.defaultPage = null;
		this.rootConfig = null;
	}

	public Action getAction(String eventId, String actionId) {
		String realId;
		if (actionId.startsWith(eventId + ":")) {
			realId = actionId.substring((eventId + ":").length() + 1);
		} else {
			realId = actionId;
		}
		Map<String, Action> actions = getActions(eventId);
		if (null != actions) {
			return actions.get(realId);
		}
		return null;
	}

	public Map<String, Action> getActions(String eventId) {
		return actionMap.get(eventId);
	}

	public Datasource getDatasource(String id) {
		Datasource datasource = datasourceMap.get(id);
		initMetaData(datasource);
		return datasource;
	}

	private void initMetaData(Datasource datasource) {
		if (null != datasource) {
			MetaData metaData = datasource.getConfig().getMetaData();
			setBindings(metaData.getBinding(), metaData.getFields());
		}
	}

	private void setBindings(String binding, List<FieldDef> fields) {
		for (FieldDef fieldDef : fields) {
			if (null == fieldDef.getBinding()) {
				if (binding != null) {
					fieldDef.setBinding(binding + "." + fieldDef.getName());
				} else {
					fieldDef.setBinding(fieldDef.getName());
				}
			}
			setBindings(fieldDef.getBinding(), fieldDef.getFields());
		}
	}

	public String getDefaultPage() {
		return defaultPage;
	}

	public Event getEvent(String id) {
		return eventMap.get(id);
	}

	public Set<String> getEventIds() {
		return eventMap.keySet();
	}

	public PageDefinition getPage(String id) {
		return pageMap.get(id);
	}

	public Map<String, PageDefinition> getPages() {
		return pageMap;
	}

	public ApplicationRootConfig getApplicationRootConfig() {
		return rootConfig;
	}

	public Set<String> getSessionParams() {
		return sessionParams;
	}

	private boolean readConfig(String resourceName, Object object) throws InvalidConfigurationException {
		LOGGER.trace("reading {}, found {}", resourceName, object.getClass().getSimpleName());
		if (object instanceof ApplicationRootConfig) {
			if (null != rootConfig) {
				throw new InvalidConfigurationException(applicationName,
						"found second ApplicationRootConfig, this is a configuration error!");
			}
			rootConfig = (ApplicationRootConfig) object;
			List<Param> sessionParam = rootConfig.getConfig().getSession().getSessionParams().getSessionParam();
			for (Param param : sessionParam) {
				this.sessionParams.add(param.getName());
			}
			Pages pages = rootConfig.getPages();
			if (null != pages) {
				for (PageDefinition page : pages.getPageList()) {
					addPage(page, resourceName);
				}
			}
			addApplicationRootConfig(resourceName);
		} else if (object instanceof Pages) {
			Pages pages = (Pages) object;
			List<PageDefinition> pageList = pages.getPageList();
			for (PageDefinition page : pageList) {
				addPage(page, resourceName);
			}
		} else if (object instanceof PageDefinition) {
			PageDefinition page = (PageDefinition) object;
			addPage(page, resourceName);
		} else if (object instanceof Datasource) {
			Datasource ds = (Datasource) object;
			addDataSource(ds, resourceName);
		} else if (object instanceof Datasources) {
			Datasources sources = (Datasources) object;
			for (Datasource ds : sources.getDatasourceList()) {
				if (DatasourceInheritanceHelper.isInheriting(ds.getId())) {
					this.descendantDatasources.add(new Object[] { ds, resourceName });
				} else {
					addDataSource(ds, resourceName);
				}
			}
		} else if (object instanceof Event) {
			Event ev = (Event) object;
			addEvent(ev, resourceName);
		} else if (object instanceof Events) {
			Events events = (Events) object;
			for (Event ev : events.getEventList()) {
				addEvent(ev, resourceName);
			}
		} else if (object instanceof Platform) {
			return true;
		} else {
			LOGGER.error("ignoring unsupported type: {}", object.getClass());
		}
		return false;
	}

	public void setDefaultPage(String defaultPage) {
		this.defaultPage = defaultPage;
	}

	public void setSessionParams(Set<String> sessionParams) {
		this.sessionParams = sessionParams;
	}

	public String getResourceNameForEvent(String eventId) {
		return resourceMap.get(RESOURCE_MAP_KEY_EVENT + eventId);
	}

	public String getResourceNameForPage(String pageId) {
		return resourceMap.get(RESOURCE_MAP_KEY_PAGE + pageId);
	}

	public String getResourceNameForDataSource(String datasourceId) {
		return resourceMap.get(RESOURCE_MAP_KEY_DATASOURCE + datasourceId);
	}

	public String getResourceNameForApplicationRootConfig() {
		return resourceMap.get(RESOURCE_MAP_KEY_APPLICATION_ROOT_CONFIG);
	}

	static class PageMap extends HashMap<String, PageDefinition> {
	}

	static class ActionMap extends HashMap<String, Map<String, Action>> {
	}

	static class DataSourceMap extends HashMap<String, Datasource> {
	}

	static class EventMap extends HashMap<String, Event> {
	}

	public synchronized ApplicationConfigProvider cloneConfig(MarshallService marshallService)
			throws InvalidConfigurationException {
		try {
			ApplicationConfigProvider configProvider = null;
			if (devMode) {
				configProvider = new ApplicationConfigProviderImpl(marshallService, applicationName, xmlFiles,
						applicationInfo, resources, devMode);
			} else {
				configProvider = new ApplicationConfigProviderImpl(applicationName, devMode, ArrayUtils.clone(data));
			}
			return configProvider;
		} catch (Exception e) {
			throw new InvalidConfigurationException(applicationName, "error while reading data", e);
		}
	}

	public ApplicationInfo getApplicationInfo() {
		return applicationInfo;
	}

	public Resources getResources() {
		return resources;
	}

	public void close() throws IOException {
		for (Resource applicationResource : xmlFiles) {
			((Closeable) applicationResource).close();
		}
		actionMap.clear();
		eventMap.clear();
		datasourceMap.clear();
		pageMap.clear();
		resourceMap.clear();

		actionMap = null;
		eventMap = null;
		datasourceMap = null;
		pageMap = null;
		data = null;
		resourceMap = null;
		xmlFiles = null;
		if (null != resources) {
			resources.close();
			resources = null;
		}
	}

}
