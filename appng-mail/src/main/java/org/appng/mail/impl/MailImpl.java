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
package org.appng.mail.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.appng.mail.Address;
import org.appng.mail.Attachment;
import org.appng.mail.Mail;
import org.appng.mail.MailException;
import org.appng.mail.Receiver;

public class MailImpl implements Mail {

	private List<Receiver> receivers;
	private List<Receiver> replyTo;
	private List<Attachment> attachments;
	private Address from;
	private String subject;
	private String htmlContent;
	private String textContent;
	private String encoding;

	MailImpl() {
		this.replyTo = new ArrayList<Receiver>();
		this.receivers = new ArrayList<Receiver>();
		this.attachments = new ArrayList<Attachment>();
	}

	public Mail addReceiver(Receiver receiver) {
		this.receivers.add(receiver);
		return this;
	}

	public Mail addReceiver(String email, RecipientType type) {
		this.receivers.add(new Receiver(email, type));
		return this;
	}

	public Mail addReceiver(String email, String name, RecipientType type) {
		this.receivers.add(new Receiver(email, name, type));
		return this;
	}

	public void markInvalidReceivers(List<String> invalid) {
		for (Receiver receiver : receivers) {
			if (invalid.indexOf(receiver.getEmail()) >= 0) {
				receiver.markInvalid();
			}
		}
	}

	public Mail setHTMLContent(String content) {
		this.htmlContent = content;
		return this;
	}

	public Mail setTextContent(String content) {
		this.textContent = content;
		return this;
	}

	public Mail addAttachment(File file, String mimeType) throws MailException {
		addAttachment(file, file.getName(), mimeType);
		return this;
	}

	public Mail addAttachment(File file, String name, String mimeType) throws MailException {
		if (null == mimeType) {
			mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file);
		}
		try {
			this.attachments.add(new AttachmentImpl(new FileInputStream(file), name, mimeType));
		} catch (FileNotFoundException e) {
			throw new MailException("error adding attachment", e);
		}
		return this;
	}

	public Mail addAttachment(InputStream is, String name, String mimeType) throws MailException {
		this.attachments.add(new AttachmentImpl(is, name, mimeType));
		return this;
	}

	public boolean hasAttachments() {
		return getAttachments().size() > 0;
	}

	public Mail setSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public Mail setFrom(Address from) {
		this.from = from;
		return this;
	}

	public Mail setFrom(String from) {
		setFrom(from, null);
		return this;
	}

	public Mail setFrom(String from, String name) {
		this.from = new Address(from, name);
		return this;
	}

	public Mail addReplyTo(String email) {
		this.replyTo.add(new Receiver(email, (RecipientType) null));
		return this;
	}

	public Mail addReplyTo(String email, String name) {
		this.replyTo.add(new Receiver(email, name, (RecipientType) null));
		return this;
	}

	public List<Receiver> getReceivers() {
		return receivers;
	}

	public List<Receiver> getReplyTo() {
		return replyTo;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public String getFrom() {
		return from == null ? null : from.getEmail();
	}

	public Address getFromAddress() {
		return from;
	}

	public String getSubject() {
		return subject;
	}

	public String getHtmlContent() {
		return htmlContent;
	}

	public String getTextContent() {
		return textContent;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String toString() {
		String sep = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("Subject: " + subject + sep);
		sb.append("From: " + from + sep);
		sb.append("Receivers: " + receivers + sep);
		sb.append("ReplyTo: " + replyTo + sep);
		sb.append("------------------------------------------------------------------" + sep);
		sb.append("Content (Text):" + sep + textContent + sep);
		sb.append("------------------------------------------------------------------" + sep);
		sb.append("Content (HTML):" + sep + htmlContent + sep);
		sb.append("------------------------------------------------------------------" + sep);
		sb.append("Attachments:" + sep + attachments + sep);
		return sb.toString();
	}

	public boolean hasReceivers() {
		return !getReceivers().isEmpty();
	}

}
