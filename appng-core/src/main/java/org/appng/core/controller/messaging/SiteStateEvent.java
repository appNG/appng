/*
 * Copyright 2011-2018 the original author or authors.
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
import java.util.concurrent.ConcurrentHashMap;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;

/**
 * An {@link Event} to be fired when a {@link Site}'s {@link SiteState} changes. Triggers a {@link NodeEvent}.
 * 
 * @author Matthias Müller
 */
public class SiteStateEvent extends Event {

	public static final String SITE_STATE = "siteState";

	private SiteState state;

	public SiteStateEvent(String siteName, SiteState state) {
		super(siteName);
		this.state = state;
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		Map<String, SiteState> stateMap = getStateMap(environment); 
		stateMap.put(getSiteName(), this.state);
		new RequestNodeState(getSiteName()).perform(environment, site);
	}

	public SiteState getState() {
		return state;
	}

	public void setState(SiteState state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return super.toString() + " - State: " + state;
	}
	
	static Map<String, SiteState> getStateMap(Environment env) {
		Map<String, SiteState> stateMap = env.getAttribute(Scope.PLATFORM, SITE_STATE);
		if (null == stateMap) {
			stateMap = new ConcurrentHashMap<String, SiteState>();
			env.setAttribute(Scope.PLATFORM, SITE_STATE, stateMap);
		}
		return stateMap;
	}

}
