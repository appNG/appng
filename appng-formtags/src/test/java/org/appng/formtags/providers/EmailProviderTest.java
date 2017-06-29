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

import java.io.File;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.appng.forms.FormUpload;
import org.appng.forms.impl.FormUploadBean;
import org.appng.formtags.Form;
import org.appng.formtags.FormElement;
import org.appng.formtags.FormElement.InputTag;
import org.appng.formtags.FormElement.InputType;
import org.appng.mail.Mail;
import org.appng.mail.MailException;
import org.appng.mail.MailTransport;
import org.appng.mail.Receiver;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class EmailProviderTest {

	@Test
	public void test() throws MailException, URISyntaxException {
		MailTransport mailtransport = Mockito.mock(MailTransport.class);
		Mail mail = Mockito.mock(Mail.class);
		Mockito.when(mailtransport.createMail()).thenReturn(mail);
		EmailProvider emailProvider = new EmailProvider(mailtransport);
		Map<String, Object> props = new HashMap<String, Object>();

		props.put(EmailProvider.SENDER, "john@doe.org");
		props.put(EmailProvider.SENDER_NAME, "John Doe");
		props.put(EmailProvider.SUBJECT, "Foobar");
		props.put(EmailProvider.EMAIL_CONTENT_HTML, "Some <bold>HTML</bold>");
		props.put(EmailProvider.EMAIL_CONTENT_TEXT, "Some text");
		props.put(EmailProvider.CONTENT, "some content");
		props.put(EmailProvider.RECEIVER_TO, "john@doe.org");
		props.put(EmailProvider.RECEIVER_CC, "jane@doe.org");
		props.put(EmailProvider.RECEIVER_BCC, "joe@doe.org, jason@doe.org");
		props.put(EmailProvider.REPLY_TO, "jim@doe.org");
		props.put(EmailProvider.ATTACHMENTS, "true");

		Form form = new Form();

		FormElement upload = form.getFormData().addFormElement();
		upload.setInputTag(InputTag.INPUT);
		upload.setInputType(InputType.FILE);
		upload.setName("upload");

		URL resource = getClass().getClassLoader().getResource("images/Jellyfish.jpg");
		File file = new File(resource.toURI());
		FormUpload formUploadBean = new FormUploadBean(file, file.getName(), "jpg", Arrays.asList("jpg"), 10000000l);
		upload.setFormUploads(Arrays.asList(formUploadBean));

		StringWriter writer = new StringWriter();
		emailProvider.onFormSuccess(writer, form, props);

		Mockito.verify(mail).setFrom((String) props.get(EmailProvider.SENDER),
				(String) props.get(EmailProvider.SENDER_NAME));
		Mockito.verify(mail).setSubject((String) props.get(EmailProvider.SUBJECT));
		Mockito.verify(mail).setHTMLContent((String) props.get(EmailProvider.EMAIL_CONTENT_HTML));
		Mockito.verify(mail).setTextContent((String) props.get(EmailProvider.EMAIL_CONTENT_TEXT));
		Mockito.verify(mail).addReplyTo((String) props.get(EmailProvider.REPLY_TO));
		Mockito.verify(mail, Mockito.times(4)).addReceiver(Mockito.any(Receiver.class));
		Assert.assertEquals(props.get(EmailProvider.CONTENT), writer.toString());
		Mockito.verify(mail).addAttachment(file, file.getName(), "jpg");
	}
}
