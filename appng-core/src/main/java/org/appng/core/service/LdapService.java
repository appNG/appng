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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.core.domain.SubjectImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing methods to login {@link Subject}s based on the LDAP-configuration of a {@link Site}. The following
 * site-properties need to be configured properly:
 * <ul>
 * <li>{@value #LDAP_HOST}
 * <li>{@value #LDAP_DOMAIN}
 * <li>{@value #LDAP_USER}
 * <li>{@value #LDAP_PASSWORD}
 * <li>{@value #LDAP_GROUP_BASE_DN}
 * <li>{@value #LDAP_USER_BASE_DN}
 * <li>{@value #LDAP_ID_ATTRIBUTE}
 * </ul>
 * 
 * @author Matthias Müller
 */
public class LdapService {

	private static final Logger LOG = LoggerFactory.getLogger(LdapService.class);

	private static final String CN_ATTRIBUTE = "cn";
	private static final String MEMBER_ATTRIBUTE = "member";
	private static final String MAIL_ATTRIBUTE = "mail";

	/** The domain for the LDAP authentication */
	public static final String LDAP_DOMAIN = "ldapDomain";
	/** Password of the LDAP service-user */
	public static final String LDAP_PASSWORD = "ldapPassword";
	/** The name of the LDAP service-user */
	public static final String LDAP_USER = "ldapUser";
	/** The base-DN for LDAP-groups */
	public static final String LDAP_GROUP_BASE_DN = "ldapGroupBaseDn";
	/** The base-DN for LDAP-users */
	public static final String LDAP_USER_BASE_DN = "ldapUserBaseDn";
	/** The LDAP host */
	public static final String LDAP_HOST = "ldapHost";
	/** The name of the LDAP-attribute containing the user-id used for authentication */
	public static final String LDAP_ID_ATTRIBUTE = "ldapIdAttribute";

	private static final String DOMAIN_SEPARATOR = "\\";

	/**
	 * Tries to login the user with the given username and password.
	 * 
	 * @param site
	 *            the {@link Site} the user wants to login at
	 * @param username
	 *            the name of the user, without base-DN (this is set in the site-property {@value #LDAP_USER_BASE_DN})
	 * @param password
	 *            the password of the user
	 * @return {@code true} if the user could be successfully logged in, {@code false} otherwise
	 */
	public boolean loginUser(Site site, String username, char[] password) {
		String ldapHost = site.getProperties().getString(LDAP_HOST);
		String baseDn = site.getProperties().getString(LDAP_USER_BASE_DN);
		String ldapDomain = site.getProperties().getString(LDAP_DOMAIN);
		return loginUser(ldapHost, baseDn, ldapDomain, username, password);
	}

	/**
	 * Tries to login the user as a member of at least one of the given groups. Therefore two steps are necessary.
	 * First, the login of the user with the given password must be successful. Second, the user must be a member of at
	 * least one group.
	 * 
	 * @param site
	 *            the {@link Site} the user wants to login at
	 * @param username
	 *            the name of the user
	 * @param password
	 *            the password of the user
	 * @param subject
	 *            a {@link SubjectImpl} where the name and real name are set, in case the user belongs to at least one
	 *            of the given groups
	 * @param groupNames
	 *            a list containing the names of all groups to check group membership for (without base-DN, this is set
	 *            in the site-property {@value #LDAP_GROUP_BASE_DN})
	 * @return the names of all groups that the user is a member of (may be empty)
	 */
	public List<String> loginGroup(Site site, String username, char[] password, SubjectImpl subject,
			List<String> groupNames) {

		String ldapHost = site.getProperties().getString(LDAP_HOST);
		String baseDn = site.getProperties().getString(LDAP_USER_BASE_DN);
		String ldapDomain = site.getProperties().getString(LDAP_DOMAIN);
		String groupBaseDn = site.getProperties().getString(LDAP_GROUP_BASE_DN);
		String idAttribute = site.getProperties().getString(LDAP_ID_ATTRIBUTE);

		String serviceUser = site.getProperties().getString(LDAP_USER);
		char[] servicePassword = site.getProperties().getString(LDAP_PASSWORD).toCharArray();
		boolean isValidUser = loginUser(ldapHost, groupBaseDn, ldapDomain, username, password);
		if (isValidUser) {
			return loginGroup(ldapHost, baseDn, ldapDomain, groupBaseDn, idAttribute, serviceUser, servicePassword,
					username, subject, groupNames);
		}
		return new ArrayList<String>();
	}

	private List<String> loginGroup(String ldapHost, String baseDn, String ldapDomain, String groupBaseDn,
			String idAttribute, String serviceUser, char[] servicePassword, String username, SubjectImpl subject,
			List<String> groupNames) {
		List<String> userGroups = new ArrayList<String>();
		Properties ldapEnv = getLdapEnv(ldapHost, serviceUser, servicePassword);
		DirContext ctx = null;
		try {
			username = getUserName(username, ldapDomain);
			ctx = getContext(ldapEnv);
			for (String group : groupNames) {
				String name = CN_ATTRIBUTE + "=" + group + "," + groupBaseDn;
				Attributes members = ctx.getAttributes(name, new String[] { MEMBER_ATTRIBUTE });
				List<String> memberNames = getMemberNames(members);
				for (String member : memberNames) {
					Attributes userAttributes = ctx.getAttributes(member);
					String id = (String) userAttributes.get(idAttribute).get();
					String realName = (String) userAttributes.get(CN_ATTRIBUTE).get();
					if (username.equals(id)) {
						userGroups.add(group);
						subject.setName(username);
						subject.setRealname(realName);
					}
				}
			}
		} catch (NamingException e) {
			logException(ldapHost, serviceUser, e);
		} finally {
			closeContext(ctx);
		}
		return userGroups;
	}

	public List<SubjectImpl> getMembersOfGroup(org.appng.api.model.Properties siteProperties, String groupName) {

		String ldapHost = siteProperties.getString(LDAP_HOST);
		String groupBaseDn = siteProperties.getString(LDAP_GROUP_BASE_DN);
		String serviceUser = siteProperties.getString(LDAP_USER);
		char[] servicePassword = siteProperties.getString(LDAP_PASSWORD).toCharArray();
		String idAttribute = siteProperties.getString(LDAP_ID_ATTRIBUTE);

		List<SubjectImpl> subjects = new ArrayList<SubjectImpl>();
		Properties ldapEnv = getLdapEnv(ldapHost, serviceUser, servicePassword);
		DirContext ctx = null;
		try {
			ctx = getContext(ldapEnv);
			String groupDn = CN_ATTRIBUTE + "=" + groupName + "," + groupBaseDn;
			Attributes members = ctx.getAttributes(groupDn, new String[] { MEMBER_ATTRIBUTE });
			List<String> membersOfGroup = getMemberNames(members);
			for (String member : membersOfGroup) {
				Attributes userAttributes = ctx.getAttributes(member);
				String realName = (String) userAttributes.get(CN_ATTRIBUTE).get();
				String username = (String) userAttributes.get(idAttribute).get();
				String email = (String) userAttributes.get(MAIL_ATTRIBUTE).get();
				SubjectImpl ldapSubject = new SubjectImpl();
				ldapSubject.setName(username);
				ldapSubject.setRealname(realName);
				ldapSubject.setEmail(email.toLowerCase());
				subjects.add(ldapSubject);
			}

		} catch (NamingException e) {
			logException(ldapHost, serviceUser, e);
		} finally {
			closeContext(ctx);
		}
		return subjects;
	}

	private boolean loginUser(String ldapHost, String baseDn, String ldapDomain, String username, char[] password) {
		String principal = null;
		if (StringUtils.isNotBlank(ldapDomain)) {
			if (username.startsWith(ldapDomain)) {
				principal = username;
				username = getUserName(username, ldapDomain);
			} else {
				principal = ldapDomain + DOMAIN_SEPARATOR + username;
			}
		} else {
			principal = CN_ATTRIBUTE + "=" + username + "," + baseDn;
		}

		Properties env = getLdapEnv(ldapHost, principal, password);
		DirContext ctx = null;
		try {
			ctx = getContext(env);
			return true;
		} catch (NamingException e) {
			logException(ldapHost, principal, e);
		} finally {
			closeContext(ctx);
		}
		return false;
	}

	protected DirContext getContext(Properties env) throws NamingException {
		return new InitialDirContext(env);
	}

	private void logException(String host, String user, NamingException e) {
		String message;
		String exceptionInfo = "(" + e.getClass().getName() + ": " + e.getMessage() + ")";
		if (e instanceof AuthenticationException) {
			message = "failed to login user '" + user + "' on host '" + host + "' " + exceptionInfo;
		} else {
			message = "LDAP operation failed on host '" + host + "' with user '" + user + "'" + exceptionInfo;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(message, e);
		} else if (LOG.isInfoEnabled()) {
			LOG.info(message);
		}
	}

	private String getUserName(String username, String ldapDomain) {
		if (null != ldapDomain) {
			if (username.startsWith(ldapDomain)) {
				return username.substring(username.indexOf(DOMAIN_SEPARATOR) + 1);
			}
		}
		return username;
	}

	private void closeContext(DirContext ctx) {
		try {
			if (null != ctx) {
				ctx.close();
			}
		} catch (NamingException e) {
			LOG.warn("error closing context", e);
		}
	}

	private Properties getLdapEnv(String ldapHost, String username, char[] password) {
		Properties env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapHost);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, username);
		env.put(Context.SECURITY_CREDENTIALS, String.valueOf(password));
		return env;
	}

	protected List<String> getMemberNames(Attributes attributes) throws NamingException {
		List<String> result = new ArrayList<String>();
		NamingEnumeration<? extends Attribute> allAttributes = attributes.getAll();
		while (allAttributes.hasMoreElements()) {
			Attribute attribute = allAttributes.nextElement();
			NamingEnumeration<?> subAttributes = attribute.getAll();
			while (subAttributes.hasMoreElements()) {
				Object object = subAttributes.nextElement();
				result.add(object.toString());
			}
		}
		return result;
	}

}

