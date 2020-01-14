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

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface Mail {

	public enum RecipientType {
		TO, CC, BCC
	}

	void setEncoding(String encoding);

	String getEncoding();

	Mail addReceiver(Receiver receiver);

	Mail addReceiver(String email, RecipientType type);

	Mail addReceiver(String email, String name, RecipientType type);

	Mail setHTMLContent(String content);

	Mail setTextContent(String content);

	Mail setSubject(String subject);

	Mail setFrom(String from);

	Mail setFrom(String from, String name);

	Mail addReplyTo(String email);

	Mail addReplyTo(String email, String name);

	Mail addAttachment(InputStream is, String name, String mimeType) throws MailException;

	Mail addAttachment(File file, String mimeType) throws MailException;

	Mail addAttachment(File file, String name, String mimeType) throws MailException;

	boolean hasAttachments();

	List<Receiver> getReceivers();

	List<Receiver> getReplyTo();

	List<Attachment> getAttachments();

	String getFrom();

	Address getFromAddress();

	String getSubject();

	String getHtmlContent();

	String getTextContent();

	void markInvalidReceivers(List<String> invalid);

	boolean hasReceivers();

}
