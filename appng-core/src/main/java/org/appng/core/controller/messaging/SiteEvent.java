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

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.core.service.InitializerService;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

abstract class SiteEvent extends Event {

	final String targetNode;
	String currentNode;

	SiteEvent(String siteName) {
		this(siteName, false);
	}

	SiteEvent(String siteName, boolean async) {
		this(siteName, null, async);
	}

	SiteEvent(String siteName, String targetNode, boolean async) {
		super(siteName, async);
		this.targetNode = targetNode;
	}

	protected boolean isTargetNode(Environment environment) {
		this.currentNode = org.appng.api.messaging.Messaging.getNodeId();
		return StringUtils.isBlank(targetNode) || currentNode.equals(targetNode);
	}

	protected void logIgnoreMessage(Logger logger) {
		logger.debug("Ignoring event for node {}, current is {}", targetNode, currentNode);
	}

	protected InitializerService getInitializerService(Environment environment) {
		return getPlatformContext(environment).getBean(InitializerService.class);
	}

	protected ApplicationContext getPlatformContext(Environment environment) {
		return environment.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
	}

}
