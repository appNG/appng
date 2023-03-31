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
package org.appng.cli.commands.subject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.appng.api.BusinessException;
import org.appng.api.model.Subject;
import org.appng.api.model.UserType;
import org.appng.cli.commands.AbstractCommandTest;
import org.appng.core.security.BCryptPasswordHandler;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CommandCreateSubjectTest extends AbstractCommandTest {

	private String email = "ad@min.com";
	private String authName = "admin";
	private UserType type = UserType.LOCAL_USER;
	private String realName = "Admin Istrator";
	private String language = "de";
	private String description = "an admin";
	private String password = "Test12!$";

	public CreateSubject getCommand() {
		return null;
	}

	@Test
	@Override
	public void test() throws BusinessException {
		new CreateSubject(authName, realName, email, password, language, description, type, false).execute(cliEnv);
		validate();
	}

	@Test
	public void testPasswordHashed() throws BusinessException {
		email = "pass@word.com";
		authName = "preHashedPassword";
		new CreateSubject(authName, realName, email, "$2a$13$DTnwQBpfOrtqbji9wiy2HOsJ/lSz1kUqvajQ3SX/JodePmu6.ClHG",
				language, description, type, true).execute(cliEnv);
		validate();
	}

	@Test
	public void testInvalidPassword() {
		try {
			new CreateSubject("anotheruser", realName, email, "test", language, description, type, false)
					.execute(cliEnv);
			fail("Must throw BusinessException!");
		} catch (BusinessException e) {
			Assert.assertTrue(e.getMessage().contains("Password must contain 1 or more uppercase letters."));
			Assert.assertTrue(e.getMessage().contains("Password must contain 1 or more numbers."));
			Assert.assertTrue(e.getMessage().contains(
					"Password must contain 1 or more special characters. Allowed are !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"));
			Assert.assertTrue(e.getMessage().contains("Password must be at least 8 characters long"));
		}
	}

	@Test
	public void testLocalNoPassword() {
		try {
			new CreateSubject("anotheruser", realName, email, null, language, description, type, false).execute(cliEnv);
			fail("Must throw BusinessException!");
		} catch (BusinessException e) {
			assertEquals("-p is mandatory for type LOCAL_USER", e.getMessage());
		}
	}

	@Test
	public void testGroup() throws BusinessException {
		authName = "adminFromLdap";
		email = "adldap@min.com";
		type = UserType.GLOBAL_GROUP;
		new CreateSubject(authName, realName, email, null, language, description, type, false).execute(cliEnv);
		validate();
	}

	@Test
	public void testGroupWithPassword() throws BusinessException {
		try {
			new CreateSubject(authName, realName, email, password, language, description, UserType.GLOBAL_GROUP, false)
					.execute(cliEnv);
			fail("Must throw BusinessException!");
		} catch (BusinessException e) {
			assertEquals("-p is not allowed for type GLOBAL_GROUP", e.getMessage());
		}
	}

	@Test
	public void testUserExists() {
		try {
			new CreateSubject(authName, realName, email, password, language, description, type, false).execute(cliEnv);
			fail("Must throw BusinessException!");
		} catch (BusinessException e) {
			assertEquals("Subject with name 'admin' already exists.", e.getMessage());
		}
	}

	public void validate() {
		Subject subject = cliEnv.getCoreService().getSubjectByEmail(email);
		Assert.assertEquals(authName, subject.getAuthName());
		Assert.assertEquals(realName, subject.getRealname());
		Assert.assertEquals(email, subject.getEmail());
		Assert.assertEquals(description, subject.getDescription());
		Assert.assertEquals(language, subject.getLanguage());
		Assert.assertEquals(type, subject.getUserType());
		Assert.assertNull(subject.getSalt());

		if (UserType.LOCAL_USER.equals(type)) {
			Assert.assertNotNull(subject.getDigest());
			assertTrue(subject.getDigest().startsWith(BCryptPasswordHandler.getPrefix()));
			assertTrue(new BCryptPasswordHandler(subject).isValidPassword(password));
		} else {
			Assert.assertNull(subject.getDigest());
		}

	}

}
