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
	public void testComplexRegex() {
		// use ASCII hex ranges
		String expression = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)" //
				+ "(?=.*[" // use positive lookahead
				+ "\\x21-\\x2f" // "!" to "/" (char code 33 - 47)
				+ "\\x3A-\\x40" // ":" to "@" (char code 58 - 64)
				+ "\\x5b-\\x60" // "[" to "`" (char code 91 - 96)
				+ "\\x7b-\\x7e" // "{" to "~" (char code 123 - 126)
				+ "])" // end lookahead
				+ "[\\x21-\\x7e]" // "!" to "~" (char code 33 - 126)
				+ "{8,}$"; // 8 or more
		PasswordPolicy policy = new DefaultPasswordPolicy(expression, "dummy");

		// only 1 of 4 character groups
		Assert.assertFalse(policy.isValidPassword("testtest".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("TESTTEST".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("12345678".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("!!!!!!!!".toCharArray()));

		// only 2 of 4 character groups
		Assert.assertFalse(policy.isValidPassword("testTEST".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("test1234".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("test!!!!".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("TEST1234".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("TEST!!!!".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("1234!!!!".toCharArray()));

		// only 3 of 4 character groups
		Assert.assertFalse(policy.isValidPassword("testTEST12".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("testTEST!!".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("test1234!!".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("TEST1234!!".toCharArray()));

		// to short
		Assert.assertFalse(policy.isValidPassword("Test!12".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("Test12!".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("12Test!".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("12!Test".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("!Test12".toCharArray()));
		Assert.assertFalse(policy.isValidPassword("!12Test".toCharArray()));

		// OK
		Assert.assertTrue(policy.isValidPassword("teST12!!".toCharArray()));
		Assert.assertTrue(policy.isValidPassword("teST!!12".toCharArray()));
		Assert.assertTrue(policy.isValidPassword("TEst!!12".toCharArray()));
		Assert.assertTrue(policy.isValidPassword("TEst12!!".toCharArray()));
		Assert.assertTrue(policy.isValidPassword("12!!teST".toCharArray()));
		Assert.assertTrue(policy.isValidPassword("12!!TEst".toCharArray()));
		Assert.assertTrue(policy.isValidPassword("!!12teST".toCharArray()));
		Assert.assertTrue(policy.isValidPassword("!!12TEst".toCharArray()));

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
