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
package org.appng.api.messaging;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * Implementations are able to receive {@link Event}s from multiple {@link Sender}s.
 * 
 * @author Matthias Müller
 * 
 * @see Event
 * @see Sender
 */
public interface Receiver extends Closeable {

	/**
	 * Registers a new {@link EventHandler}
	 * 
	 * @param handler
	 *                the {@link EventHandler} to register
	 */
	void registerHandler(EventHandler<?> handler);

	/**
	 * Sets the default {@link EventHandler}, used when no other handler is registered for a certain event type
	 * 
	 * @param defaultHandler
	 *                       the default {@link EventHandler} to use
	 */
	void setDefaultHandler(EventHandler<?> defaultHandler);

	/**
	 * Configures the receiver
	 * 
	 * @param eventDeserializer
	 *                          the {@link Serializer} for this receiver
	 * 
	 * @return the configured receiver
	 */
	Receiver configure(Serializer eventDeserializer);

	/**
	 * Creates and returns a {@link Sender} capable of sending {@link Event}s that can be received by this type of
	 * receiver.
	 * 
	 * @return the {@link Sender}
	 */
	Sender createSender();

	/**
	 * Since a receiver runs as a thread, the given {@link ExecutorService} should be used to run this thread.
	 * 
	 * @param executorService
	 *                        the {@link ExecutorService} to run this receiver with
	 */
	void runWith(ExecutorService executorService);

	/**
	 * Closes the receiver, in particular blocking I/O resources need to be closed here.
	 */
	default void close() throws IOException {

	}

}
