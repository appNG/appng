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

import org.mockito.Mockito;

/**
 * Integration test for Rabbit MQ messaging.
 * 
 * @author Matthias Müller
 *
 */
public class RabbitMQMessagingIT extends AbstractMessagingIT {

	public RabbitMQMessagingIT() {
		super(RabbitMQSender.class, RabbitMQReceiver.class);
	}

	protected void configureMessaging() {
		mockDefaultString(RabbitMQSender.RABBIT_MQ_USER, "guest");
		mockDefaultString(RabbitMQSender.RABBIT_MQ_PASSWORD, "guest");
		mockDefaultString(RabbitMQSender.RABBIT_MQ_EXCHANGE, "appng-messaging");
		Mockito.when(props.getString(Mockito.eq(RabbitMQSender.RABBIT_MQ_ADRESSES), Mockito.anyString()))
				.thenReturn("localhost:5672");
	}

}
