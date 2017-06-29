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
package org.appng.mail;

import org.appng.mail.Mail.RecipientType;

public class Receiver extends Address {

	private final RecipientType type;
	private boolean valid = true;

	public Receiver(String email, String name) {
		this(email, name, RecipientType.TO);
	}

	public Receiver(String email, String name, RecipientType type) {
		super(email, name);
		this.type = type;
	}

	public Receiver(String email, RecipientType type) {
		this(email, null, type);
	}

	public RecipientType getType() {
		return type;
	}

	public String toString() {
		return (type == null ? "" : type + ": ")
				+ (getName() == null ? getEmail() : (getName() + " (" + getEmail() + ")"));
	}

	public void markInvalid() {
		this.valid = false;
	}

	public boolean isValid() {
		return valid;
	}

}
