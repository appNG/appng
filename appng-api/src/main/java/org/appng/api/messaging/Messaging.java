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
package org.appng.api.messaging;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;

import lombok.extern.slf4j.Slf4j;

/**
 * A utility class offering static helper methods to create and retrieve a {@link Sender} and to shutdown
 * {@link Sender}s and {@link Receiver}s.
 * 
 * @author Matthias Müller
 *
 * @see    Sender
 * @see    Receiver
 */
@Slf4j
public class Messaging {

	/**
	 * Name of a system property used to identify the node
	 */
	public static final String APPNG_NODE_ID = "appng.node.id";

	/**
	 * Retrieves the previously created {@link Sender} from the {@link Environment}.
	 * 
	 * @param  env
	 *             the {@link Environment} to use
	 * 
	 * @return     the {@link Sender}, if available
	 * 
	 * @see        #createMessageSender(Environment, ExecutorService)
	 * @see        #createMessageSender(Environment, ExecutorService, String, EventHandler, Iterable)
	 */
	public static Sender getMessageSender(Environment env) {
		return env.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER);
	}

	/**
	 * Creates and returns a new {@link Sender} and a corresponding {@link Receiver}. The class name for the receiver is
	 * taken from the platform property {@value org.appng.api.Platform.Property#MESSAGING_RECEIVER}. Uses the system
	 * property {@value #APPNG_NODE_ID} to retrieve the node id for the {@link Serializer}. If this property is absent,
	 * the local host name is used and the property is set.
	 * 
	 * @param  env
	 *                  the {@link Environment} to use
	 * @param  executor
	 *                  the {@link ExecutorService} to run the {@link Receiver} with
	 * 
	 * @return
	 *                  <ul>
	 *                  <li>the {@link Sender} (if the platform property
	 *                  {@value org.appng.api.Platform.Property#MESSAGING_ENABLED} is {@code true}
	 *                  <li>{@code null} if messaging is disabled or an error occurred while creating the sender
	 *                  </ul>
	 */
	public static Sender createMessageSender(Environment env, ExecutorService executor) {
		return createMessageSender(env, executor, getNodeId(), null, null);
	}

	/**
	 * Determines and returns the node id for this node. If the system property {@value #APPNG_NODE_ID} is set, this
	 * value is used. Otherwise, the local host name is used (from
	 * {@code java.net.InetAddress.getLocalHost().getHostName()}}).
	 * 
	 * @return the ID of this cluster node
	 */
	public static String init() {
		String nodeId = System.getProperty(APPNG_NODE_ID);
		if (null == nodeId) {
			try {
				nodeId = InetAddress.getLocalHost().getHostName();
				LOGGER.info("system property {} is not set, using local host name {} as fallback", APPNG_NODE_ID,
						nodeId);
				System.setProperty(APPNG_NODE_ID, nodeId);
			} catch (UnknownHostException e) {
				LOGGER.warn(String.format("error setting system property %s", APPNG_NODE_ID), e);
			}
		}
		return nodeId;
	}

	/**
	 * Returns the ID of this cluster node
	 * 
	 * @param      env
	 *                 do not use
	 * 
	 * @return         the ID of this cluster node
	 * 
	 * @deprecated     use {@link #getNodeId()} instead
	 */
	@Deprecated
	public static String getNodeId(Environment env) {
		return getNodeId();
	}

	/**
	 * Returns the ID of this cluster node
	 * 
	 * @return the ID of this cluster node
	 */
	public static String getNodeId() {
		return System.getProperty(APPNG_NODE_ID);
	}

	protected static Properties getPlatformConfig(Environment env) {
		return env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
	}

	/**
	 * Creates and returns a new {@link Sender} and a corresponding {@link Receiver}. The class name for the receiver is
	 * taken from the platform property {@value org.appng.api.Platform.Property#MESSAGING_RECEIVER}.
	 * 
	 * @param  env
	 *                        the {@link Environment} to use
	 * @param  executor
	 *                        the {@link ExecutorService} to run the {@link Receiver} with
	 * @param  nodeId
	 *                        the node id for the {@link Serializer}
	 * @param  defaultHandler
	 *                        the default {@link EventHandler} for the {@link Receiver} (may be {@code null})
	 * @param  handlers
	 *                        a list of {@link EventHandler}s to be registered at the {@link Receiver}
	 * 
	 * @return
	 *                        <ul>
	 *                        <li>the {@link Sender} (if the platform property
	 *                        {@value org.appng.api.Platform.Property#MESSAGING_ENABLED} is {@code true}
	 *                        <li>{@code null} if messaging is disabled or an error occurred while creating the sender
	 *                        </ul>
	 */
	public static Sender createMessageSender(Environment env, ExecutorService executor, String nodeId,
			EventHandler<? extends Event> defaultHandler, Iterable<EventHandler<? extends Event>> handlers) {
		if (isEnabled(env)) {
			Sender sender = env.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER);
			if (null == sender) {
				try {
					LOGGER.info("node id is {}", nodeId);
					Serializer eventSerializer = new Serializer(env, nodeId);
					String messagingReceiverClassName = getPlatformConfig(env)
							.getString(Platform.Property.MESSAGING_RECEIVER);
					@SuppressWarnings("unchecked")
					Class<? extends Receiver> messagingReceiverClass = (Class<? extends Receiver>) Class
							.forName(messagingReceiverClassName);
					LOGGER.info("using message receiver {}", messagingReceiverClass);
					Receiver eventReceiver = messagingReceiverClass.newInstance().configure(eventSerializer);
					if (null != defaultHandler) {
						eventReceiver.setDefaultHandler(defaultHandler);
					}
					if (null != handlers) {
						for (EventHandler<? extends Event> eventHandler : handlers) {
							eventReceiver.registerHandler(eventHandler);
						}
					}

					eventReceiver.runWith(executor);
					sender = eventReceiver.createSender();
					LOGGER.info("created message sender {}", sender.getClass().getName());
					env.setAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER, sender);
					env.setAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_RECEIVER, eventReceiver);
					return sender;
				} catch (ReflectiveOperationException e) {
					LOGGER.error("error while initializing messaging", e);
					shutdown(env);
				}
			}
		} else {
			LOGGER.info("messaging is disabled");
		}
		return null;
	}

	public static boolean isEnabled(Environment env) {
		return getPlatformConfig(env).getBoolean(Platform.Property.MESSAGING_ENABLED);
	}

	/**
	 * Shuts down the messaging system, i.e. calls {@link Closeable#close()} for the {@link Sender} and the
	 * {@link Receiver} (in case the latter do implement {@link Closeable})
	 * 
	 * @param env
	 *            then {@link Environment} to use
	 */
	public static void shutdown(Environment env) {
		close(env.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_RECEIVER));
		close(env.getAttribute(Scope.PLATFORM, Platform.Environment.MESSAGE_SENDER));
	}

	private static void close(Object o) {
		if (null != o && o instanceof Closeable) {
			try {
				LOGGER.info("Closing {}", o);
				((Closeable) o).close();
			} catch (IOException e) {
				LOGGER.error("Error while closing " + o, e);
			}
		}
	}

}
