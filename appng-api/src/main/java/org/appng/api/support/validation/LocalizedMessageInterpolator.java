/*
 * Copyright 2011-2021 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.appng.api.model.Application;
import org.appng.api.support.MessageSourceChain;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.MessageSourceSupport;
import org.springframework.context.support.ResourceBundleMessageSource;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link javax.validation.MessageInterpolator} that is aware of a given {@link Locale} instead of using the default
 * one. Additionally, it uses an {@link Application}'s default {@link MessageSource} to retrieve validation messages.
 * 
 * @author Matthias Müller
 */
@Slf4j
public class LocalizedMessageInterpolator extends ResourceBundleMessageInterpolator {

	private static final String DEFAULT_VALIDATION_MESSAGES = "org.hibernate.validator.ValidationMessages";
	private static final List<String> VALIDATION_BASENAMES = Arrays.asList(USER_VALIDATION_MESSAGES,
			DEFAULT_VALIDATION_MESSAGES);
	private Locale locale;

	/**
	 * Creates a new {@code LocalizedMessageInterpolator} using the given {@link Locale} .
	 * 
	 * @param locale
	 *                      The {@link Locale} to use.
	 * @param messageSource
	 *                      An additional {@link MessageSource} to use. If this is an instance of
	 *                      {@link AbstractResourceBasedMessageSource},
	 *                      {@link AbstractResourceBasedMessageSource#getBasenameSet()} is being used to create a new
	 *                      {@link ResourceBundleMessageSource} with these base names. This is necessary because a the
	 *                      {@code messageSource} might use
	 *                      {@link MessageSourceSupport#setAlwaysUseMessageFormat(boolean)}, which can't properly be
	 *                      handled by {@link javax.validation.MessageInterpolator}.
	 */
	public LocalizedMessageInterpolator(Locale locale, final MessageSource messageSource) {
		super(getResourceBundleLocator(messageSource));
		if (null == locale) {
			throw new IllegalArgumentException("locale can not be null");
		}
		this.locale = locale;
	}

	private static ResourceBundleLocator getResourceBundleLocator(final MessageSource messageSource) {
		return new ResourceBundleLocator() {
			public ResourceBundle getResourceBundle(Locale locale) {
				MessageSource innerSource;
				if (messageSource instanceof AbstractResourceBasedMessageSource) {
					List<String> basenames = new ArrayList<>(
							((AbstractResourceBasedMessageSource) messageSource).getBasenameSet());
					basenames.addAll(VALIDATION_BASENAMES);
					innerSource = getResourceBundleMessageSource(basenames);
				} else {
					innerSource = new MessageSourceChain(messageSource,
							getResourceBundleMessageSource(VALIDATION_BASENAMES));
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Setting up MessageSourceResourceBundle with {}", innerSource);
				}
				return new MessageSourceResourceBundle(innerSource, locale);
			}

			private ResourceBundleMessageSource getResourceBundleMessageSource(List<String> basenames) {
				ResourceBundleMessageSource rbms = new ResourceBundleMessageSource();
				rbms.setFallbackToSystemLocale(false);
				rbms.setBasenames(basenames.toArray(new String[0]));
				rbms.setDefaultEncoding(StandardCharsets.UTF_8.name());
				return rbms;
			}
		};
	}

	@Override
	public String interpolate(String messageTemplate, Context context) {
		String interpolated = interpolate(messageTemplate, context, locale);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Interpolated template '{}', result is '{}'", messageTemplate, interpolated);
		}
		return interpolated;
	}

}
