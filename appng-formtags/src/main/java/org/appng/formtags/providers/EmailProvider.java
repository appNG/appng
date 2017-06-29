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
package org.appng.formtags.providers;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.appng.forms.FormUpload;
import org.appng.formtags.Form;
import org.appng.formtags.FormElement;
import org.appng.formtags.FormProcessProvider;
import org.appng.mail.Mail;
import org.appng.mail.Mail.RecipientType;
import org.appng.mail.MailException;
import org.appng.mail.MailTransport;
import org.appng.mail.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FormProcessProvider} that send's and e-mail to some receivers. All the properties of the e-mail are being
 * set via the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
 * See the following property-keys for details:
 * <ul>
 * <li>{@link #SENDER}
 * <li>{@link #SENDER_NAME}
 * <li>{@link #SUBJECT}
 * <li>{@link #RECEIVER_TO}
 * <li>{@link #RECEIVER_CC}
 * <li>{@link #RECEIVER_BCC}
 * <li>{@link #RECEIVER_DEBUG}
 * <li>{@link #REPLY_TO}
 * <li>{@link #EMAIL_CONTENT_TEXT}
 * <li>{@link #EMAIL_CONTENT_HTML}
 * </ul>
 * It is also possible to write a message to the given {@link Writer}, see {@link #CONTENT} and {@link #ERROR_MESSAGE}.
 * 
 * @author Matthias Müller
 * 
 */
public class EmailProvider implements FormProcessProvider {

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value must contain a comma-separated list of e-mail addresses to send the mail with {@link RecipientType#TO}
	 */
	public static final String RECEIVER_TO = "receiver";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value can contain a comma-separated list of e-mail addresses to send the mail with {@link RecipientType#CC}
	 */
	public static final String RECEIVER_CC = "receiverCC";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value can contain a comma-separated list of e-mail addresses to send the mail with {@link RecipientType#BCC}
	 */
	public static final String RECEIVER_BCC = "receiverBCC";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value must contain the sender's e-mail address
	 */
	public static final String SENDER = "sender";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value can contain the sender's name
	 */
	public static final String SENDER_NAME = "senderName";

	/** */
	public static final String SEND_DISABLED = "sendDisabled";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value can contain a comma-separated list of e-mail addresses to reply-to
	 */
	public static final String REPLY_TO = "replyTo";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value must contain the subject for the e-mail
	 */
	public static final String SUBJECT = "subject";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value can contain a comma-separated list of e-mail addresses to which the e-mail will be send (instead of the
	 * real receivers)
	 */
	public static final String RECEIVER_DEBUG = "receiverDebug";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value can contain the HTML-content for the e-mail to send. One or both of text or HTML-content should be set.
	 * 
	 * @see #EMAIL_CONTENT_TEXT
	 */
	public static final String EMAIL_CONTENT_HTML = "emailContentHtml";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * Value can contain the text-content for the e-mail to send. One or both of text or HTML-content should be set.
	 * 
	 * @see #EMAIL_CONTENT_HTML
	 */
	public static final String EMAIL_CONTENT_TEXT = "emailContentText";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * If the value is {@code true}, the file attachments of the {@link Form} will be added to the e-mail.
	 */
	public static final String ATTACHMENTS = "attachments";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * The value may contain the message that is written to the {@link Writer} passed to
	 * {@link #onFormSuccess(Writer, Form, Map)}, in case the e-mail was successfully sent.
	 */
	public static final String CONTENT = "content";

	/**
	 * Key for the {@code properties}-{@link Map} passed to {@link #onFormSuccess(Writer, Form, Map)}.<br/>
	 * The value may contain the message that is written to the {@link Writer} passed to
	 * {@link #onFormSuccess(Writer, Form, Map)}, in case an error occurs while sending the e-mail.
	 */
	public static final String ERROR_MESSAGE = "errorMessage";

	private static final String TRUE = "true";
	private static final String COMMA = ",";
	private static final Logger log = LoggerFactory.getLogger(EmailProvider.class);
	private MailTransport mailTransport;

	public EmailProvider(MailTransport mailTransport) {
		this.mailTransport = mailTransport;
	}

	public void onFormSuccess(Writer writer, Form form, Map<String, Object> properties) {
		String senderEMail = (String) properties.get(SENDER);
		String senderName = (String) properties.get(SENDER_NAME);
		String receiverEMail = (String) properties.get(RECEIVER_TO);
		String receiverEMailDebug = (String) properties.get(RECEIVER_DEBUG);
		String ccReceiverEMail = (String) properties.get(RECEIVER_CC);
		String bccReceiverEMail = (String) properties.get(RECEIVER_BCC);
		String replyTo = (String) properties.get(REPLY_TO);
		String subject = (String) properties.get(SUBJECT);
		String content = (String) properties.get(CONTENT);
		String errorMessage = (String) properties.get(ERROR_MESSAGE);

		String textEmail = (String) properties.get(EMAIL_CONTENT_TEXT);
		String htmlEmail = (String) properties.get(EMAIL_CONTENT_HTML);

		boolean sendDisabled = TRUE.equalsIgnoreCase((String) properties.get(SEND_DISABLED));
		mailTransport.setDisableSend(sendDisabled);
		log.debug("sending emails is {}", sendDisabled ? "disabled" : "enabled");

		boolean addAttachments = TRUE.equalsIgnoreCase((String) properties.get(ATTACHMENTS));

		List<Receiver> debugReceivers = getDebugReceivers(receiverEMailDebug);

		try {

			if (StringUtils.isEmpty(senderEMail)) {
				throw new MailException("parameter '" + SENDER + "' is invalid: " + senderEMail);
			}
			if (StringUtils.isEmpty(subject)) {
				throw new MailException("parameter '" + SUBJECT + "' is invalid: " + subject);
			}

			Mail mail = mailTransport.createMail();

			mail.setSubject(subject);
			mail.setFrom(senderEMail, senderName);
			if (StringUtils.isNotBlank(replyTo)) {
				addReplies(mail, replyTo);
			}

			if (StringUtils.isNotBlank(textEmail)) {
				mail.setTextContent(textEmail);
			}
			if (StringUtils.isNotBlank(htmlEmail)) {
				mail.setHTMLContent(htmlEmail);
			}

			addReceivers(mail, receiverEMail, org.appng.mail.Mail.RecipientType.TO);
			addReceivers(mail, ccReceiverEMail, org.appng.mail.Mail.RecipientType.CC);
			addReceivers(mail, bccReceiverEMail, org.appng.mail.Mail.RecipientType.BCC);

			if (addAttachments) {
				addAttachements(mail, form);
			}

			String mailAsString = mailTransport.getMailAsString(mail);
			if (debugReceivers.isEmpty()) {
				log.debug("sending mail: " + mailAsString);
				mailTransport.send(mail);
			} else {
				Mail debugEmail = mailTransport.createMail();
				for (Receiver receiver : debugReceivers) {
					debugEmail.addReceiver(receiver);
				}
				debugEmail.setSubject(subject);
				debugEmail.setFrom(senderEMail, senderName);
				debugEmail.setTextContent(mailAsString);
				addAttachements(debugEmail, form);

				log.debug("sending debug-mail: " + mailTransport.getMailAsString(debugEmail));
				mailTransport.send(debugEmail);
			}
			if (StringUtils.isNotBlank(content)) {
				writer.write(content);
			}
		} catch (Exception e) {
			if (StringUtils.isNotBlank(errorMessage)) {
				try {
					writer.write(errorMessage);
				} catch (IOException ioe) {
					log.error("unable to append errorMessage", ioe);
				}
			}
			log.error("Error occured while trying to send E-Mail.", e);
		}
	}

	private void addReplies(Mail mail, String replyTo) {
		for (String receiver : replyTo.split(COMMA)) {
			mail.addReplyTo(receiver.trim());
		}
	}

	private void addAttachements(Mail mail, Form form) throws MailException {
		for (FormElement formElement : form.getFormData().getElements()) {
			List<FormUpload> formUploads = formElement.getFormUploads();
			if (null != formUploads) {
				for (FormUpload formUpload : formUploads) {
					mail.addAttachment(formUpload.getFile(), formUpload.getOriginalFilename(),
							formUpload.getContentType());
				}
			}
		}
	}

	private void addReceivers(Mail mail, String receiverEmails, org.appng.mail.Mail.RecipientType type) {
		if (isValidReceiver(receiverEmails)) {
			for (String receiver : receiverEmails.split(COMMA)) {
				mail.addReceiver(new Receiver(receiver.trim(), type));
			}
		}
	}

	public boolean isValidReceiver(String receiverEmails) {
		return null != receiverEmails && receiverEmails.length() > 0;
	}

	private List<Receiver> getDebugReceivers(String receivers) {
		List<Receiver> debugReceivers = new ArrayList<Receiver>();
		if (isValidReceiver(receivers)) {
			for (String receiver : receivers.split(COMMA)) {
				debugReceivers.add(new Receiver(receiver.trim(), org.appng.mail.Mail.RecipientType.TO));
			}
		}
		return debugReceivers;
	}
}
