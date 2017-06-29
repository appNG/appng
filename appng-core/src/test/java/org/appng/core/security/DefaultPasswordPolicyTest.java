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

import org.appng.api.auth.PasswordPolicy;
import org.junit.Assert;
import org.junit.Test;

public class DefaultPasswordPolicyTest {

	PasswordPolicy passwordPolicy = new DefaultPasswordPolicy();

	@Test
	public void testGenerate() {
		for (int i = 0; i < 1000; i++) {
			String password = passwordPolicy.generatePassword();
			assertValid(password);
		}
	}

	@Test
	public void test() {
		assertInvalid("abcde");
		assertInvalid("      ");
		assertInvalid("		");
		assertInvalid("Сардинские лакомства");
		assertInvalid("Сарди");
		assertInvalid("01234567890123456789012345678901234567890123456789012345678912345");

		assertValid("0123456789012345678901234567890123456789012345678901234567891234");
		assertValid("123456");
		assertValid("abcdef");
		assertValid("ABCDEF");
		assertValid("aBc4dE7");

		assertValid("Сардинскиелакомства");
		assertValid("²³'иел");
	}

	private void assertValid(String password) {
		Assert.assertTrue(password + " should be valid", passwordPolicy.isValidPassword(password.toCharArray()));
	}

	private void assertInvalid(String password) {
		Assert.assertFalse(password + " should be invalid", passwordPolicy.isValidPassword(password.toCharArray()));
	}
}
