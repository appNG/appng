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
package org.appng.core.controller.messaging;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.EventRegistry;
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Site;
import org.slf4j.Logger;

/**
 * Base class for {@link Receiver}s and {@link Sender}s.
 * 
 * @author Matthias Müller
 *
 */
abstract class Messaging {

	protected Serializer serializer;

	Messaging() {
	}

	protected void handleMessage(byte[] body, EventRegistry eventRegistry) {
		Event event = serializer.deserialize(body);
		if (null != event) {
			try {
				Site site = serializer.getSite(event.getSiteName());
				String currentNode = serializer.getNodeId();
				String originNode = event.getNodeId();
				logger().debug("current node: {}, originNode node: {}", currentNode, originNode);
				boolean sameNode = StringUtils.equals(currentNode, originNode);
				if (!sameNode) {
					logger().info("about to execute {} ", event);
					for (EventHandler<Event> eventHandler : eventRegistry.getHandlers(event)) {
						eventHandler.onEvent(event, serializer.getEnvironment(), site);
					}
				} else {
					logger().debug("message is from myself and can be ignored");
				}

			} catch (Exception e) {
				logger().error(String.format("Error while executing event %s", event), e);
			}
		} else {
			logger().debug("could not read event");
		}
	}

	abstract Logger logger();

}