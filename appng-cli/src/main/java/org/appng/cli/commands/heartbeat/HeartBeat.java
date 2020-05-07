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
package org.appng.cli.commands.heartbeat;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

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
import org.appng.api.model.Subject;
import org.appng.api.support.SiteClassLoader;
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
		Environment env = new PlatformEnv();
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
		Map<String, Site> siteMap = new HashMap<>();
		List<SiteImpl> sites = cle.getCoreService().getSites();
		for (SiteImpl site : sites) {
			site.setSiteClassLoader(new SiteClassLoader(site.getName()));
			siteMap.put(site.getName(), site);
		}
		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);

		Sender sender = Messaging.createMessageSender(env, executor, nodeId, loggingHandler,
				Arrays.asList(heartBeatHandler));
		LOGGER.debug("created {}", sender);
		while (!Thread.currentThread().isInterrupted()) {
			sender.send(new HeartBeatEvent(site));
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

	@SuppressWarnings("unchecked")
	class PlatformEnv implements Environment {
		private final Map<String, Object> attributes = new ConcurrentHashMap<>();

		private boolean isPlatformScope(Scope scope) {
			boolean isPlatformScope = Scope.PLATFORM.equals(scope);
			return isPlatformScope;
		}

		public void setAttribute(Scope scope, String name, Object value) {
			if (isPlatformScope(scope)) {
				attributes.put(name, value);
			}
		}

		public <T> T getAttribute(Scope scope, String name) {
			if (isPlatformScope(scope)) {
				return (T) attributes.get(name);
			}
			return null;
		}

		public <T> T removeAttribute(Scope scope, String name) {
			if (isPlatformScope(scope)) {
				return (T) attributes.remove(name);
			}
			return null;
		}

		public String getAttributeAsString(Scope scope, String name) {
			Object attrib = getAttribute(scope, name);
			if (attrib != null) {
				return attrib.toString();
			}
			return null;
		}

		public Set<String> keySet(Scope scope) {
			if (isPlatformScope(scope)) {
				return attributes.keySet();
			}
			return null;
		}

		public Subject getSubject() {
			return null;
		}

		public Locale getLocale() {
			return Locale.getDefault();
		}

		public TimeZone getTimeZone() {
			return TimeZone.getDefault();
		}

		public boolean isSubjectAuthenticated() {
			return false;
		}

		public void init(ServletContext context, HttpSession session, ServletRequest request, ServletResponse response,
				String host) {
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
}
