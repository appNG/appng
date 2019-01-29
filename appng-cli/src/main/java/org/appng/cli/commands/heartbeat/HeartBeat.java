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
package org.appng.cli.commands.heartbeat;

import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.Messaging;
import org.appng.api.messaging.Sender;
import org.appng.api.model.Site;
import org.appng.api.support.SiteClassLoader;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.cli.CliEnvironment;
import org.appng.cli.ExecutableCliCommand;
import org.appng.core.domain.SiteImpl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Sends and receives a heartbeat using the configured messaging settings.<br/>
 * 
 * <pre>
 * Usage: heartbeat [options]
 *   Options:
 *     -i
 *       The interval of the heartbeat in milliseconds.
 *       Default: 1000
 *     -n
 *       The node id for the events to be send.
 *       Default: System.getProperty("appng.node.id") 
 *     -s
 *       The site name for the heartbeat event.
 *       Default: appng
 * </pre>
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
@Parameters(commandDescription = "Sends and receives a heartbeat using the configured messaging settings.")
public class HeartBeat implements ExecutableCliCommand {

	@Parameter(names = "-i", description = "The interval of the heartbeat in milliseconds.")
	private int interval = 1000;

	@Parameter(names = "-s", description = "The site name for the heartbeat event.")
	private String site = "appng";

	@Parameter(names = "-n", description = "The node id for the events to be send. Default: System.getProperty(\"appng.node.id\")")
	private String nodeId = System.getProperty(Messaging.APPNG_NODE_ID);

	public void execute(CliEnvironment cle) throws BusinessException {
		ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
		ThreadFactory threadFactory = tfb.setDaemon(true).setNameFormat("appng-heartbeat").build();
		ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
		if (null == nodeId) {
			try {
				nodeId = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				nodeId = "appng-heartbeat";
			}
		}
		Environment env = DefaultEnvironment.get(new DummyContext());
		EventHandler<HeartBeatEvent> heartBeatHandler = new EventHandler<HeartBeatEvent>() {
			public void onEvent(HeartBeatEvent event, Environment environment, Site site)
					throws InvalidConfigurationException, BusinessException {
				LOGGER.info("received {}", event);
				event.perform(environment, site);
			}

			public Class<HeartBeatEvent> getEventClass() {
				return HeartBeatEvent.class;
			}
		};

		EventHandler<Event> loggingHandler = new EventHandler<Event>() {
			public void onEvent(Event event, Environment environment, Site site)
					throws InvalidConfigurationException, BusinessException {
				LOGGER.info("received {}", event);
			}

			public Class<Event> getEventClass() {
				return Event.class;
			}
		};
		env.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG, cle.getPlatformConfig());
		Map<String, Site> siteMap = new HashMap<String, Site>();
		List<SiteImpl> sites = cle.getCoreService().getSites();
		for (SiteImpl site : sites) {
			site.setSiteClassLoader(new SiteClassLoader(site.getName()));
			siteMap.put(site.getName(), site);
		}
		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);

		Sender sender = Messaging.createMessageSender(env, executor, nodeId, loggingHandler, Arrays.asList(heartBeatHandler));
		LOGGER.debug("created {}", sender);
		while (true) {
			sender.send(new HeartBeatEvent(site));
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
			}
		}

	}

	public static class HeartBeatEvent extends Event implements Serializable {

		public HeartBeatEvent(String site) {
			super(site);
		}

		public void perform(Environment environment, Site site)
				throws InvalidConfigurationException, BusinessException {
			LOGGER.info("still beating!");
		}

	}

	class DummyContext implements ServletContext {

		Map<String, Object> attributes = new HashMap<>();

		public Object getAttribute(String name) {
			return attributes.get(name);
		}

		public void setAttribute(String name, Object object) {
			attributes.put(name, object);
		}

		public void removeAttribute(String name) {
			attributes.remove(name);
		}

		// ### NOT NEEDED
		public Enumeration<String> getAttributeNames() {
			return null;
		}

		public String getContextPath() {
			return null;
		}

		public ServletContext getContext(String uripath) {
			return null;
		}

		public int getMajorVersion() {
			return 0;
		}

		public int getMinorVersion() {
			return 0;
		}

		public int getEffectiveMajorVersion() {
			return 0;
		}

		public int getEffectiveMinorVersion() {
			return 0;
		}

		public String getMimeType(String file) {
			return null;
		}

		public Set<String> getResourcePaths(String path) {
			return null;
		}

		public URL getResource(String path) throws MalformedURLException {
			return null;
		}

		public InputStream getResourceAsStream(String path) {
			return null;
		}

		public RequestDispatcher getRequestDispatcher(String path) {
			return null;
		}

		public RequestDispatcher getNamedDispatcher(String name) {
			return null;
		}

		public Servlet getServlet(String name) throws ServletException {
			return null;
		}

		public Enumeration<Servlet> getServlets() {
			return null;
		}

		public Enumeration<String> getServletNames() {
			return null;
		}

		public void log(String msg) {
		}

		public void log(Exception exception, String msg) {
		}

		public void log(String message, Throwable throwable) {
		}

		public String getRealPath(String path) {
			return null;
		}

		public String getServerInfo() {
			return null;
		}

		public String getInitParameter(String name) {
			return null;
		}

		public Enumeration<String> getInitParameterNames() {
			return null;
		}

		public boolean setInitParameter(String name, String value) {
			return false;
		}

		public String getServletContextName() {
			return null;
		}

		public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, String className) {
			return null;
		}

		public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
			return null;
		}

		public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName,
				Class<? extends Servlet> servletClass) {

			return null;
		}

		public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
			return null;
		}

		public ServletRegistration getServletRegistration(String servletName) {
			return null;
		}

		public Map<String, ? extends ServletRegistration> getServletRegistrations() {
			return null;
		}

		public Dynamic addFilter(String filterName, String className) {
			return null;
		}

		public Dynamic addFilter(String filterName, Filter filter) {
			return null;
		}

		public Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
			return null;
		}

		public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
			return null;
		}

		public FilterRegistration getFilterRegistration(String filterName) {
			return null;
		}

		public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
			return null;
		}

		public SessionCookieConfig getSessionCookieConfig() {
			return null;
		}

		public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
		}

		public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
			return null;
		}

		public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
			return null;
		}

		public void addListener(String className) {
		}

		public <T extends EventListener> void addListener(T t) {
		}

		public void addListener(Class<? extends EventListener> listenerClass) {
		}

		public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
			return null;
		}

		public JspConfigDescriptor getJspConfigDescriptor() {
			return null;
		}

		public ClassLoader getClassLoader() {
			return null;
		}

		public void declareRoles(String... roleNames) {
		}

		public String getVirtualServerName() {
			return null;
		}

	}
}
