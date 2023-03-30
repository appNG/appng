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

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.messaging.Event;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;

import lombok.Getter;
import lombok.Setter;

/**
 * An {@link Event} to be fired when a {@link Site}'s {@link SiteState} changes. Triggers a {@link NodeEvent}.
 * 
 * @author Matthias Müller
 */
public class SiteStateEvent extends Event {

	public static final String SITE_STATE = "siteState";

	private @Getter @Setter SiteState state;

	public SiteStateEvent(String siteName, SiteState state, String nodeId) {
		super(siteName);
		this.state = state;
		setNodeId(nodeId);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		handleSiteState(environment);
		new RequestNodeState(getSiteName(), getNodeId()).perform(environment, site);
	}

	public void handleSiteState(Environment environment) {
		Map<String, SiteState> siteState = NodeEvent.siteState(environment, getNodeId());
		if (SiteState.DELETED.equals(this.state)) {
			siteState.remove(getSiteName());
		} else {
			siteState.put(getSiteName(), this.state);
		}
	}

	@Override
	public String toString() {
		return super.toString() + " - State: " + state;
	}

}
