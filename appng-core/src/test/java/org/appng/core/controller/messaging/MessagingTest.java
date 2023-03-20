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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.messaging.NodeEvent.NodeState;
import org.appng.core.domain.SiteImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockServletContext;

public class MessagingTest {

	private static final String APPNG_NODE1 = "appng.node1";
	private static final String APPNG_NODE2 = "appng.node2";
	private static final String LOCALHOST = "localhost";

	private Sender sender;
	private MulticastReceiver receiver;
	private final List<Event> processedEvents = new ArrayList<>();
	private final List<String> nodeList = Arrays.asList("127.0.0.1", "127.0.0.2");

	static class TestEvent extends Event {
		private String nodeIdInternal;
		protected volatile boolean processed = false;

		protected TestEvent(String nodeId) {
			super(LOCALHOST);
			setNodeId(nodeId);
		}

		public void perform(Environment environment, Site site)
				throws InvalidConfigurationException, BusinessException {
			processed = true;
		}

		@Override
		public String getNodeId() {
			return nodeIdInternal;
		}

		public void setNodeId(String nodeId) {
			this.nodeIdInternal = nodeId;
		}

	}

	@Test
	public void test() throws IOException, InterruptedException {
		prepare();

		// IP not in list -> event not even deserialized
		receiver.onEvent(serialize(new TestEvent(APPNG_NODE1)), nodeList, "127.0.0.3");
		Assert.assertTrue(processedEvents.isEmpty());

		// same address, same node -> not processed!
		receiver.onEvent(serialize(new TestEvent(APPNG_NODE1)), nodeList, "127.0.0.1");
		Assert.assertFalse(((TestEvent) processedEvents.get(0)).processed);

		// same address, different node -> processed!
		receiver.onEvent(serialize(new TestEvent(APPNG_NODE2)), nodeList, "127.0.0.1");
		Assert.assertTrue(((TestEvent) processedEvents.get(1)).processed);

		// different address, same node -> processed!
		receiver.onEvent(serialize(new TestEvent(APPNG_NODE1)), nodeList, "127.0.0.2");
		Assert.assertTrue(((TestEvent) processedEvents.get(2)).processed);

		// different address, different node -> processed!
		receiver.onEvent(serialize(new TestEvent(APPNG_NODE2)), nodeList, "127.0.0.1");
		Assert.assertTrue(((TestEvent) processedEvents.get(3)).processed);

	}

	@Test
	public void testClusterState() throws IOException, InterruptedException {
		Environment env = prepare();
		Map<String, Site> siteMap = new HashMap<String, Site>();
		SiteImpl siteA = new SiteImpl();
		siteA.setName("siteA");
		siteA.setSender(sender);
		SiteImpl siteB = new SiteImpl();
		siteB.setName("siteB");
		siteMap.put(siteA.getName(), siteA);
		siteMap.put(siteB.getName(), siteB);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.SITES, siteMap);

		SiteStateEvent siteAState = new SiteStateEvent(siteA.getName(), SiteState.STARTED, APPNG_NODE1);
		receiver.onEvent(serialize(siteAState), nodeList, "127.0.0.2");

		Map<String, NodeState> nodeStates = NodeEvent.clusterState(env, APPNG_NODE1);
		Assert.assertEquals(1, nodeStates.size());
		NodeState currentNodeState = nodeStates.get(APPNG_NODE1);
		Map<String, SiteState> siteStates = currentNodeState.getSiteStates();
		Assert.assertEquals(SiteState.STARTED, siteStates.get(siteA.getName()));

		SiteStateEvent siteBState = new SiteStateEvent(siteB.getName(), SiteState.STARTING, APPNG_NODE1);
		receiver.onEvent(serialize(siteBState), nodeList, "127.0.0.2");
		Assert.assertEquals(SiteState.STARTING, siteStates.get(siteB.getName()));
		siteBState = new SiteStateEvent(siteB.getName(), SiteState.STARTING, APPNG_NODE2);
		receiver.onEvent(serialize(siteBState), nodeList, "127.0.0.2");
		Assert.assertEquals(2, nodeStates.size());
	}

	private Environment prepare() {
		processedEvents.clear();
		DefaultEnvironment env = DefaultEnvironment.get(new MockServletContext());
		receiver = new MulticastReceiver("224.2.2.4", 4000);
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getSiteClassLoader()).thenReturn(new URLClassLoader(new URL[0]));
		Mockito.when(site.getName()).thenReturn(LOCALHOST);
		Mockito.when(site.getHost()).thenReturn(LOCALHOST);
		Mockito.when(site.getDomain()).thenReturn(LOCALHOST);
		Mockito.when(site.getProperties()).thenReturn(Mockito.mock(Properties.class));
		Serializer serializer = Mockito.mock(Serializer.class);
		Mockito.when(serializer.getEnvironment()).thenReturn(env);
		Mockito.when(serializer.getNodeId()).thenReturn(APPNG_NODE1);
		Mockito.when(serializer.getPlatformConfig()).thenReturn(Mockito.mock(Properties.class));
		Mockito.doAnswer(invocation -> {
			Event event = deserialize(invocation.getArgumentAt(0, byte[].class));
			processedEvents.add(event);
			return event;
		}).when(serializer).deserialize(Mockito.any(byte[].class));
		receiver.configure(serializer);
		sender = Mockito.mock(Sender.class);
		env.setAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER, sender);
		return env;
	}

	private byte[] serialize(Event event) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(out)) {
			oos.writeObject(event.getSiteName());
			oos.writeObject(event);
			oos.flush();
			out.flush();
			return out.toByteArray();
		}
	}

	private Event deserialize(byte[] data) throws IOException, ClassNotFoundException {
		try (ObjectInputStream oos = new ObjectInputStream(new ByteArrayInputStream(data))) {
			oos.readObject();
			Event event = (Event) oos.readObject();
			return event;
		}
	}

}
