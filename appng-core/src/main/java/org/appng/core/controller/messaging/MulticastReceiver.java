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
package org.appng.core.controller.messaging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.Platform;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.EventRegistry;
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MulticastReceiver extends MessageHandler implements Receiver, Runnable {

	static final String APPNG_MESSAGING_NODE_IPS = "appng.messaging.node_ips";

	private Serializer eventSerializer;

	private EventRegistry eventRegistry = new EventRegistry();

	private MulticastSocket socket;

	public MulticastReceiver() {

	}

	public MulticastReceiver(String address, Integer port) {
		super(address, port);
	}

	public MulticastReceiver configure(Serializer eventSerializer) {
		this.eventSerializer = eventSerializer;
		Properties platformConfig = eventSerializer.getPlatformConfig();
		Integer port = platformConfig.getInteger(Platform.Property.MESSAGING_GROUP_PORT);
		String address = platformConfig.getString(Platform.Property.MESSAGING_GROUP_ADDRESS);
		setGroupAddress(address);
		setGroupPort(port);
		return this;
	}

	public void runWith(ExecutorService executorService) {
		executorService.submit(this);
	}

	public Sender createSender() {
		return new MulticastSender(getGroupAddress(), getGroupPort()).configure(eventSerializer);
	}

	public void run() {
		try {
			Integer groupPort = getGroupPort();
			String groupAddress = getGroupAddress();
			socket = new MulticastSocket(groupPort);
			InetAddress address = InetAddress.getByName(groupAddress);
			socket.joinGroup(address);
			LOGGER.info("start listening at multicast {}:{}", groupAddress, groupPort);
			String nodeIpsProp = System.getProperty(APPNG_MESSAGING_NODE_IPS);
			List<String> nodeIps = new ArrayList<>();
			if (null != nodeIpsProp) {
				nodeIps.addAll(Arrays.asList(nodeIpsProp.split(",")));
				LOGGER.debug("node IPs: {}", nodeIpsProp);
			}
			while (true) {
				byte[] inBuf = new byte[(int) FileUtils.ONE_MB];
				DatagramPacket inPacket = new DatagramPacket(inBuf, inBuf.length);
				socket.receive(inPacket);
				Event event = eventSerializer.deserialize(inBuf);
				if (null != event) {
					InetAddress senderAddress = inPacket.getAddress();
					String senderHost = senderAddress.getHostAddress();
					onEvent(eventSerializer.getSite(event.getSiteName()), event, nodeIps, senderHost);
				} else {
					LOGGER.debug("could not read event");
				}
			}
		} catch (Exception e) {
			LOGGER.error("error in run()", e);
		} finally {
			socket.close();
		}

	}

	void onEvent(Site site, Event event, List<String> nodeIps, String senderHost)
			throws IOException, InterruptedException {
		if (nodeIps.isEmpty() || nodeIps.contains(senderHost)) {
			LOGGER.info("received message from {}: {}", senderHost, event);
			String currentNode = eventSerializer.getNodeId();
			String originNode = event.getNodeId();
			LOGGER.debug("current node: {}, originNode node: {}", currentNode, originNode);
			boolean sameNode = StringUtils.equals(currentNode, originNode);
			boolean sameAddress = isSameAddress(senderHost);
			if (!sameAddress || !sameNode) {
				try {
					LOGGER.info("about to execute {} received from  {}", event, senderHost);
					for (EventHandler<Event> eventHandler : eventRegistry.getHandlers(event)) {
						eventHandler.onEvent(event, eventSerializer.getEnvironment(), site);
					}
				} catch (Exception e) {
					LOGGER.error(String.format("error while performing event %s", event), e);
				}
			} else {
				LOGGER.debug("message is from myself and can be ignored");
			}
		} else {
			LOGGER.debug("ignoring message from {}", senderHost);
		}
	}

	public void registerHandler(EventHandler<?> handler) {
		eventRegistry.register(handler);
	}

	public void setDefaultHandler(EventHandler<?> defaultHandler) {
		eventRegistry.setDefaultHandler(defaultHandler);
	}

	public void close() throws IOException {
		if (null != socket) {
			socket.close();
		}
	}

}
