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
package org.appng.core.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class Sha1PasswordHandlerTest extends PasswordHandlerTest {

	private static final String SHA1_DIGEST = "J3IAPwfD2uOg98+rooFUN1ItUcE=";

	@Test
	public void testSavePassword() {
		PasswordHandler handler = new Sha1PasswordHandler(subject);
		handler.applyPassword(PASSWORD);
		Assert.assertNotNull(subject.getDigest());
		Assert.assertTrue(!subject.getDigest().startsWith(BCryptPasswordHandler.getPrefix()));
		Assert.assertNotNull(subject.getSalt());
	}

	@Test
	public void testValidPassword() {
		subject.setDigest(SHA1_DIGEST);
		subject.setSalt(SHA1_SALT);
		PasswordHandler handler = new Sha1PasswordHandler(subject);
		assertTrue(handler.isValidPassword(PASSWORD));
	}

	@Test
	public void testInvalidPassword() {
		subject.setDigest(SHA1_DIGEST.substring(1));
		subject.setSalt(SHA1_SALT);
		PasswordHandler handler = new Sha1PasswordHandler(subject);
		assertFalse(handler.isValidPassword(PASSWORD));
	}

	@Test
	public void testGetPasswordResetDigest() {
		subject.setSalt(SHA1_SALT);
		subject.setEmail(EMAIL);
		PasswordHandler handler = new Sha1PasswordHandler(subject);
		String digest = handler.calculatePasswordResetDigest();
		Assert.assertEquals(SHA1_SALT, subject.getSalt());
		Assert.assertEquals(SHA1_PW_RESET_DIGEST, digest);
	}

	@Test
	public void testIsValidPasswordResetDigest() {
		testIsValidPasswordResetDigest(new Sha1PasswordHandler(subject));
	}

	@Test
	public void testIsInvalidPasswordResetDigest() {
		testIsInvalidPasswordResetDigest(new Sha1PasswordHandler(subject));
	}

}
