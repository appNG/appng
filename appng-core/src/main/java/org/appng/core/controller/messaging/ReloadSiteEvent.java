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

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.model.Site;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.core.service.CoreService;
import org.appng.core.service.InitializerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class ReloadSiteEvent extends Event {

	private static final long serialVersionUID = 8053808333634879840L;

	public ReloadSiteEvent(String siteName) {
		super(siteName);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		Logger logger = LoggerFactory.getLogger(ReloadSiteEvent.class);
		logger.info("about to reload site: {}", getSiteName());
		ApplicationContext platformContext = environment.getAttribute(Scope.PLATFORM,
				Platform.Environment.CORE_PLATFORM_CONTEXT);
		InitializerService initializerService = platformContext.getBean(InitializerService.class);
		CoreService coreService = platformContext.getBean(CoreService.class);
		FieldProcessorImpl fp = new FieldProcessorImpl("ReloadSiteEvent");
		initializerService.loadSite(environment, coreService.getSiteByName(getSiteName()), false, fp);
	}
}
