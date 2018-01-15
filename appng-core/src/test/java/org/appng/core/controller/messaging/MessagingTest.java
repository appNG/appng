/*
 * Copyright 2011-2018 the original author or authors.
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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.Serializer;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class MessagingTest extends Event {

	private static final String LOCALHOST = "localhost";
	protected volatile boolean processed = false;

	public MessagingTest() {
		super(LOCALHOST);
	}

	protected MessagingTest(String siteName) {
		super(siteName);
	}

	@Test
	public void test() throws IOException, InterruptedException {
		Site site = Mockito.mock(Site.class);
		Mockito.when(site.getSiteClassLoader()).thenReturn(new URLClassLoader(new URL[0]));
		Mockito.when(site.getName()).thenReturn(LOCALHOST);
		Mockito.when(site.getHost()).thenReturn(LOCALHOST);
		Mockito.when(site.getDomain()).thenReturn(LOCALHOST);
		Mockito.when(site.getProperties()).thenReturn(Mockito.mock(Properties.class));

		Assert.assertFalse(processed);
		MulticastReceiver receiver = new MulticastReceiver("224.2.2.4", 4000);
		Serializer serializer = Mockito.mock(Serializer.class);
		Mockito.when(serializer.getEnvironment()).thenReturn(Mockito.mock(Environment.class));
		Mockito.when(serializer.getPlatformConfig()).thenReturn(Mockito.mock(Properties.class));
		receiver.configure(serializer);
		receiver.onEvent(site, new MessagingTest("example.com"), Arrays.asList(LOCALHOST), LOCALHOST);
		Assert.assertFalse(processed);
		receiver.onEvent(site, this, Arrays.asList("somehost"), LOCALHOST);
		Assert.assertFalse(processed);
		receiver.onEvent(site, this, Arrays.asList(LOCALHOST), LOCALHOST);
		Assert.assertTrue(processed);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException, BusinessException {
		processed = true;
	}

	@Override
	public String getNodeId() {
		return "appng.node2";
	}
}
