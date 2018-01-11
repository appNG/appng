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
package org.appng.api.messaging;

/**
 * Implementations are able to send {@link Event}s to multiple {@link Receiver}s.
 * 
 * @author Matthias Müller
 * 
 * @see Event
 * @see Receiver
 */
public interface Sender {

	/**
	 * Configures this sender with the given {@link Serializer}
	 * 
	 * @param eventDeserializer
	 *            the {@link Serializer} to use
	 * @return the sender
	 */
	Sender configure(Serializer eventDeserializer);

	/**
	 * Sends the given {@link Event}
	 * 
	 * @param event
	 *            the {@link Event} so send
	 * @return whether or not sending the event was successful
	 */
	boolean send(Event event);

}
