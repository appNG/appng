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
package org.appng.core.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.naming.NamingException;

import org.appng.api.model.Site;
import org.appng.core.domain.SubjectImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class LdapServiceTest {

	private HashMap<String, Object> sitePropertyMocks;
	private LdapService ldapService;

	@Mock
	private Site mockedSite;

	@Mock
	private SubjectImpl mockedSubject;

	@Mock
	private org.appng.api.model.Properties mockedProperties;

	public LdapServiceTest() {
		ldapService = new LdapService();
		ldapService.setLdapCtxFactory("org.appng.core.service.LdapContextFactoryMock");

		sitePropertyMocks = new HashMap<>();
		sitePropertyMocks.put(LdapService.LDAP_HOST, "ldap://localhost:389");
		sitePropertyMocks.put(LdapService.LDAP_USER_BASE_DN, "ou=users,dc=example,dc=com");
		sitePropertyMocks.put(LdapService.LDAP_GROUP_BASE_DN, "ou=groups,dc=example,dc=com");
		sitePropertyMocks.put(LdapService.LDAP_USER, "serviceuser");
		sitePropertyMocks.put(LdapService.LDAP_PASSWORD, "inferno");
		sitePropertyMocks.put(LdapService.LDAP_PRINCIPAL_SCHEME, "SAM");
		sitePropertyMocks.put(LdapService.LDAP_START_TLS, false);
		sitePropertyMocks.put(LdapService.LDAP_DOMAIN, "EXAMPLE");
		sitePropertyMocks.put(LdapService.LDAP_ID_ATTRIBUTE, LdapContextMock.MOCKED_ID_ATTR);
	}

	public LdapContextMock setup(String userPrincipal, String userPassword, String servicePrincipal,
			String servicePassword) throws NamingException, IOException {
		MockitoAnnotations.initMocks(this);
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];

		String testId = caller.getMethodName() + ":" + caller.getLineNumber();
		sitePropertyMocks.replace(LdapService.LDAP_HOST, "ldaps://" + testId);

		Mockito.when(mockedSite.getName()).thenReturn(caller.getMethodName());
		Mockito.when(mockedSite.getProperties()).thenReturn(mockedProperties);
		for (String propName : sitePropertyMocks.keySet()) {
			Object propVal = sitePropertyMocks.get(propName);
			if (propVal instanceof String) {
				Mockito.when(mockedProperties.getString(propName)).thenReturn((String) propVal);
			} else if (propVal instanceof Boolean) {
				Mockito.when(mockedProperties.getBoolean(propName)).thenReturn(((Boolean) propVal).booleanValue());
			}
		}

		return LdapContextFactoryMock.setup(userPrincipal, userPassword, servicePrincipal, servicePassword,
				sitePropertyMocks);
	}

	@Test
	public void testLoginUserSucces() throws NamingException, IOException {
		boolean success;

		// DN
		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "DN");
		sitePropertyMocks.put(LdapService.LDAP_USER_BASE_DN, "l=egypt");
		sitePropertyMocks.put(LdapService.LDAP_ID_ATTRIBUTE, "uid");
		setup("uid=aziz,l=egypt", "light", null, null);
		success = ldapService.loginUser(mockedSite, "aziz", "light".toCharArray());
		Assert.assertTrue(success);

		// UPN
		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "UPN");
		sitePropertyMocks.replace(LdapService.LDAP_DOMAIN, "egypt");
		setup("aziz@egypt", "light", null, null);
		success = ldapService.loginUser(mockedSite, "aziz", "light".toCharArray());
		Assert.assertTrue(success);

		// SAM
		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "SAM");
		sitePropertyMocks.replace(LdapService.LDAP_DOMAIN, "egypt");
		setup("egypt\\aziz", "light", null, null);
		success = ldapService.loginUser(mockedSite, "aziz", "light".toCharArray());
		Assert.assertTrue(success);

		// Fallback to plain name
		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "bogus");
		setup("aziz", "light", null, null);
		success = ldapService.loginUser(mockedSite, "aziz", "light".toCharArray());
		Assert.assertTrue(success);
	}

	@Test
	public void testLoginUserFailure() throws NamingException, IOException {
		boolean success;
		List<Exception> exList;
		Exception ex;
		LdapContextMock ldapContextMock;

		// Principal wrong
		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "SAM");
		sitePropertyMocks.replace(LdapService.LDAP_DOMAIN, "egypt");
		ldapContextMock = setup("bielefeld\\aziz", "light", null, null);
		success = ldapService.loginUser(mockedSite, "aziz", "light".toCharArray());
		exList = ldapContextMock.exceptionHistory;
		ex = exList.size() > 0 ? exList.get(exList.size() - 1) : new Exception("Placeholder - nothing was thrown.");
		Assert.assertFalse(success);
		Assert.assertEquals(LdapContextMock.MSG_WRONG_USER, ex.getMessage());

		// Password wrong
		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "SAM");
		sitePropertyMocks.replace(LdapService.LDAP_DOMAIN, "egypt");
		ldapContextMock = setup("egypt\\aziz", "shadow", null, null);
		success = ldapService.loginUser(mockedSite, "aziz", "light".toCharArray());
		Assert.assertFalse(success);
		exList = ldapContextMock.exceptionHistory;
		ex = exList.size() > 0 ? exList.get(exList.size() - 1) : new Exception("Placeholder - nothing was thrown.");
		Assert.assertFalse(success);
		Assert.assertEquals(LdapContextMock.MSG_WRONG_PASS, ex.getMessage());
	}

	@Test
	public void testLoginGroupExistent() throws NamingException, IOException {
		List<String> searchedGroups;
		List<String> resultGroups;

		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "UPN");
		sitePropertyMocks.replace(LdapService.LDAP_DOMAIN, "egypt");
		sitePropertyMocks.put(LdapService.LDAP_USER, "mondoshawan");
		sitePropertyMocks.put(LdapService.LDAP_PASSWORD, "stones");
		sitePropertyMocks.put(LdapService.LDAP_USER_BASE_DN, "ou=users,l=egypt");
		sitePropertyMocks.put(LdapService.LDAP_GROUP_BASE_DN, "ou=groups,l=egypt");

		setup("aziz@egypt", "light", "mondoshawan@egypt", "stones");
		searchedGroups = Arrays.asList(LdapContextMock.MOCKED_GROUP_NAME);
		resultGroups = ldapService.loginGroup(mockedSite, "aZiZ", "light".toCharArray(), mockedSubject, searchedGroups);
		Assert.assertArrayEquals(searchedGroups.toArray(new String[0]), resultGroups.toArray(new String[0]));
	}

	@Test
	// The same as testLoginGroupExistent(), but additionally tests, if service user logins with DN do work.
	public void testLoginGroupExistentServiceUserDirectDN() throws NamingException, IOException {
		List<String> searchedGroups;
		List<String> resultGroups;

		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "UPN");
		sitePropertyMocks.replace(LdapService.LDAP_DOMAIN, "egypt");
		sitePropertyMocks.put(LdapService.LDAP_USER, "cn=mondoshawan,l=homeplanet");
		sitePropertyMocks.put(LdapService.LDAP_PASSWORD, "stones");
		sitePropertyMocks.put(LdapService.LDAP_USER_BASE_DN, "ou=users,l=egypt");
		sitePropertyMocks.put(LdapService.LDAP_GROUP_BASE_DN, "ou=groups,l=egypt");

		setup("aziz@egypt", "light", "cn=mondoshawan,l=homeplanet", "stones");
		searchedGroups = Arrays.asList(LdapContextMock.MOCKED_GROUP_NAME);
		resultGroups = ldapService.loginGroup(mockedSite, "aziz", "light".toCharArray(), mockedSubject, searchedGroups);
		Assert.assertArrayEquals(searchedGroups.toArray(new String[0]), resultGroups.toArray(new String[0]));
	}

	@Test
	public void testLoginGroupMissing() throws NamingException, IOException {
		List<String> searchedGroups;
		List<String> resultGroups;

		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "UPN");
		sitePropertyMocks.replace(LdapService.LDAP_DOMAIN, "egypt");
		sitePropertyMocks.put(LdapService.LDAP_USER, "mondoshawan");
		sitePropertyMocks.put(LdapService.LDAP_PASSWORD, "stones");
		sitePropertyMocks.put(LdapService.LDAP_USER_BASE_DN, "ou=users,l=egypt");
		sitePropertyMocks.put(LdapService.LDAP_GROUP_BASE_DN, "ou=groups,l=egypt");

		setup("aziz@egypt", "light", "mondoshawan@egypt", "stones");
		searchedGroups = Arrays.asList(
				"Well, if there's a bright center to the universe, you're on the usergroup that it's farthest from.");
		resultGroups = ldapService.loginGroup(mockedSite, "aziz", "light".toCharArray(), mockedSubject, searchedGroups);
		Assert.assertArrayEquals(new String[0], resultGroups.toArray(new String[0]));
	}

	@Test
	public void testUsersOfGroup() throws NamingException, IOException {
		List<SubjectImpl> resultSubjects;
		String[] expectSubjNames = new String[] { "aziz", "aziz' brother" };
		String[] expectSubjRealNames = new String[] { "Dummy", "Dummy" };

		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "UPN");
		sitePropertyMocks.replace(LdapService.LDAP_DOMAIN, "egypt");
		sitePropertyMocks.put(LdapService.LDAP_USER, "mondoshawan");
		sitePropertyMocks.put(LdapService.LDAP_PASSWORD, "stones");
		sitePropertyMocks.put(LdapService.LDAP_USER_BASE_DN, "ou=users,l=egypt");
		sitePropertyMocks.put(LdapService.LDAP_GROUP_BASE_DN, "ou=groups,l=egypt");

		setup("aziz@egypt", "light", "mondoshawan@egypt", "stones");
		resultSubjects = ldapService.getMembersOfGroup(mockedSite, LdapContextMock.MOCKED_GROUP_NAME);

		Assert.assertEquals(resultSubjects.size(), 2);
		for (int idx = 0; idx < 2; idx++) {
			Assert.assertEquals(resultSubjects.get(idx).getName(), expectSubjNames[idx]);
			Assert.assertEquals(resultSubjects.get(idx).getRealname(), expectSubjRealNames[idx]);
			Assert.assertFalse(resultSubjects.get(idx).isAuthenticated());
		}
	}

	@Test
	public void testLoginUserStartTls() throws NamingException, IOException {
		boolean success;

		sitePropertyMocks.replace(LdapService.LDAP_PRINCIPAL_SCHEME, "DN");
		sitePropertyMocks.put(LdapService.LDAP_USER_BASE_DN, "l=egypt");
		sitePropertyMocks.put(LdapService.LDAP_ID_ATTRIBUTE, "uid");
		sitePropertyMocks.put(LdapService.LDAP_START_TLS, true);
		setup("uid=aziz,l=egypt", "light", null, null);
		success = ldapService.loginUser(mockedSite, "aziz", "light".toCharArray());
		Assert.assertTrue(success);
	}
}
