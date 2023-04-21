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
package org.appng.core.controller.messaging;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Messaging;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.core.controller.messaging.NodeEvent.NodeState;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.CoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ReloadSiteEvent extends SiteEvent {

	private static final long serialVersionUID = 8053808333634879840L;

	public ReloadSiteEvent(String siteName) {
		super(siteName);
	}

	public ReloadSiteEvent(String siteName, String targetNode) {
		super(siteName, targetNode);
	}

	public void perform(Environment env, Site site) throws InvalidConfigurationException {
		Logger logger = LoggerFactory.getLogger(ReloadSiteEvent.class);
		if (isTargetNode(env)) {
			logger.info("about to start site: {}", getSiteName());
			FieldProcessor fp = new FieldProcessorImpl("start");

			ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
					.setNameFormat(String.format("siteloader-%s-wait", site.getName())).build());
			executor.submit(() -> wait(env, site, logger));
			executor.shutdown();

			SiteImpl siteByName = getPlatformContext(env).getBean(CoreService.class).getSiteByName(getSiteName());
			getInitializerService(env).loadSite(env, siteByName, false, fp);
		} else {
			logIgnoreMessage(logger);
		}
	}

	public void wait(Environment env, Site site, Logger logger) {
		if (delayed()) {
			Properties nodeCfg = env.getAttribute(Scope.PLATFORM, Platform.Environment.NODE_CONFIG);
			long delayMillis = nodeCfg.getInteger(Platform.Property.SITE_RELOAD_DELAY, 0);
			if (delayMillis <= 0) {
				Properties cfg = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
				Integer siteReloadMaxDelay = cfg.getInteger(Platform.Property.SITE_RELOAD_MAX_RANDOM_DELAY, 10000);
				delayMillis = (long) (Math.random() * siteReloadMaxDelay);
			}
			try {
				logger.info("Waiting {}ms before reloading site {} on node {}", delayMillis, site.getName(),
						Messaging.getNodeId());
				Thread.sleep(delayMillis);
			} catch (InterruptedException e) {
				//
			}
		}
		waitForClusterState(env, site, logger);
	}

	public void waitForClusterState(Environment env, Site site, Logger logger) {
		Properties cfg = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		if (cfg.getBoolean("waitForSitesStarted", false)) {
			String nodeId = Messaging.getNodeId();
			Map<String, NodeState> nodeStates = NodeEvent.clusterState(env, nodeId);
			int numNodes = nodeStates.size();
			int minActiveNodes = (numNodes + 1) / 2;
			int waited = 0;
			int waitTime = cfg.getInteger("waitForSitesStartedWaitTime", 5);
			int maxWaittime = cfg.getInteger("waitForSitesStartedMaxWaitTime", 30);
			int activeNodes = 0;

			do {
				for (Entry<String, NodeState> state : nodeStates.entrySet()) {
					String otherNode = state.getKey();
					if (!nodeId.equals(otherNode)) {
						SiteState siteState = state.getValue().getSiteStates().get(site.getName());
						if (SiteState.STARTED.equals(siteState)) {
							activeNodes++;
						}
						logger.debug("Site {} is {} on node {}", site.getName(), siteState, otherNode);
					}
				}
				if (activeNodes < minActiveNodes) {
					try {
						logger.debug("Site {} is active on {} of {} nodes, waiting {}s for site to start on {} nodes.",
								site.getName(), activeNodes, numNodes, waitTime, minActiveNodes - activeNodes);
						waited += waitTime;
						Thread.sleep(TimeUnit.SECONDS.toMillis(waitTime));
					} catch (InterruptedException e) {
						//
					}
				}
			} while (activeNodes < minActiveNodes && waited < maxWaittime);
			if (waited >= maxWaittime) {
				logger.info("Reached maximum waiting time of {}s, now reloading site {}.", maxWaittime, site.getName());
			} else {
				logger.info("Site {} is active on {} of {} nodes, reloading now.", site.getName(), activeNodes,
						numNodes);
			}
		}
	}

	protected boolean delayed() {
		return true;
	}
}
