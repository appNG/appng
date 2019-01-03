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
package org.appng.api.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

import org.appng.api.Environment;
import org.appng.api.model.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Sender}, {@link Receiver} and {@link Serializer} for testing purposes.
 * 
 * @author Matthias Müller
 *
 */
public class TestReceiver implements Receiver, Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestReceiver.class);

	private ArrayBlockingQueue<Event> events = new ArrayBlockingQueue<>(10);
	private Serializer eventDeserializer;
	private List<Event> processed = new ArrayList<>();

	public void registerHandler(EventHandler<?> handler) {

	}

	public void setDefaultHandler(EventHandler<?> defaultHandler) {

	}

	public Receiver configure(Serializer eventDeserializer) {
		this.eventDeserializer = eventDeserializer;
		return this;
	}

	public Sender createSender() {
		return new TestSender().configure(eventDeserializer);
	}

	public void runWith(ExecutorService executorService) {
		executorService.submit(this);
	}

	@Override
	public void run() {
		while (true) {
			try {
				Event event = events.take();
				processed.add(event);
				LOGGER.info("received {}", event);
				Environment environment = eventDeserializer.getEnvironment();
				Site site = eventDeserializer.getSite(event.getSiteName());
				event.perform(environment, site);
			} catch (Throwable e) {
				LOGGER.error("error while retrieving events", e);
			}
		}

	}

	class TestSender implements Sender {
		private Serializer eventDeserializer;

		public Sender configure(Serializer eventDeserializer) {
			this.eventDeserializer = eventDeserializer;
			return this;
		}

		public boolean send(Event event) {
			try {
				eventDeserializer.serialize(new ByteArrayOutputStream(), event);
			} catch (IOException e) {
				LOGGER.error("error while sending event", e);
			}
			return events.offer(event);
		}
	}

	public List<Event> getProcessed() {
		return processed;
	}

	public static class TestSerializer extends Serializer {

		public TestSerializer(Environment environment, String nodeId) {
			super(environment, nodeId);
		}

	}
}