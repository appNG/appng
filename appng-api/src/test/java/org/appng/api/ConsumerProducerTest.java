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
package org.appng.api;

import java.util.concurrent.TimeoutException;

import org.appng.api.search.Consumer;
import org.appng.api.search.Producer;
import org.junit.Assert;
import org.junit.Test;

public class ConsumerProducerTest {

	private boolean passed = false;

	class StringProducer extends Producer<String> implements Runnable {

		public StringProducer(int queueSize) {
			super(queueSize);
		}

		public void run() {
			try {
				int ms = 0;
				while (true) {
					ms += 10;
					put("waiting " + ms + " ms");
					Thread.sleep(ms);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	class StringConsumer extends Consumer<String, Producer<String>> implements Runnable {

		public void run() {
			try {
				Producer<String> producer = get();
				String s = null;
				while ((s = producer.getWithTimeout(80l)) != null) {
					System.out.println(s);
				}
			} catch (Exception e) {
				boolean isTimeout = TimeoutException.class.equals(e.getClass());
				boolean ismessage = e.getMessage().equals("no java.lang.String returned after 80 milliseconds");
				System.out.println(e.getMessage());
				passed = isTimeout && ismessage;
			}

		}
	}

	@Test(timeout = 5000)
	public void test() throws Exception {
		StringProducer producer = new StringProducer(5);
		StringConsumer consumer = new StringConsumer();
		consumer.put(producer);

		new Thread(producer).start();
		new Thread(consumer).start();
		while (!passed) {
			Thread.sleep(100);
		}
		Assert.assertTrue(passed);
	}
}
