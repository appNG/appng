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
package org.appng.mail.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.appng.mail.Attachment;
import org.appng.mail.Mail;
import org.appng.mail.Mail.RecipientType;
import org.appng.mail.MailException;
import org.appng.mail.MailTransport;
import org.appng.mail.Receiver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultTransport implements MailTransport {

	protected static final String TEXT_HTML = "text/html";
	protected static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
	protected static final String UTF_8 = "UTF-8";
	protected static final String MIXED = "mixed";
	protected static final String ALTERNATIVE = "alternative";

	public static final String HOST = "mail.smtp.host";
	public static final String PORT = "mail.smtp.port";

	protected Session session;
	protected Properties p;
	protected boolean disableSend = false;
	protected boolean debug = false;

	protected DefaultTransport() {

	}

	public DefaultTransport(Session session) {
		this.session = session;
	}

	public DefaultTransport(String host, int port) {
		this.p = new Properties();
		p.put(HOST, host);
		p.put(PORT, port);
		this.session = Session.getInstance(p);
	}

	public DefaultTransport(Properties props) {
		this.p = props;
		this.session = Session.getInstance(p);
	}

	public DefaultTransport(Properties props, String user, String password) {
		this.p = props;
		final PasswordAuthentication pa = new PasswordAuthentication(user, password);
		Authenticator authenticator = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return pa;
			}
		};
		this.session = Session.getInstance(p, authenticator);
	}

	public Mail createMail() {
		return new MailImpl();
	}

	public void send(Mail mail) throws MailException {
		List<String> invalid = new ArrayList<>();
		try {
			send(mail, invalid);
		} catch (MailException e) {
			if (!invalid.isEmpty()) {
				mail.markInvalidReceivers(invalid);
			}
			throw e;
		}
	}

	public String getMailAsString(Mail mail) {
		return mail.toString();
	}

	private String getLineBreak() {
		return System.getProperty("line.separator");
	}

	protected void send(Mail mail, final Collection<String> invalidAdresses) throws MailException {
		if (isEmpty(mail.getFrom())) {
			throw new MailException("e-mail can not be send because it has no sender!");
		}
		if (isEmpty(mail.getSubject())) {
			throw new MailException("e-mail can not be send because it has no subject!");
		}
		if (!mail.hasReceivers()) {
			throw new MailException("e-mail can not be send because it has no receivers!");
		}

		if (disableSend) {
			LOGGER.info("sending is disabled, not sending message{}{}", getLineBreak(), getMailAsString(mail));
		} else {
			try {
				Message message = buildMessage(mail);
				Transport.send(message);
			} catch (Exception e) {
				if (e instanceof MailException) {
					throw (MailException) e;
				} else if (e instanceof SendFailedException) {
					Address[] invalidAddresses = ((SendFailedException) e).getInvalidAddresses();
					if (null != invalidAddresses) {
						for (Address address : invalidAddresses) {
							String email = ((InternetAddress) address).getAddress();
							if (null != invalidAddresses) {
								invalidAdresses.add(email);
							}
							LOGGER.warn("invalid address: '{}'", email);
						}
					}
				}
				throw new MailException(e);
			}
		}
	}

	private boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	protected Message buildMessage(Mail mail) throws MailException {
		session.setDebug(debug);
		Message mimeMessage = new MimeMessage(session);
		return buildMessage(mimeMessage, mail);
	}

	protected Message buildMessage(Message mimeMessage, Mail mail) throws MailException {
		try {
			for (Receiver receiver : mail.getReceivers()) {
				if (receiver.isValid()) {
					RecipientType type = receiver.getType();
					javax.mail.Message.RecipientType messageType = getRealType(type);
					mimeMessage.addRecipient(messageType, toAddress(receiver));
				}
			}
			List<Receiver> replyTo = mail.getReplyTo();
			Address[] replyArr = new Address[replyTo.size()];
			for (int i = 0; i < replyArr.length; i++) {
				replyArr[i] = toAddress(replyTo.get(i));
			}
			mimeMessage.setReplyTo(replyArr);

			mimeMessage.setSubject(mail.getSubject());
			mimeMessage.setFrom(toAddress(mail.getFromAddress()));
			mimeMessage.setSentDate(new Date());

			MimeMultipart multipartAlternative = new MimeMultipart(ALTERNATIVE);

			String textContent = mail.getTextContent();
			String encoding = mail.getEncoding() == null ? UTF_8 : mail.getEncoding();
			if (null != textContent) {
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setText(textContent, encoding);
				multipartAlternative.addBodyPart(bodyPart);
			}

			String htmlContent = mail.getHtmlContent();
			if (null != htmlContent) {
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(htmlContent, TEXT_HTML + "; charset=" + encoding);
				bodyPart.setHeader(CONTENT_TRANSFER_ENCODING, "7bit");
				multipartAlternative.addBodyPart(bodyPart);
			}

			boolean hasAttachments = mail.hasAttachments();
			if (hasAttachments) {
				MimeMultipart multipartMixed = new MimeMultipart(MIXED);
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(multipartAlternative);
				multipartMixed.addBodyPart(bodyPart);
				for (Attachment attachment : mail.getAttachments()) {
					addAttachment(multipartMixed, attachment);
				}
				mimeMessage.setContent(multipartMixed);
			} else {
				mimeMessage.setContent(multipartAlternative);
			}

			mimeMessage.saveChanges();
			return mimeMessage;
		} catch (Exception e) {
			throw new MailException(e);
		}
	}

	private javax.mail.Message.RecipientType getRealType(RecipientType type) {
		switch (type) {
		case CC:
			return javax.mail.Message.RecipientType.CC;
		case BCC:
			return javax.mail.Message.RecipientType.BCC;
		default:
			return javax.mail.Message.RecipientType.TO;
		}
	}

	private void addAttachment(MimeMultipart multipartMixed, Attachment attachment) throws Exception {
		ByteArrayDataSource ds = new ByteArrayDataSource(attachment.getInputStream(), attachment.getMimeType());
		ds.setName(attachment.getName());
		MimeBodyPart attachmentPart = new MimeBodyPart();
		attachmentPart.setDataHandler(new DataHandler(ds));
		attachmentPart.setFileName(attachment.getName());
		attachmentPart.setDisposition(Part.ATTACHMENT);
		multipartMixed.addBodyPart(attachmentPart);
	}

	public void disableSend() {
		this.disableSend = true;
	}

	public boolean isDisableSend() {
		return disableSend;
	}

	public void setDisableSend(boolean disableSend) {
		this.disableSend = disableSend;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public Address toAddress(org.appng.mail.Address address) throws UnsupportedEncodingException {
		return new InternetAddress(address.getEmail(), address.getName());
	}
}
