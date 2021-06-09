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
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Sender;
import org.appng.api.model.Site;

/**
 * An {@link Event} that triggers the {@link Receiver} to notify about the current status of the node by sending a
 * {@link NodeEvent}.
 * 
 * @author Matthias Müller
 */
public class RequestNodeState extends Event {

	public RequestNodeState(String siteName) {
		super(siteName);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		Sender sender = environment.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER);
		sender.send(new NodeEvent(environment, getSiteName()));
	}

}
