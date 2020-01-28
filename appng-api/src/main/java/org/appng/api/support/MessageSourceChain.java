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
package org.appng.api.support;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

/**
 * A {@link MessageSource} that wraps multiple {@link MessageSource} and processes them in sequence. The message is
 * always taken from the first {@link MessageSource} that returns a non-{@code null} value (if any).
 * 
 * @author Matthias Müller
 * 
 */
public class MessageSourceChain implements MessageSource {

	private MessageSource[] messageSources;

	public MessageSourceChain() {

	}

	public MessageSourceChain(MessageSource... messageSources) {
		this.messageSources = messageSources;
	}

	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		check();
		String message = null;
		for (int i = 0; i < messageSources.length && message == null; i++) {
			message = messageSources[i].getMessage(code, args, defaultMessage, locale);
		}
		return message;
	}

	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		check();
		String message = null;
		NoSuchMessageException ex = null;
		for (int i = 0; i < messageSources.length && message == null; i++) {
			try {
				message = messageSources[i].getMessage(code, args, locale);
			} catch (NoSuchMessageException nsme) {
				ex = nsme;
			}
		}
		if (null == message && null != ex) {
			throw ex;
		}
		return message;
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		check();
		String message = null;
		NoSuchMessageException ex = null;
		for (int i = 0; i < messageSources.length && message == null; i++) {
			try {
				message = messageSources[i].getMessage(resolvable, locale);
			} catch (NoSuchMessageException nsme) {
				ex = nsme;
			}
		}
		if (null == message && null != ex) {
			throw ex;
		}
		return message;
	}

	private void check() {
		if (messageSources == null || messageSources.length == 0) {
			throw new UnsupportedOperationException("MessageSourceChain must contain at least one MessageSource");
		}
	}

	public MessageSource[] getMessageSources() {
		return messageSources;
	}

	public void setMessageSources(MessageSource[] messageSources) {
		this.messageSources = messageSources;
	}

}
