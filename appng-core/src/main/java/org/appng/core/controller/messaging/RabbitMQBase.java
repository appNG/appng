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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;
import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Base class to provide some base functionality for message receiver and sender for RabbitMQ
 * 
 * @author Claus Stümke, aiticon GmbH, 2015
 * @author Matthias Müller
 *
 */
public abstract class RabbitMQBase implements Closeable {

	protected static final String RABBIT_MQ_EXCHANGE = "rabbitMQExchange";
	protected static final String RABBIT_MQ_PASSWORD = "rabbitMQPassword";
	protected static final String RABBIT_MQ_USER = "rabbitMQUser";
	protected static final String RABBIT_MQ_ADRESSES = "rabbitMQAdresses";

	protected Serializer eventSerializer;
	protected String user;
	protected String password;
	protected String exchange;
	protected String addresses;

	protected ConnectionFactory factory;
	protected Connection connection;
	protected Channel channel;

	public void initialize(String threadNameFormat) {
		Properties platformConfig = eventSerializer.getPlatformConfig();
		addresses = platformConfig.getString(RABBIT_MQ_ADRESSES, "localhost:5672");
		user = platformConfig.getString(RABBIT_MQ_USER, "guest");
		password = platformConfig.getString(RABBIT_MQ_PASSWORD, "guest");
		exchange = platformConfig.getString(RABBIT_MQ_EXCHANGE, "appng-messaging");
		List<Address> addrs = initFactory(threadNameFormat);
		try {
			connection = factory.newConnection(addrs);
			channel = connection.createChannel();
		} catch (IOException | TimeoutException e) {
			log().error("error while creating connection/channel", e);
		}
	}

	private List<Address> initFactory(String threadNameFormat) {
		factory = new ConnectionFactory();
		factory.setUsername(user);
		factory.setPassword(password);
		ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(threadNameFormat)
				.build();
		factory.setThreadFactory(threadFactory);

		List<Address> addrs = new ArrayList<>();
		for (String adress : addresses.split(",")) {
			String[] singleAddress = adress.split(":");
			String host = singleAddress[0];
			if (singleAddress.length == 1) {
				addrs.add(new Address(host));
			} else {
				addrs.add(new Address(host, Integer.parseInt(singleAddress[1])));
			}
		}
		return addrs;
	}

	public void close() throws IOException {
		try {
			if (null != channel) {
				log().info("closing channel {}", channel);
				channel.close();
			}
		} catch (IOException | TimeoutException e) {
			log().error("error closing channel", e);
		}
		try {
			if (null != connection) {
				log().info("closing connection {}", connection);
				connection.close();
			}
		} catch (IOException e) {
			log().error("error closing connection", e);
		}
	}

	abstract Logger log();

}
