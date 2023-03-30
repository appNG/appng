/*
 * Copyright 2011-2023 the original author or authors.
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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

abstract class MessageHandler {

	private Integer port;
	private String address;

	MessageHandler() {

	}

	protected MessageHandler(String address, Integer port) {
		this.address = address;
		this.port = port;
	}

	protected Integer getGroupPort() {
		return port;
	}

	protected String getGroupAddress() {
		return address;
	}

	protected void setGroupPort(Integer port) {
		this.port = port;
	}

	protected void setGroupAddress(String address) {
		this.address = address;
	}

	protected boolean isSameAddress(String hostAddress) throws UnknownHostException, SocketException {
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			NetworkInterface n = e.nextElement();
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = ee.nextElement();
				if (i.getHostAddress().equals(hostAddress)) {
					return true;
				}
			}
		}
		return false;
	}
}
