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

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.appng.api.Environment;
import org.appng.api.FileUpload.Unit;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.Messaging;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;

import lombok.Getter;
import lombok.Setter;

/**
 * An {@link Event} that holds information about the current status of an node (system properties and environment,memory
 * usage, state of sites).
 * 
 * @author Matthias Müller
 */
public class NodeEvent extends Event {

	public static final String NODE_STATE = "nodeState";
	private NodeState nodeState;

	public NodeEvent(Environment environment, String siteName) {
		this(environment, siteName, Messaging.getNodeId());
	}

	public NodeEvent(Environment environment, String siteName, String nodeId) {
		super(siteName);
		Map<String, SiteState> siteState = siteState(environment, nodeId);
		Map<String, Site> siteMap = environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		for (String site : siteMap.keySet()) {
			SiteState state = siteMap.get(site).getState();
			if (!(siteState.containsKey(site) || SiteState.DELETED.equals(state))) {
				siteState.put(site, state);
			}
		}
		this.nodeState = new NodeState(null, siteState);
		setNodeId(nodeId);
	}

	private NodeEvent() {

	}

	@Override
	protected void setNodeId(String nodeId) {
		super.setNodeId(nodeId);
		nodeState.setNodeId(nodeId);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		Map<String, NodeState> stateMap = clusterState(environment, getNodeId());
		stateMap.put(getNodeId(), this.nodeState);
	}

	public static Map<String, NodeState> clusterState(Environment environment, String nodeId) {
		Map<String, NodeState> clusterState = environment.getAttribute(Scope.PLATFORM, NODE_STATE);
		if (null == clusterState) {
			clusterState = new ConcurrentHashMap<>();
			environment.setAttribute(Scope.PLATFORM, NODE_STATE, clusterState);
		}
		if (!clusterState.containsKey(nodeId)) {
			clusterState.put(nodeId, new NodeEvent().new NodeState(nodeId, new HashMap<String, SiteState>()));
		}
		return clusterState;
	}

	public static Map<String, SiteState> siteState(Environment env, String nodeId) {
		Map<String, NodeState> clusterState = clusterState(env, nodeId);
		return clusterState.get(nodeId).getSiteStates();
	}

	@Getter
	@Setter
	public class MemoryUsage implements Serializable {
		private long size;
		private long max;
		private long used;
		private double usedPercent;

		public MemoryUsage(java.lang.management.MemoryUsage usage) {
			this.size = usage.getCommitted() / Unit.MB.getFactor();
			this.max = usage.getMax() / Unit.MB.getFactor();
			this.used = usage.getUsed() / Unit.MB.getFactor();
			this.usedPercent = (double) usage.getUsed() / (double) usage.getMax();
		}

	}

	@Getter
	@Setter
	public class NodeState implements Serializable {
		private String nodeId;
		private Date date;
		private MemoryUsage heap;
		private MemoryUsage nonHeap;
		private Properties props;
		private Map<String, String> env;
		private Map<String, SiteState> siteStates;

		NodeState(String nodeId, Map<String, SiteState> siteStates) {
			this.nodeId = nodeId;
			MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
			this.heap = new MemoryUsage(memoryMXBean.getHeapMemoryUsage());
			this.nonHeap = new MemoryUsage(memoryMXBean.getNonHeapMemoryUsage());
			this.props = new Properties();
			props.putAll(System.getProperties());
			this.env = new HashMap<>(System.getenv());
			this.siteStates = siteStates;
			this.date = new Date();
		}

	}

	@Override
	public String toString() {
		return String.format("%s (%s: %s)", getClass().getSimpleName(), getNodeId(), nodeState.getSiteStates());
	}
}
