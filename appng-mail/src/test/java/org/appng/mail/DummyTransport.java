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
package org.appng.mail;

import org.appng.mail.impl.DefaultTransport;

public class DummyTransport extends DefaultTransport {

	public DummyTransport() {

	}

	public void send(Mail mail) throws MailException {
		buildMessage(mail);
		System.out.println("NOT sending mail");
		System.out.println(mail);
	}

}
