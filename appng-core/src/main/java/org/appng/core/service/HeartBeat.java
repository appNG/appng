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
package org.appng.core.service;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.messaging.Messaging;
import org.appng.api.messaging.Sender;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.messaging.NodeEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Continuously sends {@link NodeEvent}s to the other cluster members, if messaging is enabled.
 */
@Slf4j
public class HeartBeat extends Thread {

	private final long heartBeatInterval;

	public HeartBeat(long heartBeatInterval) {
		super("appng-heartbeat");
		this.heartBeatInterval = heartBeatInterval;
	}

	@Override
	public void run() {
		DefaultEnvironment env = DefaultEnvironment.getGlobal();
		Sender sender = Messaging.getMessageSender(env);
		while (!isInterrupted()) {
			boolean sent = sender.send(new NodeEvent(env, StringUtils.EMPTY, Messaging.getNodeId()));
			if (!sent) {
				LOGGER.warn("NodeEvent could not be sent, please check messaging configuration.");
			}
			try {
				sleep(heartBeatInterval);
			} catch (InterruptedException e) {
				LOGGER.error("Thread was interrupted!");
				interrupt();
			}
		}
	}

}
