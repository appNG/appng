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
package org.appng.core.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.appng.core.controller.messaging.HazelcastReceiver;
import org.springframework.util.ClassUtils;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Utility class to retrieve the {@link HazelcastInstance} to be used by appNG.
 * This instance is then being used by {@link CacheService} and also by
 * {@link HazelcastReceiver}.
 * 
 * @author Matthias Müller
 *
 */
@Slf4j
public class HazelcastConfigurer {

	private static final String HAZELCAST_CLIENT = "com.hazelcast.client.HazelcastClient";
	private static HazelcastInstance instance;
	private static boolean clientPresent;
	private static boolean isClient;

	static {
		clientPresent = ClassUtils.isPresent(HAZELCAST_CLIENT, HazelcastConfigurer.class.getClassLoader());
	}

	HazelcastConfigurer() {

	}

	public static HazelcastInstance getInstance(PlatformProperties platformProperties) {
		return getInstance(platformProperties, null);
	}

	public static HazelcastInstance getInstance(PlatformProperties platformProperties, String clientId) {
		if (null == instance) {
			if (null != platformProperties) {
				try {
					InputStream cacheConfig = platformProperties.getCacheConfig();
					if (null != cacheConfig) {
						String providerType = System.getProperty("hazelcast.jcache.provider.type");
						if ("server".equals(providerType) || !clientPresent) {
							Config config = new XmlConfigBuilder(cacheConfig).build();
							instance = Hazelcast.getOrCreateHazelcastInstance(config);
							LOGGER.info("Using {}", instance);
						} else {
							ClientConfig clientConfig = new XmlClientConfigBuilder(cacheConfig).build();
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
						}
					}
				} catch (IOException e) {
					LOGGER.error("failed to create Hazalcast instance!", e);
				}
			}
		} else if (isClient) {
			LOGGER.info("Using existing client '{}' for ID '{}'", instance.getName(), clientId);
		}
		if (null == instance) {
			instance = Hazelcast.newHazelcastInstance();
			LOGGER.info("Created default instance {}", instance.getName());
		}
		return instance;
	}

	public static void shutdown() {
		if (null != instance) {
			LOGGER.info("Shutting down instance {}", instance.getName());
			instance.shutdown();
			instance = null;
		}
	}

}
