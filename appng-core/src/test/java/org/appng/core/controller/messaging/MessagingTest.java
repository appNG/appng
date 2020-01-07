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
import java.util.List;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class MessagingTest {

	private static final String APPNG_NODE1 = "appng.node1";
	private static final String APPNG_NODE2 = "appng.node2";
	private static final String LOCALHOST = "localhost";

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
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getSiteClassLoader()).thenReturn(new URLClassLoader(new URL[0]));
		Mockito.when(site.getName()).thenReturn(LOCALHOST);
		Mockito.when(site.getHost()).thenReturn(LOCALHOST);
		Mockito.when(site.getDomain()).thenReturn(LOCALHOST);
		Mockito.when(site.getProperties()).thenReturn(Mockito.mock(Properties.class));

		try (MulticastReceiver receiver = new MulticastReceiver("224.2.2.4", 4000)) {
			Serializer serializer = Mockito.mock(Serializer.class);
			Mockito.when(serializer.getEnvironment()).thenReturn(Mockito.mock(Environment.class));
			Mockito.when(serializer.getNodeId()).thenReturn(APPNG_NODE1);
			Mockito.when(serializer.getPlatformConfig()).thenReturn(Mockito.mock(Properties.class));
			final List<TestEvent> processedEvents = new ArrayList<>();
			Mockito.doAnswer(invocation -> {
				Event event = deserialize(invocation.getArgumentAt(0, byte[].class));
				processedEvents.add((TestEvent) event);
				return event;
			}).when(serializer).deserialize(Mockito.any(byte[].class));
			receiver.configure(serializer);
			List<String> nodeList = Arrays.asList("127.0.0.1", "127.0.0.2");

			// IP not in list -> event not even deserialized
			receiver.onEvent(serialize(new TestEvent(APPNG_NODE1)), nodeList, "127.0.0.3");
			Assert.assertTrue(processedEvents.isEmpty());

			// same address, same node -> not processed!
			receiver.onEvent(serialize(new TestEvent(APPNG_NODE1)), nodeList, "127.0.0.1");
			Assert.assertFalse(processedEvents.get(0).processed);

			// same address, different node -> processed!
			receiver.onEvent(serialize(new TestEvent(APPNG_NODE2)), nodeList, "127.0.0.1");
			Assert.assertTrue(processedEvents.get(1).processed);

			// different address, same node -> processed!
			receiver.onEvent(serialize(new TestEvent(APPNG_NODE1)), nodeList, "127.0.0.2");
			Assert.assertTrue(processedEvents.get(2).processed);

			// different address, different node -> processed!
			receiver.onEvent(serialize(new TestEvent(APPNG_NODE2)), nodeList, "127.0.0.1");
			Assert.assertTrue(processedEvents.get(3).processed);

		}
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
