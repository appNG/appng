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

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.Connections;
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

	protected static final String RABBIT_MQ_EXCHANGE = "rabbitMQExchange";
	protected static final String RABBIT_MQ_PASSWORD = "rabbitMQPassword";
	protected static final String RABBIT_MQ_USER = "rabbitMQUser";
	protected static final String RABBIT_MQ_ADRESSES = "rabbitMQAdresses";
	protected static final String RABBIT_MQ_HOST = "rabbitMQHost";
	protected static final String RABBIT_MQ_PORT = "rabbitMQPort";

	private static final int INTERVAL_RECOVER_MILLIS = 200;

	protected static final String EXCHANGE_TYPE_FANOUT = "fanout";

	protected Serializer eventSerializer;
	protected int port;
	protected String host;
	protected String user;
	protected String password;
	protected String exchange;
	protected String addresses;

	protected ConnectionFactory factory;

	public void initialize() {
		Properties platformConfig = eventSerializer.getPlatformConfig();
		port = platformConfig.getInteger(RABBIT_MQ_PORT, 5672);
		host = platformConfig.getString(RABBIT_MQ_HOST, "localhost");
		addresses = platformConfig.getString(RABBIT_MQ_ADRESSES);
		user = platformConfig.getString(RABBIT_MQ_USER, "guest");
		password = platformConfig.getString(RABBIT_MQ_PASSWORD, "guest");
		exchange = platformConfig.getString(RABBIT_MQ_EXCHANGE, "appng-messaging");
		initConnectionFactory();
	}

	protected Config getConnectionConfig() {
		Config config = new Config()
				.withRecoveryPolicy(
						RecoveryPolicies.recoverAlways().withInterval(Duration.millis(INTERVAL_RECOVER_MILLIS)))
				.withRetryPolicy(new RetryPolicy().withMaxAttempts(20).withInterval(Duration.seconds(1))
						.withMaxDuration(Duration.minutes(5)));
		return config;
	}

	protected void initConnectionFactory() {
		factory = new ConnectionFactory();
		if (StringUtils.isBlank(addresses)) {
			factory.setHost(this.host);
			factory.setPort(this.port);
		}
		factory.setUsername(this.user);
		factory.setPassword(this.password);
	}

	protected Connection getConnection(ConnectionOptions options) throws IOException, TimeoutException {
		if (StringUtils.isBlank(addresses)) {
			return Connections.create(factory, getConnectionConfig());
		} else {
			options.withUsername(user);
			options.withPassword(password);
			return Connections.create(options.withAddresses(addresses), getConnectionConfig());
		}
	}

}
