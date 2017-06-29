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
package org.appng.core.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.appng.core.domain.SubjectImpl;

public class PasswordHandlerTest {

	protected static final String PASSWORD = "myVeryStrongPassword";
	protected static final String EMAIL = "info@appng.org";
	protected static final String SHA1_SALT = "6EzR7iXOEOU=";
	protected static final String SHA1_PW_RESET_DIGEST = "BWgMxpQ5/75Fahxox6qlkWCI4Ns=";
	protected final SubjectImpl subject = new SubjectImpl();

	protected void testIsValidPasswordResetDigest(PasswordHandler handler) {
		subject.setEmail(EMAIL);
		subject.setSalt(SHA1_SALT);
		assertTrue(handler.isValidPasswordResetDigest(SHA1_PW_RESET_DIGEST));
	}

	protected void testIsInvalidPasswordResetDigest(PasswordHandler handler) {
		subject.setEmail(EMAIL);
		subject.setSalt(SHA1_SALT.substring(1));
		assertFalse(handler.isValidPasswordResetDigest(SHA1_PW_RESET_DIGEST));
	}

}
