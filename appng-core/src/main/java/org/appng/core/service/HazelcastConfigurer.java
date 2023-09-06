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
package org.appng.core.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Messaging;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.api.support.environment.ScopedEnvironment;
import org.appng.core.controller.messaging.HazelcastReceiver;
import org.appng.core.controller.messaging.NodeEvent;
import org.appng.core.controller.messaging.NodeEvent.NodeState;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to retrieve the {@link HazelcastInstance} to be used by appNG. This instance is then being used by
 * {@link CacheService} and also by {@link HazelcastReceiver}.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class HazelcastConfigurer {

	private static final String HAZELCAST_CLIENT_XML = "WEB-INF/conf/hazelcast-client.xml";
	public static final String HAZELCAST_USE_CLIENT = "hazelcastUseClient";
	private static HazelcastInstance instance;
	private static boolean isClient = false;

	HazelcastConfigurer() {

	}

	public static HazelcastInstance getInstance(PlatformProperties platformProperties) {
		return getInstance(platformProperties, null, null);
	}

	public static HazelcastInstance getInstance(PlatformProperties platformProps, String clientId,
			Environment environment) {
		if (null == instance) {
			if (null != platformProps) {
				try {
					String providerType = System.getProperty("hazelcast.jcache.provider.type");
					Boolean useClient = platformProps.getBoolean(HAZELCAST_USE_CLIENT, false);
					if ("client".equals(providerType) || useClient) {
						String appNGData = platformProps.getString(Platform.Property.APPNG_DATA);
						FileInputStream clientIs = new FileInputStream(new File(appNGData, HAZELCAST_CLIENT_XML));

						ClientConfig clientConfig = new XmlClientConfigBuilder(clientIs).build();
						if (StringUtils.isNotBlank(clientId)) {
							clientConfig.setInstanceName(clientConfig.getInstanceName() + "_" + clientId);
						}
						instance = HazelcastClient.getHazelcastClientByName(clientConfig.getInstanceName());
						if (null == instance) {
							instance = HazelcastClient.newHazelcastClient(clientConfig);
							LOGGER.info("Created new client '{}' for ID '{}'", instance.getName(), clientId);
						} else {
							LOGGER.info("Using existing client '{}' for ID '{}'", instance.getName(), clientId);
						}
						isClient = true;
					} else {
						InputStream cacheConfig = platformProps.getCacheConfig();
						if (null != cacheConfig) {
							Config config = new XmlConfigBuilder(cacheConfig).build();
							config.getMemberAttributeConfig().setAttribute(Messaging.APPNG_NODE_ID,
									Messaging.getNodeId());
							instance = Hazelcast.getOrCreateHazelcastInstance(config);
							LOGGER.info("Using {}", instance);
						}
					}
					instance.getCluster().addMembershipListener(getMembershipListener(environment));
				} catch (IOException e) {
					LOGGER.error("failed to create Hazelcast instance!", e);
				}
			}
		} else if (isClient) {
			LOGGER.info("Using existing client '{}' for ID '{}'", instance.getName(), clientId);
		}
		if (null == instance) {
			LOGGER.warn("No Hazelcast configuration could be found, using default!");
			instance = Hazelcast.newHazelcastInstance();
			instance.getCluster().addMembershipListener(getMembershipListener(environment));
			LOGGER.info("Created default instance {}", instance);
		}
		return instance;
	}

	private static MembershipListener getMembershipListener(Environment environment) {

		return new MembershipListener() {

			final ScopedEnvironment scoped = null == environment ? null
					: ((DefaultEnvironment) environment).getEnvironment(Scope.PLATFORM);

			@Override
			public void memberRemoved(MembershipEvent me) {
				Member member = me.getMember();
				String nodeId = member.getAttribute(Messaging.APPNG_NODE_ID);
				LOGGER.info("Node removed: {} ({})", nodeId, member.getUuid());
				if (null != scoped) {
					Map<String, NodeState> clusterState = scoped.getAttribute(NodeEvent.NODE_STATE);
					NodeState removed = clusterState.remove(nodeId);
					if (removed == null) {
						LOGGER.warn("Failed removing node '{}' from cluster state.", nodeId);
					} else {
						LOGGER.info("Removed node '{}' from cluster state.", nodeId);
					}
				}
			}

			@Override
			public void memberAdded(MembershipEvent me) {
				Member member = me.getMember();
				String nodeId = member.getAttribute(Messaging.APPNG_NODE_ID);
				LOGGER.info("Node added: {} ({})", nodeId, member.getUuid());
			}
		};
	}

	public static void shutdown() {
		if (null != instance) {
			LOGGER.info("Shutting down instance {}", instance.getName());
			instance.shutdown();
			instance = null;
		}
	}

	public static boolean isClient() {
		return isClient;
	}

}
