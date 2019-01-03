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
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

import org.apache.catalina.tribes.ByteMessage;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelInterceptor;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.membership.McastService;
import org.apache.catalina.tribes.transport.ReplicationTransmitter;
import org.apache.catalina.tribes.transport.nio.NioReceiver;
import org.apache.catalina.tribes.transport.nio.PooledParallelSender;
import org.appng.api.BusinessException;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.EventHandler;
import org.appng.api.messaging.EventRegistry;
import org.appng.api.messaging.Receiver;
import org.appng.api.messaging.Sender;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class TribesReceiver extends MessageHandler implements Receiver, Runnable, ChannelListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(TribesReceiver.class);
	private GroupChannel channel = new GroupChannel();
	private Serializer eventSerializer;
	private EventRegistry eventRegistry = new EventRegistry();

	public TribesReceiver() {
		super("", -1);
	}

	public TribesReceiver configure(Serializer eventSerializer) {
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
		return new TribesSender(channel).configure(eventSerializer);
	}

	public void run() {
		try {
			InetAddress inetAddress = getNodeAddress();

			McastService membershipService = new McastService();
			membershipService.setAddress(getGroupAddress());
			membershipService.setPort(getGroupPort());
			membershipService.setFrequency(500);
			membershipService.setDropTime(3000);
			channel.setMembershipService(membershipService);
			channel.addMembershipListener(membershipService);

			NioReceiver channelReceiver = new NioReceiver();
			channelReceiver.setAddress(inetAddress.getHostAddress());
			channelReceiver.setPort(getGroupPort());
			channelReceiver.setAutoBind(100);
			channelReceiver.setSelectorTimeout(5000);
			channel.setChannelReceiver(channelReceiver);

			ReplicationTransmitter clusterSender = new ReplicationTransmitter();
			clusterSender.setTransport(new PooledParallelSender());
			channel.setChannelSender(clusterSender);

			ChannelInterceptor messageDispatchInterceptor = getMessageDispatchInterceptor();
			channel.addInterceptor(messageDispatchInterceptor);
			channel.addInterceptor(new TcpFailureDetector());
			channel.addChannelListener(this);

			channel.start(Channel.DEFAULT);
		} catch (ChannelException | IOException | ReflectiveOperationException e) {
			LOGGER.error("error starting channel ", e);
		}
	}

	protected ChannelInterceptor getMessageDispatchInterceptor() throws ReflectiveOperationException {
		ClassLoader classLoader = getClass().getClassLoader();
		String interceptorClass = "MessageDispatchInterceptor";
		if (!ClassUtils.isPresent(interceptorClass, classLoader)) {
			interceptorClass = "MessageDispatch15Interceptor";
		}
		return (ChannelInterceptor) classLoader
				.loadClass("org.apache.catalina.tribes.group.interceptors." + interceptorClass).newInstance();
	}

	public void messageReceived(Serializable msg, Member sender) {
		if (msg instanceof ByteMessage) {
			try {
				Event event = eventSerializer.deserialize(((ByteMessage) msg).getMessage());
				if (null != event) {
					LOGGER.debug("about to perform {}", event);
					Site site = eventSerializer.getSite(event.getSiteName());
					for (EventHandler<Event> eventHandler : eventRegistry.getHandlers(event)) {
						eventHandler.onEvent(event, eventSerializer.getEnvironment(), site);
					}
				}
			} catch (InvalidConfigurationException | BusinessException e) {
				LOGGER.error("error performing event " + msg, e);
			}
		}
	}

	public boolean accept(Serializable msg, Member sender) {
		try {
			InetAddress byAddress = InetAddress.getByAddress(sender.getHost());
			InetAddress nodeAddress = getNodeAddress();
			boolean equalsAddress = byAddress.equals(nodeAddress);
			if (equalsAddress) {
				return false;
			}
		} catch (UnknownHostException e) {
		}
		return (msg instanceof ByteMessage);
	}

	protected InetAddress getNodeAddress() throws UnknownHostException {
		String nodeAddress = System.getProperty(MulticastSender.APPNG_MESSAGING_BIND_ADR);
		return null == nodeAddress ? InetAddress.getLocalHost() : InetAddress.getByName(nodeAddress);
	}

	public void registerHandler(EventHandler<?> handler) {
		eventRegistry.register(handler);
	}

	public void setDefaultHandler(EventHandler<?> defaultHandler) {
		eventRegistry.setDefaultHandler(defaultHandler);
	}
}
