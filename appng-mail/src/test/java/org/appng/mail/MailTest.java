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
package org.appng.mail;

import java.io.File;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import org.appng.mail.Mail.RecipientType;
import org.appng.mail.impl.DefaultTransport;
import org.junit.Assert;
import org.junit.Test;

public class MailTest {

	@Test
	public void test() {
		Mail mail = null;
		try {
			TestMailtransport transport = new TestMailtransport();
			transport.setDebug(true);
			mail = transport.createMail();

			String address = "mm@aiticon.de";

			mail.setSubject("subject");
			mail.setFrom(address);
			mail.addReplyTo(address);

			mail.setTextContent("Ä - Ö  - Ü - ß - конфиденциальности");
			mail.setHTMLContent("foo<ul><li>1</li></ul>bar");

			ClassLoader classLoader = MailTest.class.getClassLoader();
			File a1 = new File(classLoader.getResource("deathstar.jpg").toURI());
			File a2 = new File(classLoader.getResource("log4j.properties").toURI());
			File a3 = new File(classLoader.getResource("test.xlsx").toURI());

			mail.addAttachment(a1, null);
			mail.addAttachment(a2, null);
			mail.addAttachment(a3, null);

			mail.addReceiver(address, "John Doe", RecipientType.TO);
			mail.addReceiver(address, "Foo Bar", RecipientType.CC);
			mail.addReceiver(address, "Max Mustermann", RecipientType.BCC);
			Message message = transport.getMessage(mail);

			Assert.assertEquals("subject", message.getSubject());
			InternetAddress[] addresses = new InternetAddress[]{new InternetAddress(address)};
			Assert.assertArrayEquals(addresses, message.getFrom());
			Assert.assertArrayEquals(addresses, message.getRecipients(javax.mail.Message.RecipientType.TO));
			Assert.assertArrayEquals(addresses, message.getRecipients(javax.mail.Message.RecipientType.CC));
			Assert.assertArrayEquals(addresses, message.getRecipients(javax.mail.Message.RecipientType.BCC));
			Assert.assertArrayEquals(addresses, message.getReplyTo());
			Assert.assertNotNull(message.getSentDate());
			Assert.assertNotNull(message.getHeader("date"));
			Assert.assertNotNull(message.getDataHandler());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	class TestMailtransport extends DefaultTransport {

		TestMailtransport() {
			super("localhost", 25);
		}

		Message getMessage(Mail mail) throws MailException {
			return buildMessage(mail);
		}
	}
}
