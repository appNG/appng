/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.api.messaging;

import java.io.Serializable;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.model.Site;

/**
 * An event can be send by a {@link Sender} to multiple {@link Receiver}s.
 * 
 * @author Matthias Müller
 * 
 * @see Sender
 * @see Receiver
 * @see Site#sendEvent(Event)
 */
public abstract class Event implements Serializable {

	private final String siteName;
	private String nodeId;

	protected Event() {
		this(null);
	}

	/**
	 * Creates a new event
	 * 
	 * @param siteName
	 *            the name of the {@link Site} this event is for
	 */
	protected Event(String siteName) {
		this.siteName = siteName;
	}

	/**
	 * Returns the node id of this event, i.e. the node id of the origin node. The node id is automatically set by the
	 * {@link Serializer} when serializing the event.
	 * 
	 * @return the node id
	 */
	public String getNodeId() {
		return nodeId;
	}

	protected void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Returns the name of the origin {@link Site} this event was created for
	 * 
	 * @return the name of the {@link Site}
	 */
	public String getSiteName() {
		return siteName;
	}

	/**
	 * Performs the event
	 * 
	 * @param environment
	 *            then {@link Environment} to use
	 * @param site
	 *            the {@link Site} where the event occurred
	 * @throws InvalidConfigurationException
	 *             if there's a configuration error
	 * @throws BusinessException
	 *             if an error occurs while performing the event
	 */
	public abstract void perform(Environment environment, Site site)
			throws InvalidConfigurationException, BusinessException;

	@Override
	public String toString() {
		return getClass().getName() + " - Origin: " + getNodeId() + " - Site: " + getSiteName();
	}

}
