/*
 * Copyright 2011-2019 the original author or authors.
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

public class BCryptPasswordHandlerTest extends PasswordHandlerTest {

	private static final String BCRYPT_DIGEST = "$2a$13$uNyTfveWdKj05QUu14ess.Df7JCCw0ONmEGScG4V1rIQcSA3naAGK";

	@Test
	public void testSavePassword() {
		PasswordHandler handler = new BCryptPasswordHandler(subject);
		handler.savePassword(PASSWORD);
		Assert.assertNotNull(subject.getDigest());
		Assert.assertTrue(subject.getDigest().startsWith(BCryptPasswordHandler.getPrefix()));
		Assert.assertNull(subject.getSalt());
	}

	@Test
	public void testValidPassword() {
		subject.setDigest(BCRYPT_DIGEST);
		PasswordHandler handler = new BCryptPasswordHandler(subject);
		assertTrue(handler.isValidPassword(PASSWORD));
	}

	@Test
	public void testInvalidPassword() {
		subject.setDigest(BCRYPT_DIGEST.substring(0, BCRYPT_DIGEST.length() - 1));
		PasswordHandler handler = new BCryptPasswordHandler(subject);
		assertFalse(handler.isValidPassword(PASSWORD));
	}

	@Test
	public void testGetPasswordResetDigest() {
		subject.setSalt(null);
		subject.setEmail(EMAIL);
		PasswordHandler handler = new BCryptPasswordHandler(subject);
		String digest = handler.getPasswordResetDigest();
		Assert.assertNotNull(subject.getSalt());
		assertTrue(!digest.startsWith(BCryptPasswordHandler.getPrefix()));
	}

	@Test
	public void testIsValidPasswordResetDigest() {
		testIsValidPasswordResetDigest(new BCryptPasswordHandler(subject));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIsInvalidPasswordResetDigest() {
		testIsInvalidPasswordResetDigest(new BCryptPasswordHandler(subject));
	}

}
