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
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;

/**
 * An {@link Event} that holds informations about the current status of an node (system properties and
 * environment,memory usage, state of sites).
 * 
 * @author Matthias Müller
 *
 */
public class NodeEvent extends Event {

	public static final String NODE_STATE = "nodeState";
	private NodeState nodeState;

	public NodeEvent(Environment environment, String siteName) {
		super(siteName);
		Map<String, SiteState> stateMap = SiteStateEvent.getStateMap(environment);
		Map<String, Site> siteMap = environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		for (String site : siteMap.keySet()) {
			if (!stateMap.containsKey(site)) {
				stateMap.put(site, SiteState.STARTED);
			}
		}
		this.nodeState = new NodeState(getNodeId(), stateMap);
	}

	@Override
	protected void setNodeId(String nodeId) {
		super.setNodeId(nodeId);
		nodeState.setNodeId(nodeId);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		Map<String, NodeState> stateMap = environment.getAttribute(Scope.PLATFORM, NODE_STATE);
		if (null == stateMap) {
			stateMap = new ConcurrentHashMap<>();
			environment.setAttribute(Scope.PLATFORM, NODE_STATE, stateMap);
		}
		stateMap.put(getNodeId(), this.nodeState);
	}

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

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}

		public long getMax() {
			return max;
		}

		public void setMax(long max) {
			this.max = max;
		}

		public long getUsed() {
			return used;
		}

		public void setUsed(long used) {
			this.used = used;
		}

		public double getUsedPercent() {
			return usedPercent;
		}

		public void setUsedPercent(double usedPercent) {
			this.usedPercent = usedPercent;
		}

	}

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

		public String getNodeId() {
			return nodeId;
		}

		public void setNodeId(String nodeId) {
			this.nodeId = nodeId;
		}

		public MemoryUsage getHeap() {
			return heap;
		}

		public void setHeap(MemoryUsage heap) {
			this.heap = heap;
		}

		public MemoryUsage getNonHeap() {
			return nonHeap;
		}

		public void setNonHeap(MemoryUsage nonHeap) {
			this.nonHeap = nonHeap;
		}

		public Properties getProps() {
			return props;
		}

		public void setProps(Properties props) {
			this.props = props;
		}

		public Map<String, String> getEnv() {
			return env;
		}

		public void setEnv(Map<String, String> env) {
			this.env = env;
		}

		public Map<String, SiteState> getSiteStates() {
			return siteStates;
		}

		public void setSiteStates(Map<String, SiteState> siteStates) {
			this.siteStates = siteStates;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

	}
}
