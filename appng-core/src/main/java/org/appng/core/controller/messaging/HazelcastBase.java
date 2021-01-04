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
package org.appng.core.controller.messaging;

import org.appng.api.messaging.Serializer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;

/**
 * Base class for Hazelcast messaging
 * 
 * @author Matthias Müller
 *
 */
abstract class HazelcastBase {

	protected HazelcastInstance instance;
	protected Serializer serializer;

	HazelcastBase() {

	}

	HazelcastBase(HazelcastInstance instance) {
		this.instance = instance;
	}

	protected ITopic<byte[]> getTopic() {
		return instance.getReliableTopic(getTopicName());
	}

	protected String getTopicName() {
		return serializer.getPlatformConfig().getString("hazelcastTopicName", "appng-messaging");
	}

}
