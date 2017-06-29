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

import org.apache.commons.lang3.StringUtils;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;

import redis.clients.jedis.Jedis;

/**
 * TODO insert description
 * 
 * @author Claus Stümke, aiticon GmbH, 2015
 *
 */
public class JedisBase {
	protected Serializer eventSerializer;
	protected String host;
	protected String channel;
	protected String password;
	protected int port;
	protected int timeout;

	protected void initialize() {
		Properties platformConfig = eventSerializer.getPlatformConfig();
		port = platformConfig.getInteger("redisMessagingPort", 6379);
		host = platformConfig.getString("redisMessagingHost", "localhost");
		timeout = platformConfig.getInteger("redisMessagingTimeout", 0);
		channel = platformConfig.getString("redisMessagingChannel", "appng-messaging");
		password = platformConfig.getString("redisMessagingPassword", "");
	}

	public Jedis getJedis() {
		Jedis jedis;
		if (timeout > 0) {
			jedis = new Jedis(host, port, timeout);
		} else {
			jedis = new Jedis(host, port);
		}
		if (StringUtils.isNotEmpty(password)) {
			jedis.auth(password);
		}
		return jedis;

	}
}
