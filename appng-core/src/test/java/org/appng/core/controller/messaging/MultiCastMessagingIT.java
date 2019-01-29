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

import org.appng.api.Platform;
import org.mockito.Mockito;

/**
 * Integration test for Multicast messaging.
 * <p>
 * Before running this test, execute the following commands:
 * 
 * <pre>
 * # enable multicast traffic for loopback interface
 * sudo ip link set multicast on dev lo
 *
 * # route traffic to multicast address range over loopback interface
 * sudo ip route add 224.0.0.0/4 dev lo
 * </pre>
 * </p>
 * 
 * @author Matthias Müller
 *
 */
public class MultiCastMessagingIT extends AbstractMessagingIT {

	public MultiCastMessagingIT() {
		super(MulticastSender.class, MulticastReceiver.class);
	}

	protected void configureMessaging() {
		Mockito.when(props.getInteger(Platform.Property.MESSAGING_GROUP_PORT)).thenReturn(4000);
		Mockito.when(props.getString(Platform.Property.MESSAGING_GROUP_ADDRESS)).thenReturn("224.2.2.4");
	}

}
