/*
 * Copyright 2011-2017 the original author or authors.
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

/**
 * Integration test for Jedis messaging.
 * 
 * @author Matthias Müller
 *
 */
public class JedisMessagingIT extends AbstractMessagingIT {

	public JedisMessagingIT() {
		super(JedisSender.class, JedisReceiver.class);
	}

	protected void configureMessaging() {
		mockDefaultString(JedisSender.REDIS_MESSAGING_HOST, "localhost");
		mockDefaultInteger(JedisSender.REDIS_MESSAGING_PORT, 6379);
		mockDefaultInteger(JedisSender.REDIS_MESSAGING_TIMEOUT, 0);
		mockDefaultString(JedisSender.REDIS_MESSAGING_PASSWORD, "");
		mockDefaultString(JedisSender.REDIS_MESSAGING_CHANNEL, "appng-messaging");
	}

}
