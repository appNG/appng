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

import java.io.InputStream;

import org.appng.core.controller.messaging.HazelcastReceiver;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * 
 * Utility class to retrieve the {@link HazelcastInstance} to be used by appNG.
 * This instance is then being used by {@link CacheService} and also by
 * {@link HazelcastReceiver}.
 * 
 * @author Matthias Müller
 *
 */
public class HazelcastConfigurer {

	private static HazelcastInstance instance;

	HazelcastConfigurer() {

	}

	public static HazelcastInstance configure(InputStream inputStream) {
		if (null != inputStream) {
			String providerType = System.getProperty("hazelcast.jcache.provider.type");
			if ("server".equals(providerType)) {
				Config config = new XmlConfigBuilder(inputStream).build();
				instance = Hazelcast.getOrCreateHazelcastInstance(config);
			} else {
				ClientConfig clientConfig = new XmlClientConfigBuilder(inputStream).build();
				instance = HazelcastClient.getHazelcastClientByName(clientConfig.getInstanceName());
				if (null == instance) {
					instance = HazelcastClient.newHazelcastClient(clientConfig);
				}
			}
		} else {
			instance = Hazelcast.newHazelcastInstance();
		}
		return instance;
	}

	public static HazelcastInstance getInstance() {
		return instance;
	}
}
