/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.appng.api.MessageParam;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.auth.PasswordPolicy.ValidationResult;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;
import org.appng.api.support.PropertyHolder;
import org.appng.core.service.PropertySupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.passay.AllowedCharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.HistoryRule;
import org.passay.LengthRule;
import org.passay.UsernameRule;
import org.passay.WhitespaceRule;

public class ConfigurablePasswordPolicyTest {

	PasswordPolicy policy = new ConfigurablePasswordPolicy();

	@Before
	public void setup() {
		policy.configure(null);
	}

	@Test
	public void testGenerate() {
		for (int i = 0; i < 1000; i++) {
			String password = policy.generatePassword();
			Assert.assertTrue(
					policy.validatePassword("johndoe", "test".toCharArray(), password.toCharArray()).isValid());
		}
	}

	@Test
	public void testGroupsMissing() {
		PasswordPolicy policy = new ConfigurablePasswordPolicy();
		List<Property> properties = new ArrayList<>();
		SimpleProperty configurablePasswordPolicy = new SimpleProperty(
				PropertySupport.PREFIX_PLATFORM + "configurablePasswordPolicy", null);
		configurablePasswordPolicy.setClob("numCharacterGroups = 3");
		properties.add(configurablePasswordPolicy);
		PropertyHolder platformProperties = new PropertyHolder(PropertySupport.PREFIX_PLATFORM, properties);
		policy.configure(platformProperties);

		ValidationResult validated = policy.validatePassword("john", "test".toCharArray(), "TEST1234".toCharArray());
		MessageParam[] messages = validated.getMessages();
		Assert.assertTrue(messages.length == 3);
		Assert.assertEquals("ConfigurablePasswordPolicy.INSUFFICIENT_LOWERCASE", messages[0].getMessageKey());
		Assert.assertEquals("ConfigurablePasswordPolicy.INSUFFICIENT_SPECIAL", messages[1].getMessageKey());
		Assert.assertEquals("ConfigurablePasswordPolicy.INSUFFICIENT_CHARACTERISTICS", messages[2].getMessageKey());
	}

	@Test
	public void testLowerOnly() {
		PasswordPolicy policy = new ConfigurablePasswordPolicy();
		List<Property> properties = new ArrayList<>();
		SimpleProperty configurablePasswordPolicy = new SimpleProperty(
				PropertySupport.PREFIX_PLATFORM + "configurablePasswordPolicy", null);
		configurablePasswordPolicy.setClob("minUppercase=0\rminDigits=0\rminSpecialChars=0");
		properties.add(configurablePasswordPolicy);
		PropertyHolder platformProperties = new PropertyHolder(PropertySupport.PREFIX_PLATFORM, properties);
		policy.configure(platformProperties);

		ValidationResult validated = policy.validatePassword("john", "test".toCharArray(), "testtest".toCharArray());
		Assert.assertTrue(validated.isValid());
	}

	@Test
	public void testPasswords() {

		String username = "johndoe";
		String currentPassword = "test";

		// contains invalid character 'ß'
		assertFirstError(AllowedCharacterRule.ERROR_CODE, username, currentPassword, "TEst12!!ß");

		// contains space
		assertFirstError(WhitespaceRule.ERROR_CODE, username, currentPassword, "TEst12!! ");

		// contains username
		assertFirstError(UsernameRule.ERROR_CODE, username, currentPassword, username + "12!O");

		// equals last password
		assertFirstError(HistoryRule.ERROR_CODE, username, "Test123!!", "Test123!!");

		// only 1 of 4 character groups
		String noUpperCase = EnglishCharacterData.UpperCase.getErrorCode();
		String noLowerCase = EnglishCharacterData.LowerCase.getErrorCode();
		String noDigit = EnglishCharacterData.Digit.getErrorCode();
		String noSpecial = EnglishCharacterData.Special.getErrorCode();

		assertFirstError(noUpperCase, username, currentPassword, "testtest");
		assertFirstError(noLowerCase, username, currentPassword, "TESTTEST");
		assertFirstError(noLowerCase, username, currentPassword, "12345678");
		assertFirstError(noLowerCase, username, currentPassword, "!!!!!!!!");

		// only 2 of 4 character groups
		assertFirstError(noDigit, username, currentPassword, "testTEST");
		assertFirstError(noUpperCase, username, currentPassword, "test1234");
		assertFirstError(noUpperCase, username, currentPassword, "test!!!!");
		assertFirstError(noLowerCase, username, currentPassword, "TEST1234");
		assertFirstError(noLowerCase, username, currentPassword, "TEST!!!!");
		assertFirstError(noLowerCase, username, currentPassword, "1234!!!!");

		// only 3 of 4 character groups
		assertFirstError(noSpecial, username, currentPassword, "testTEST12");
		assertFirstError(noSpecial, username, currentPassword, "testTEST12");
		assertFirstError(noDigit, username, currentPassword, "testTEST!!");
		assertFirstError(noUpperCase, username, currentPassword, "test1234!!");
		assertFirstError(noLowerCase, username, currentPassword, "TEST1234!!");

		// to short
		String toShort = LengthRule.ERROR_CODE_MIN;
		assertFirstError(toShort, username, currentPassword, "Test!12");
		assertFirstError(toShort, username, currentPassword, "Test12!");
		assertFirstError(toShort, username, currentPassword, "12Test!");
		assertFirstError(toShort, username, currentPassword, "12!Test");
		assertFirstError(toShort, username, currentPassword, "!Test12");
		assertFirstError(toShort, username, currentPassword, "!12Test");

		// OK
		Assert.assertTrue(
				policy.validatePassword(username, currentPassword.toCharArray(), "teST12!!".toCharArray()).isValid());
		Assert.assertTrue(
				policy.validatePassword(username, currentPassword.toCharArray(), "teST!!12".toCharArray()).isValid());
		Assert.assertTrue(
				policy.validatePassword(username, currentPassword.toCharArray(), "TEst!!12".toCharArray()).isValid());
		Assert.assertTrue(
				policy.validatePassword(username, currentPassword.toCharArray(), "TEst12!!".toCharArray()).isValid());
		Assert.assertTrue(
				policy.validatePassword(username, currentPassword.toCharArray(), "12!!teST".toCharArray()).isValid());
		Assert.assertTrue(
				policy.validatePassword(username, currentPassword.toCharArray(), "12!!TEst".toCharArray()).isValid());
		Assert.assertTrue(
				policy.validatePassword(username, currentPassword.toCharArray(), "!!12teST".toCharArray()).isValid());
		Assert.assertTrue(
				policy.validatePassword(username, currentPassword.toCharArray(), "!!12TEst".toCharArray()).isValid());

	}

	private void assertFirstError(String error, String username, String currentPassword, String password) {
		ValidationResult validated = policy.validatePassword(username, currentPassword.toCharArray(),
				password.toCharArray());
		Assert.assertEquals(ConfigurablePasswordPolicy.class.getSimpleName() + "." + error,
				validated.getMessages()[0].getMessageKey());
	}

}
