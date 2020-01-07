/*
 * Copyright 2011-2020 the original author or authors.
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

/**
 * SPI for sending e-mails.
 * 
 * @author Matthias Müller
 * 
 */
public interface MailTransport {

	/**
	 * Creates and returns a new {@link Mail}.
	 * 
	 * @return the new {@link Mail}
	 */
	Mail createMail();

	/**
	 * Send the given {@link Mail}.
	 * 
	 * @param mail
	 *            the {@link Mail} to send
	 * @throws MailException
	 *             if an error occurs while sending the {@link Mail}
	 */
	void send(Mail mail) throws MailException;

	/**
	 * Returns a string-representation if the given {@link Mail}.<br/>
	 * <b>Attention:</b> The {@link Mail} may contain sensitive informations like passwords!
	 * 
	 * @param mail
	 *            the {@link Mail} to get the string-representation for
	 * @return a {@link String} representation the {@link Mail}
	 */
	String getMailAsString(Mail mail);

	/**
	 * Checks whether this {@link MailTransport} is disabled.
	 * 
	 * @return {@code true} if this {@link MailTransport} is disabled, {@code false} otherwise.
	 */
	boolean isDisableSend();

	/**
	 * Enable/disable sending for this {@link MailTransport}. If set to {@code true}, {@link Mail}s will be logged
	 * instead of being send.
	 * 
	 * @param disableSend
	 */
	void setDisableSend(boolean disableSend);

}
