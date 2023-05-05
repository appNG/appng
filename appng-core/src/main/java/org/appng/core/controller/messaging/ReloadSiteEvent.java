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

public class ReloadSiteEvent extends SiteEvent {

	private static final long serialVersionUID = 8053808333634879840L;
	private static final int DEFAULT_MAX_RELOAD_DELAY = (int) TimeUnit.SECONDS.toMillis(6);

	public ReloadSiteEvent(String siteName) {
		super(siteName, true);
	}

	public ReloadSiteEvent(String siteName, String targetNode) {
		super(siteName, targetNode, true);
	}

	public void perform(Environment env, Site site) throws InvalidConfigurationException {
		Logger logger = LoggerFactory.getLogger(ReloadSiteEvent.class);
		if (isTargetNode(env)) {
			logger.info("about to start site: {}", getSiteName());
			FieldProcessor fp = new FieldProcessorImpl("start");
			waitForClusterState(env, site, logger);
			SiteImpl siteByName = getPlatformContext(env).getBean(CoreService.class).getSiteByName(getSiteName());
			getInitializerService(env).loadSite(env, siteByName, false, fp, false);
		} else {
			logIgnoreMessage(logger);
		}
	}

	public void waitForClusterState(Environment env, Site site, Logger logger) {
		Properties cfg = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		Integer maxReloadDelay = cfg.getInteger(Platform.Property.SITE_RELOAD_MAX_RANDOM_DELAY,
				DEFAULT_MAX_RELOAD_DELAY);
		long delayMillis = maxReloadDelay + (long) (Math.random() * maxReloadDelay);
		try {
			logger.info("Waiting {}ms before reloading site {} on node {}", delayMillis, site.getName(),
					Messaging.getNodeId());
			Thread.sleep(delayMillis);
		} catch (InterruptedException e) {
			//
		}
		if (cfg.getBoolean("waitForSitesStarted", true)) {
			String nodeId = Messaging.getNodeId();
			Map<String, NodeState> nodeStates = NodeEvent.clusterState(env, nodeId);
			int numNodes = nodeStates.size();
			int minActiveNodes = numNodes > 3 ? (numNodes + 1) / 2 : numNodes;
			int waited = 0;
			int waitTime = cfg.getInteger("waitForSitesStartedWaitTime", 5);
			int maxWaittime = cfg.getInteger("waitForSitesStartedMaxWaitTime", 30);
			int activeNodes;

			logger.info("Site {} must be {} on {} of {} nodes before reloading.", site.getName(), SiteState.STARTED,
					minActiveNodes, numNodes);

			do {
				activeNodes = 0;
				for (Entry<String, NodeState> state : nodeStates.entrySet()) {
					String otherNode = state.getKey();
					SiteState siteState = state.getValue().getSiteStates().get(site.getName());
					if (SiteState.STARTED.equals(siteState)) {
						activeNodes++;
					}
					logger.debug("Site {} is {} on node {}", site.getName(), siteState, otherNode);
				}
				if (activeNodes < minActiveNodes) {
					try {
						logger.info("Site {} is active on {} of {} nodes, waiting {}s for site to start on {} nodes.",
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

}
