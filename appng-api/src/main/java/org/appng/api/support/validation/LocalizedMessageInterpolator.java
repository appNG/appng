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
package org.appng.api.support.validation;

import java.util.Locale;
import java.util.ResourceBundle;

import org.appng.api.support.MessageSourceChain;
import org.appng.api.support.ResourceBundleMessageSource;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;

/**
 * A {@link javax.validation.MessageInterpolator} that is aware of a given {@link Locale} instead of using the default
 * {@link Locale}.
 * 
 * @author Matthias Müller
 */
public class LocalizedMessageInterpolator extends ResourceBundleMessageInterpolator {

	private static final String DEFAULT_VALIDATION_MESSAGES = "org.hibernate.validator.ValidationMessages";
	private Locale locale;

	/**
	 * Creates a new {@code LocalizedMessageInterpolator} using the given {@link Locale} .
	 * 
	 * @param locale
	 *            the {@link Locale} to use
	 * @param messageSource
	 *            an additional {@link MessageSource} to use
	 */
	public LocalizedMessageInterpolator(Locale locale, final MessageSource messageSource) {
		super(new ResourceBundleLocator() {
			public ResourceBundle getResourceBundle(Locale locale) {
				ResourceBundleMessageSource validationMessages = new ResourceBundleMessageSource();
				validationMessages.setFallbackToSystemLocale(false);
				validationMessages.setBasenames(USER_VALIDATION_MESSAGES, DEFAULT_VALIDATION_MESSAGES);
				MessageSource chain = new MessageSourceChain(messageSource, validationMessages);
				return new MessageSourceResourceBundle(chain, locale);
			}
		});
		if (null == locale) {
			throw new IllegalArgumentException("locale can not be null");
		}
		this.locale = locale;
	}

	@Override
	public String interpolate(String messageTemplate, Context context) {
		return interpolate(messageTemplate, context, locale);
	}

}
