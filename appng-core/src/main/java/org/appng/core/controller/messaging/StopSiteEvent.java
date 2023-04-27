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

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.model.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopSiteEvent extends SiteEvent {

	private static final long serialVersionUID = 8053808333634879840L;

	public StopSiteEvent(String siteName) {
		super(siteName, false);
	}

	public StopSiteEvent(String siteName, String targetNode) {
		super(siteName, targetNode, false);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		Logger logger = LoggerFactory.getLogger(StopSiteEvent.class);
		if (isTargetNode(environment)) {
			logger.info("about to stop site: {}", getSiteName());
			getInitializerService(environment).shutDownSite(environment, site, false);
		} else {
			logIgnoreMessage(logger);
		}
	}

}
