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
package org.appng.api.support;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.StaticMessageSource;

public class MessageSourceChainTest {

	private static final String KEY1 = "key1";
	private static final String KEY2 = "key2";
	private static final String KEY3 = "key3";
	private static final String KEY4 = "key4";
	private static final String VALUE1 = "value1-1";
	private static final String VALUE2 = "value2-1";
	private static final String VALUE3 = "value3-1";
	private static final String VALUE_DEFAULT = "default-1";

	private MessageSource ms;
	private Locale locale = Locale.ENGLISH;
	private String defaultMessage = "default-{0}";
	private Object[] args = new Object[] { 1 };

	@Before
	public void setup() {
		StaticMessageSource source1 = new StaticMessageSource();
		source1.addMessage(KEY1, locale, "value1-{0}");

		StaticMessageSource source2 = new StaticMessageSource();
		source1.addMessage(KEY2, locale, "value2-{0}");

		StaticMessageSource source3 = new StaticMessageSource();
		source1.addMessage(KEY3, locale, "value3-{0}");

		ms = new MessageSourceChain(source1, source2, source3);
	}

	@Test
	public void testGetMessage() {
		Assert.assertEquals(VALUE1, ms.getMessage(KEY1, args, locale));
		Assert.assertEquals(VALUE2, ms.getMessage(KEY2, args, locale));
		Assert.assertEquals(VALUE3, ms.getMessage(KEY3, args, locale));
	}

	@Test(expected = NoSuchMessageException.class)
	public void testGetMessageException() {
		ms.getMessage(KEY4, args, locale);
	}

	@Test
	public void testGetMessageWithDefault() {
		Assert.assertEquals(VALUE1, ms.getMessage(KEY1, args, defaultMessage, locale));
		Assert.assertEquals(VALUE2, ms.getMessage(KEY2, args, defaultMessage, locale));
		Assert.assertEquals(VALUE3, ms.getMessage(KEY3, args, defaultMessage, locale));
		Assert.assertEquals(VALUE_DEFAULT, ms.getMessage(KEY4, args, defaultMessage, locale));
		Assert.assertNull(ms.getMessage(KEY4, args, null, locale));
	}

	@Test
	public void testGetMessageFromMessageSourceResolvable() {
		MessageSourceResolvable r1 = getResolvable(KEY1, null);
		Assert.assertEquals(VALUE1, ms.getMessage(r1, locale));

		MessageSourceResolvable r2 = getResolvable(KEY2, null);
		Assert.assertEquals(VALUE2, ms.getMessage(r2, locale));

		MessageSourceResolvable r3 = getResolvable(KEY3, null);
		Assert.assertEquals(VALUE3, ms.getMessage(r3, locale));

		MessageSourceResolvable r4 = getResolvable(KEY4, defaultMessage);
		Assert.assertEquals(VALUE_DEFAULT, ms.getMessage(r4, locale));
	}

	@Test(expected = NoSuchMessageException.class)
	public void testGetMessageFromMessageSourceResolvableWithException() {
		ms.getMessage(getResolvable(KEY4, null), locale);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testNoMessageSources() {
		new MessageSourceChain().getMessage(KEY1, args, locale);
	}

	public MessageSourceResolvable getResolvable(final String key, final String defaultMessage) {
		MessageSourceResolvable resolvable = new MessageSourceResolvable() {

			public String getDefaultMessage() {
				return defaultMessage;
			}

			public String[] getCodes() {
				return new String[] { key };
			}

			public Object[] getArguments() {
				return args;
			}
		};
		return resolvable;
	}
}
