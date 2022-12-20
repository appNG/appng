/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.concurrent.TimeUnit;

import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.CoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			SiteImpl siteByName = getPlatformContext(env).getBean(CoreService.class).getSiteByName(getSiteName());
			FieldProcessor fp = new FieldProcessorImpl("start");
			if (delayed()) {
				Properties nodeConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.NODE_CONFIG);
				Integer delay = nodeConfig.getInteger(Platform.Property.SITE_RELOAD_DELAY, 0);
				if (delay > 0) {
					logger.info("Waiting %s before reloading site %s on node %s", delay, siteByName.getName(),
							org.appng.api.messaging.Messaging.getNodeId(env));
					try {
						Thread.sleep(TimeUnit.SECONDS.toMillis(delay));
					} catch (InterruptedException e) {
						//
					}
				}
			}
			getInitializerService(env).loadSite(siteByName, env, false, fp, false);
		} else {
			logIgnoreMessage(logger);
		}
	}

	protected boolean delayed() {
		return true;
	}
}
