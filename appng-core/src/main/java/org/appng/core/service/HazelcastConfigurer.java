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
package org.appng.core.service;

import java.util.Properties;

import org.apache.commons.lang3.EnumUtils;
import org.appng.core.controller.messaging.HazelcastReceiver;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * 
 * Utility class to retrieve the {@link HazelcastInstance} to be used by appNG.
 * This instance is then being used by {@link CacheService} and also by
 * {@link HazelcastReceiver}. <br/>
 * The {@link #configure(Properties)} method needs to be called, allwing to
 * configure the instance with the following properties (defaults in brackets):
 * 
 * <ul>
 * <li><i>mode</i> (multicast)<br/>
 * The connection mode, on of: multicast,tcp,client</li>
 * <li><i>instanceName</i> (appNG)<br/>
 * The instance name for the hazelcast cluster. Supported mode(s):
 * multicast,tcp,client</li>
 * <li><i>multicast.group</i> (224.2.2.3)<br/>
 * The multicast group. Supported mode(s): multicast</li>
 * <li><i>multicast.port</i> (54327)<br/>
 * The multicast port. Supported mode(s): multicast</li>
 * <li><i>multicast.timeToLive</i> (32)<br/>
 * The multicast TTL to use. Supported mode(s): multicast</li>
 * <li><i>multicast.timeoutSeconds</i> (2)<br/>
 * The multicast timeout to use. Supported mode(s): multicast</li>
 * <li><i>port</i> (5701)<br/>
 * The local port to use. Supported mode(s): multicast, tcp</li>
 * <li><i>group</i> (dev)<br/>
 * The group to use. Supported mode(s): client</li>
 * <li><i>addresses</i> (localhost:5701)<br/>
 * The addresses to connect to. Supported mode(s): tcp, client</li>
 * <li><i>logging</i> (jdk)<br/>
 * Type of logging. Allowed Values: jdk, slf4j</li>
 * </ul>
 * 
 * @author Matthias Müller
 *
 */
public class HazelcastConfigurer {

	public static final String MODE = "mode";
	private static final String MULTICAST_GROUP = "multicast.group";
	private static final String MULTICAST_PORT = "multicast.port";
	private static final String MULTICAST_TIME_TO_LIVE = "multicast.timeToLive";
	private static final String MULTICAST_TIMEOUT_SECONDS = "multicast.timeoutSeconds";
	private static final String PORT = "port";
	private static final String GROUP = "group";
	private static final String ADDRESSES = "addresses";
	private static final String LOGGING = "logging";
	private static final String INSTANCE_NAME = "instanceName";
	private static HazelcastInstance instance;

	enum Mode {
		MULTICAST, TCP, CLIENT;
	}

	HazelcastConfigurer() {

	}

	public static HazelcastInstance configure(Properties cachingProps) {

		String modeString = cachingProps.getProperty(MODE, "multicast");
		String instanceName = cachingProps.getProperty(INSTANCE_NAME, "appNG");
		String loggingType = cachingProps.getProperty(LOGGING, "jdk");
		String addresses = cachingProps.getProperty(ADDRESSES, "localhost:5701");
		String group = cachingProps.getProperty(GROUP, "dev");
		String port = cachingProps.getProperty(PORT, "5701");
		String multicastGroup = cachingProps.getProperty(MULTICAST_GROUP, MulticastConfig.DEFAULT_MULTICAST_GROUP);
		String multicastPort = cachingProps.getProperty(MULTICAST_PORT,
				String.valueOf(MulticastConfig.DEFAULT_MULTICAST_PORT));
		String multicastTimeoutSeconds = cachingProps.getProperty(MULTICAST_TIMEOUT_SECONDS,
				String.valueOf(MulticastConfig.DEFAULT_MULTICAST_TIMEOUT_SECONDS));
		String multicastTimeToLive = cachingProps.getProperty(MULTICAST_TIME_TO_LIVE,
				String.valueOf(MulticastConfig.DEFAULT_MULTICAST_TTL));

		Mode mode = EnumUtils.getEnumIgnoreCase(Mode.class, modeString);
		HazelcastInstance instance;
		Config config = new Config();
		config.setProperty("hazelcast.logging.type", loggingType);
		config.setInstanceName(instanceName);
		config.getNetworkConfig().setPort(Integer.valueOf(port));
		JoinConfig joinConfig = config.getNetworkConfig().getJoin();
		switch (mode) {
		case CLIENT:
			ClientConfig clientConfig = new ClientConfig();
			clientConfig.getGroupConfig().setName(group);
			clientConfig.setInstanceName(instanceName);
			String[] addressArr = addresses.split(",");
			for (String address : addressArr) {
				clientConfig.getNetworkConfig().addAddress(address.trim());
			}
			instance = HazelcastClient.getHazelcastClientByName(clientConfig.getInstanceName());
			if (null == instance) {
				instance = HazelcastClient.newHazelcastClient(clientConfig);
			}
			break;

		case TCP:
			joinConfig.getTcpIpConfig().setEnabled(true);
			joinConfig.getMulticastConfig().setEnabled(false);
			joinConfig.getTcpIpConfig().addMember(addresses);
			instance = Hazelcast.getOrCreateHazelcastInstance(config);
			break;

		default:
			joinConfig.getTcpIpConfig().setEnabled(false);
			joinConfig.getMulticastConfig().setEnabled(true);
			joinConfig.getMulticastConfig().setMulticastGroup(multicastGroup);
			joinConfig.getMulticastConfig().setMulticastPort(Integer.valueOf(multicastPort));
			joinConfig.getMulticastConfig().setMulticastTimeoutSeconds(Integer.valueOf(multicastTimeoutSeconds));
			joinConfig.getMulticastConfig().setMulticastTimeToLive(Integer.valueOf(multicastTimeToLive));
			instance = Hazelcast.getOrCreateHazelcastInstance(config);
			break;
		}

		return instance;

	}

	public static HazelcastInstance getInstance() {
		return instance;
	}
}
