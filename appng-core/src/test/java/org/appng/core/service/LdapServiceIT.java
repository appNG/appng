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
package org.appng.core.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.naming.ldap.LdapName;

import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.core.domain.SubjectImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ldap.core.LdapAttributes;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.ldif.parser.LdifParser;
import org.testcontainers.containers.GenericContainer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Ignore("run locally")
public class LdapServiceIT {

	private static final String USER_SECRET = "secret";
	private static final String USER_DN = "uid=admin,ou=system";
	private static GenericContainer<?> apacheDS;
	private static int mappedPort;

	@BeforeClass
	public static void setup() throws Exception {
		apacheDS = new GenericContainer<>("greggigon/apacheds");
		apacheDS.withExposedPorts(10389);
		apacheDS.start();
		mappedPort = apacheDS.getMappedPort(10389);
		LOGGER.info("ApacheDS running on port {}", mappedPort);
		writeUsersAndGroups();
	}

	@AfterClass
	public static void shutdown() {
		apacheDS.stop();
		apacheDS.close();
	}

	@Test
	public void testLoginGroupInvalidCredentials() throws Exception {
		Site site = mockSite("");
		List<String> memberOf = new LdapService().loginGroup(site, "John Wick", "wrong".toCharArray(),
				new SubjectImpl(), Arrays.asList());
		Assert.assertEquals(0, memberOf.size());
	}

	@Test
	public void testLoginUser() throws Exception {
		Site site = mockSite("ou=ACME,ou=Groups,dc=example,dc=com");
		boolean success = new LdapService().loginUser(site, "John Wick", "secret".toCharArray());
		Assert.assertTrue(success);
	}

	@Test
	public void testLoginUserEmptyCredentials() throws Exception {
		boolean success = new LdapService().loginUser(mockSite(""), "John Wick", "".toCharArray());
		Assert.assertFalse(success);
	}

	@Test
	public void testLoginUserInvalidCredentials() throws Exception {
		boolean success = new LdapService().loginUser(mockSite(""), "John Wick", "wrong".toCharArray());
		Assert.assertFalse(success);
	}

	@Test
	public void testLoginGroupLooneyToones() throws Exception {
		Site site = mockSite("ou=ACME,ou=Groups,dc=example,dc=com");
		List<String> groups = Arrays.asList("SHIELD", "Heroes", "Looney Tunes");
		List<String> memberOf = new LdapService().loginGroup(site, "John Wick", "secret".toCharArray(),
				new SubjectImpl(), groups);
		// group base dn is defined at site. appNG should find only one group for this user because it can evaluate only
		// one group
		Assert.assertEquals(1, memberOf.size());
	}

	@Test
	public void testLoginGroupDC() throws Exception {
		Site site = mockSite("ou=DC,ou=Groups,dc=example,dc=com");
		List<String> groups = Arrays.asList("SHIELD", "Heroes", "Looney Tunes");
		List<String> memberOf = new LdapService().loginGroup(site, "John Wick", "secret".toCharArray(),
				new SubjectImpl(), groups);
		// group base dn is defined at site. appNG should find only one group for this user because it can evaluate only
		// one group
		Assert.assertEquals(1, memberOf.size());
	}

	@Test
	public void testLoginAllGroups() throws Exception {
		Site site = mockSite("");
		List<String> groups = Arrays.asList("cn=SHIELD,ou=Marvel,ou=Groups,dc=example,dc=com",
				"cn=Heroes,ou=DC,ou=Groups,dc=example,dc=com", "cn=Looney Tunes,ou=ACME,ou=Groups,dc=example,dc=com");
		List<String> memberOf = new LdapService().loginGroup(site, "John Wick", "secret".toCharArray(),
				new SubjectImpl(), groups);
		// all groups are defined by full LDAP Name. groupBaseDn is empty. LdapService should find all groups and Mr.
		// Wick is member in all groups
		Assert.assertEquals(3, memberOf.size());
	}

	@Test
	public void testLoginNonSenseGroups() throws Exception {
		Site site = mockSite("");
		List<String> groups = Arrays.asList("this is nonsense", "insane group definition",
				"cn=Looney Tunes,ou=ACME,ou=Groups,dc=example,dc=com");
		List<String> memberOf = new LdapService().loginGroup(site, "John Wick", "secret".toCharArray(),
				new SubjectImpl(), groups);
		// even if the previous groups are nonsense, the implementation shall be tolerant and actually find the well
		// defined groups
		Assert.assertEquals(1, memberOf.size());
	}

	@Test
	public void testGetMembersOfGroup() throws Exception {
		Site site = mockSite("ou=ACME,ou=Groups,dc=example,dc=com");
		List<SubjectImpl> membersOfGroup = new LdapService().getMembersOfGroup(site, "Looney Tunes");
		Assert.assertEquals(3, membersOfGroup.size());
		Assert.assertEquals("jane@example.com", membersOfGroup.get(0).getEmail());
		Assert.assertEquals("joe@example.com", membersOfGroup.get(1).getEmail());
		Assert.assertEquals("john@example.com", membersOfGroup.get(2).getEmail());
	}

	@Test
	public void testGetMembersOfGroupEmptyBaseDn() throws Exception {
		Site site = mockSite("");
		List<SubjectImpl> membersOfGroup = new LdapService().getMembersOfGroup(site,
				"cn=Heroes,ou=DC,ou=Groups,dc=example,dc=com");
		Assert.assertEquals(2, membersOfGroup.size());
		Assert.assertEquals("jane@example.com", membersOfGroup.get(0).getEmail());
		Assert.assertEquals("john@example.com", membersOfGroup.get(1).getEmail());
	}

	private Site mockSite(String groupBaseDn) {
		Site site = Mockito.mock(Site.class);
		Properties siteProps = Mockito.mock(Properties.class);
		Mockito.when(site.getProperties()).thenReturn(siteProps);

		Mockito.when(siteProps.getString(LdapService.LDAP_HOST)).thenReturn("ldap://localhost:" + mappedPort);
		Mockito.when(siteProps.getString(LdapService.LDAP_ID_ATTRIBUTE)).thenReturn("cn");
		Mockito.when(siteProps.getString(LdapService.LDAP_USER)).thenReturn(USER_DN);
		Mockito.when(siteProps.getString(LdapService.LDAP_PASSWORD)).thenReturn(USER_SECRET);
		Mockito.when(siteProps.getString(LdapService.LDAP_GROUP_BASE_DN)).thenReturn(groupBaseDn);
		Mockito.when(siteProps.getString(LdapService.LDAP_USER_BASE_DN)).thenReturn("ou=Users,dc=example,dc=com");
		Mockito.when(siteProps.getString(LdapService.LDAP_PRINCIPAL_SCHEME)).thenReturn("DN");
		Mockito.when(siteProps.getBoolean(LdapService.LDAP_START_TLS)).thenReturn(false);
		return site;
	}

	private static void writeUsersAndGroups() throws Exception, IOException {
		LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUserDn(USER_DN);
		contextSource.setPassword(USER_SECRET);
		contextSource.setUrl("ldap://localhost:" + mappedPort);
		contextSource.afterPropertiesSet();
		LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
		LdifParser parser = new LdifParser(new ClassPathResource("ldif/users-and-groups.ldif"));
		parser.open();
		while (parser.hasMoreRecords()) {
			LdapAttributes record = parser.getRecord();
			LdapName dn = record.getName();
			ldapTemplate.bind(dn, null, record);
			LOGGER.info("Wrote: {}", dn);
		}
		parser.close();
	}

}
