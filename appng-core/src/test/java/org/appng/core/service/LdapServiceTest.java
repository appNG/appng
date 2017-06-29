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
package org.appng.core.service;

import java.util.Arrays;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.domain.SubjectImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class LdapServiceTest extends LdapService {

	private static final String USERS = "users";
	private static final String PASSWORD = "password";
	private static final String USERNAME = "username";
	private static final String SERVICEUSER = "serviceuser";

	@Mock
	private DirContext dirContext;

	@Mock
	private Site site;

	@Mock
	private Properties properties;

	private java.util.Properties env;

	public java.util.Properties setup(String password) {
		MockitoAnnotations.initMocks(this);
		Mockito.when(site.getProperties()).thenReturn(properties);
		Mockito.when(properties.getString(LDAP_DOMAIN)).thenReturn("EXAMPLE");
		Mockito.when(properties.getString(LDAP_GROUP_BASE_DN)).thenReturn("OU=Groups,DC=example,DC=com");
		Mockito.when(properties.getString(LDAP_HOST)).thenReturn("ldap:localhost:389");
		Mockito.when(properties.getString(LDAP_ID_ATTRIBUTE)).thenReturn("cn");
		Mockito.when(properties.getString(LDAP_PASSWORD)).thenReturn(password);
		Mockito.when(properties.getString(LDAP_USER)).thenReturn(SERVICEUSER);
		Mockito.when(properties.getString(LDAP_USER_BASE_DN)).thenReturn("OU=Users,DC=example,DC=com");

		java.util.Properties expected = new java.util.Properties();
		expected.put(Context.PROVIDER_URL, "ldap:localhost:389");
		expected.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		expected.put(Context.SECURITY_PRINCIPAL, "EXAMPLE\\username");
		expected.put(Context.SECURITY_AUTHENTICATION, "simple");
		expected.put(Context.SECURITY_CREDENTIALS, password);
		return expected;
	}

	@Test
	public void testLoginUser() {
		java.util.Properties expected = setup(PASSWORD);
		boolean success = this.loginUser(site, USERNAME, PASSWORD.toCharArray());
		Assert.assertTrue(success);
		Assert.assertEquals(expected, env);
	}

	@Test
	public void testLoginUserFail() {
		String password = "wrongPassword";
		java.util.Properties expected = setup(password);
		boolean success = this.loginUser(site, "EXAMPLE\\username", password.toCharArray());
		Assert.assertFalse(success);
		Assert.assertEquals(expected, env);
	}

	@Test
	public void testLoginGroup() throws NamingException {
		java.util.Properties expected = setup(PASSWORD);
		expected.put(Context.SECURITY_PRINCIPAL, SERVICEUSER);
		setAttributes(USERNAME);
		SubjectImpl subject = new SubjectImpl();
		List<String> loginGroup = this
				.loginGroup(site, USERNAME, PASSWORD.toCharArray(), subject, Arrays.asList(USERS));
		Assert.assertEquals(Arrays.asList(USERS), loginGroup);
		Assert.assertEquals(expected, env);
		Assert.assertEquals("username", subject.getName());
		Assert.assertEquals("username", subject.getRealname());
	}

	@Test
	public void testLoginGroupFail() throws NamingException {
		java.util.Properties expected = setup(PASSWORD);
		expected.put(Context.SECURITY_PRINCIPAL, SERVICEUSER);
		setAttributes("");
		SubjectImpl subject = new SubjectImpl();
		List<String> loginGroups = this.loginGroup(site, USERNAME, PASSWORD.toCharArray(), subject,
				Arrays.asList(USERS));
		Assert.assertTrue(loginGroups.isEmpty());
		Assert.assertEquals(expected, env);
		Assert.assertNull(subject.getName());
		Assert.assertNull(subject.getRealname());
	}

	public void setAttributes(String userName) throws NamingException {
		Attributes attributes = Mockito.mock(Attributes.class);
		Mockito.when(dirContext.getAttributes(USERS)).thenReturn(attributes);
		Attribute attribute = Mockito.mock(Attribute.class);
		Mockito.when(attributes.get("cn")).thenReturn(attribute);
		Mockito.when(attribute.get()).thenReturn(userName);
	}

	@Override
	protected DirContext getContext(java.util.Properties env) throws NamingException {
		this.env = env;
		if (!env.get(Context.SECURITY_CREDENTIALS).equals(PASSWORD)) {
			throw new NamingException("wrong password");
		}
		return dirContext;
	}

	@Override
	protected List<String> getMemberNames(Attributes attributes) throws NamingException {
		return Arrays.asList(USERS);
	}
}
