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

import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;

import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.RecoveryPolicies;
import net.jodah.lyra.config.RetryPolicy;
import net.jodah.lyra.util.Duration;

/**
 * Base class to provide some base functionality for message receiver and sender for RabbitMQ
 * 
 * @author Claus Stümke, aiticon GmbH, 2015
 *
 */

public class RabbitMQBase {

	/**
	 * 
	 */
	private static final int INTERVAL_RECOVER_MILLIS = 200;

	protected static final String EXCHANGE_TYPE_FANOUT = "fanout";

	protected Serializer eventSerializer;
	protected int port;
	protected String host;
	protected String user;
	protected String password;
	protected String exchange;

	public void initialize() {
		Properties platformConfig = eventSerializer.getPlatformConfig();
		port = platformConfig.getInteger("rabbitMQPort", 5672);
		host = platformConfig.getString("rabbitMQHost", "localhost");
		user = platformConfig.getString("rabbitMQUser", "guest");
		password = platformConfig.getString("rabbitMQPassword", "guest");
		exchange = platformConfig.getString("rabbitMQExchange", "appng-messaging");
	}

	protected Config getConnectionConfig() {
		Config config = new Config()
				.withRecoveryPolicy(
						RecoveryPolicies.recoverAlways().withInterval(Duration.millis(INTERVAL_RECOVER_MILLIS)))
				.withRetryPolicy(new RetryPolicy().withMaxAttempts(20).withInterval(Duration.seconds(1))
						.withMaxDuration(Duration.minutes(5)));
		return config;
	}

}
