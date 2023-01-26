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
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.domain.Template;
import org.appng.core.service.CoreService;
import org.appng.core.service.PlatformProperties;
import org.springframework.context.ApplicationContext;

/**
 * An event to be fired when the {@link Template} for a {@link Site} needs to be reloaded.
 * 
 * @author Matthias Müller
 */
public class ReloadTemplateEvent extends SiteEvent {

	public ReloadTemplateEvent(String siteName) {
		super(siteName);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		ApplicationContext platformContext = environment.getAttribute(Scope.PLATFORM,
				Platform.Environment.CORE_PLATFORM_CONTEXT);
		CoreService coreService = platformContext.getBean(CoreService.class);
		Properties platformConfig = environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		coreService.reloadTemplate(site, PlatformProperties.get(platformConfig));
	}
}
