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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.core.domain.SubjectImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service providing methods to login {@link Subject}s based on the LDAP-configuration of a {@link Site}. The following
 * site-properties need to be configured properly:
 * <ul>
 * <li>{@value #LDAP_DOMAIN}
 * <li>{@value #LDAP_GROUP_BASE_DN}
 * <li>{@value #LDAP_HOST}
 * <li>{@value #LDAP_ID_ATTRIBUTE}
 * <li>{@value #LDAP_PASSWORD}
 * <li>{@value #LDAP_PRINCIPAL_SCHEME}
 * <li>{@value #LDAP_START_TLS}
 * <li>{@value #LDAP_USER}
 * <li>{@value #LDAP_USER_BASE_DN}
 * </ul>
 * 
 * @author Matthias Müller
 * @author Dirk Heuvels
 */
public class LdapService {
	private static final Logger LOG = LoggerFactory.getLogger(LdapService.class);

	private String ldapCtxFactory = "com.sun.jndi.ldap.LdapCtxFactory";
	private LdapContext ctx = null;
	private StartTlsResponse tls = null;

	private static final String CN_ATTRIBUTE = "cn";
	private static final String MEMBER_ATTRIBUTE = "member";
	private static final String MAIL_ATTRIBUTE = "mail";

	private static final String SAM_DOMAIN_SEPARATOR = "\\";
	private static final String LDAP_NETWORK_TIMEOUTS = "8000";

	/** The domain for the LDAP authentication */
	public static final String LDAP_DOMAIN = "ldapDomain";
	/** The base-DN for LDAP-groups */
	public static final String LDAP_GROUP_BASE_DN = "ldapGroupBaseDn";
	/** The LDAP host */
	public static final String LDAP_HOST = "ldapHost";
	/** The name of the LDAP-attribute containing the user-id used for authentication */
	public static final String LDAP_ID_ATTRIBUTE = "ldapIdAttribute";
	/** Password of the LDAP service-user */
	public static final String LDAP_PASSWORD = "ldapPassword";
	/** How the LDAP principal is derived from a given username when logging in (DN, SAM, UPN) */
	public static final String LDAP_PRINCIPAL_SCHEME = "ldapPrincipalScheme";
	/** Whether to use STARTTLS for the LDAP connection */
	public static final String LDAP_START_TLS = "ldapStartTls";
	/** The name of the LDAP service-user */
	public static final String LDAP_USER = "ldapUser";
	/** The base-DN for LDAP-users */
	public static final String LDAP_USER_BASE_DN = "ldapUserBaseDn";

	/**
	 * Set another factory class to be used as JNDI parameter {@code Context.INITIAL_CONTEXT_FACTORY}. This is primarily
	 * useful for unit testing. The default value is {@code com.sun.jndi.ldap.LdapCtxFactory}.
	 *
	 * @param ldapCtxFactory
	 *            an alternative context factory class to be used.
	 */
	public void setLdapCtxFactory(String ldapCtxFactory) {
		this.ldapCtxFactory = ldapCtxFactory;
	}

	// Nested class to encapsulate LdapService specific details of credentials and rules for creating JNDI environments.
	private class LdapCredentials {
		private String siteName;

		private String principal;
		private String password;
		private String ldapHost;
		private String baseDn;
		private boolean useStartTls;

		private LdapCredentials(Site site, String username, char[] password, boolean isServiceUser) {
			org.appng.api.model.Properties siteProperties = site.getProperties();
			this.siteName = site.getName();

			this.password = String.valueOf(password);
			this.ldapHost = siteProperties.getString(LDAP_HOST);
			this.baseDn = siteProperties.getString(LDAP_USER_BASE_DN);
			this.useStartTls = siteProperties.getBoolean(LDAP_START_TLS);

			String idAttribute = siteProperties.getString(LDAP_ID_ATTRIBUTE);
			String windowsDomain = siteProperties.getString(LDAP_DOMAIN);
			String principalScheme = siteProperties.getString(LDAP_PRINCIPAL_SCHEME);

			boolean rawPrincipal = false;
			if (isServiceUser) {
				Pattern dnPattern = Pattern
						.compile("^[a-z0-9+\"\\\\<>; \\n\\d]+?=.+?(,[a-z0-9+\"\\\\<>; \\n\\d]+?=.+?)+$");
				Matcher dnMatcher = dnPattern.matcher(username);
				if (dnMatcher.matches())
					rawPrincipal = true;
			}

			if (rawPrincipal) {
				this.principal = username;
			} else {
				switch (principalScheme.toUpperCase()) {
				case "DN":
					this.principal = idAttribute + "=" + username + "," + baseDn;
					break;
				case "UPN":
					this.principal = username + "@" + windowsDomain;
					break;
				case "SAM":
					this.principal = windowsDomain + SAM_DOMAIN_SEPARATOR + username;
					break;
				default:
					this.principal = username;
					LOG.info("Unknown keyword '" + principalScheme + "' in site property '" + siteName + "."
							+ LDAP_PRINCIPAL_SCHEME + "'. Falling back to plain username '" + username
							+ "' as principal.");
				}
			}
		}

		private Properties getLdapEnv() {
			Properties env = new Properties();
			env.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxFactory);
			env.put(Context.PROVIDER_URL, ldapHost);
			env.put("com.sun.jndi.ldap.connect.timeout", LDAP_NETWORK_TIMEOUTS);
			env.put("com.sun.jndi.ldap.read.timeout", LDAP_NETWORK_TIMEOUTS);
			if (useStartTls) {
				// No authentication until we have successfully negotiated STARTTLS.
				env.put(Context.SECURITY_AUTHENTICATION, "none");
				return env;
			} else {
				if (!ldapHost.toLowerCase().startsWith("ldaps://"))
					LOG.info("LDAP Configuration of site '" + siteName + "' neither uses LDAP over SSL ('ldaps://')"
							+ " nor STARTTLS. Credentials will be transmitted as cleartext.");
				env.put(Context.SECURITY_AUTHENTICATION, "simple");
				env.put(Context.SECURITY_PRINCIPAL, principal);
				env.put(Context.SECURITY_CREDENTIALS, password);
				return env;
			}
		}

		private void addToContext(LdapContext ctx) throws NamingException {
			ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
			ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, principal);
			ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
		}
	}

	/**
	 * Tries to login the user with the given username and password.
	 * 
	 * @param site
	 *            the {@link Site} the user wants to login at
	 * @param username
	 *            The plain name of the user without base-DN. This name will be mapped to an LDAP principal according to
	 *            the value of {@value #LDAP_PRINCIPAL_SCHEME}.
	 *            <ul>
	 *            <li>"DN": results in <code>{@value #LDAP_ID_ATTRIBUTE}=username,{@value #LDAP_USER_BASE_DN}</code>
	 *            (this should work with any LDAP server)</li>
	 *            <li>"UPN": results in <code>username@{@value #LDAP_DOMAIN}</code> (probably most common name format to
	 *            log on to Active Directory, @see <a https://msdn.microsoft.com/en-us/library/cc223499.aspx">MSDN on
	 *            LDAP simple authentication</a>)</li>
	 *            <li>"SAM": results in <code>{@value #LDAP_DOMAIN}&#92;username</code> (name format including
	 *            sAMAccountName and NetBios name to logon to active Directory)</li>
	 *            </ul>
	 * @param password
	 *            the password of the user
	 * @return {@code true} if the user could be successfully logged in, {@code false} otherwise
	 */

	public boolean loginUser(Site site, String username, char[] password) {
		LdapCredentials ldapCredentials = new LdapCredentials(site, username, password, false);
		try {
			getContext(ldapCredentials);
			return true;
		} catch (IOException | NamingException ex) {
			logException(ldapCredentials.ldapHost, ldapCredentials.principal, ex);
			return false;
		} finally {
			closeContext();
		}
	}

	/**
	 * Tries to login the user as a member of at least one of the given groups. Therefore two steps are necessary.
	 * First, the login of the user with the given password must be successful. Second, the user must be a member of at
	 * least one group.
	 * 
	 * Note that to determine the memberships a service user with credentials taken from {@value #LDAP_USER} and
	 * {@value #LDAP_PASSWORD}, will be used. This username may be specified as Distinguished Name (DN) e.g. "cn=Service
	 * User, dc=mycompany, dc=com". If this is the case, it will be used as LDAP principal without mapping. If it is not
	 * a DN, it will be mapped as described in {@link #loginUser(Site, String, char[])}.
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
		if (loginUser(site, username, password)) {
			return getUserGroups(username, site, subject, groupNames);
		}
		return new ArrayList<String>();
	}

	private List<String> getUserGroups(String username, Site site, SubjectImpl subject, List<String> groupNames) {
		List<String> userGroups = new ArrayList<String>();

		String serviceUser = site.getProperties().getString(LDAP_USER);
		char[] servicePassword = site.getProperties().getString(LDAP_PASSWORD).toCharArray();
		LdapCredentials ldapCredentials = new LdapCredentials(site, serviceUser, servicePassword, true);

		String groupBaseDn = site.getProperties().getString(LDAP_GROUP_BASE_DN);
		String idAttribute = site.getProperties().getString(LDAP_ID_ATTRIBUTE);

		try {
			ctx = getContext(ldapCredentials);
			for (String group : groupNames) {
				String groupDn = CN_ATTRIBUTE + "=" + group + "," + groupBaseDn;
				Attributes memberAttrs = ctx.getAttributes(groupDn, new String[] { MEMBER_ATTRIBUTE });
				for (String member : getMemberNames(memberAttrs)) {
					Attributes userAttrs = ctx.getAttributes(member);
					String id = (String) userAttrs.get(idAttribute).get();
					String realName = (String) userAttrs.get(CN_ATTRIBUTE).get();
					if (username.equals(id)) {
						userGroups.add(group);
						subject.setName(username);
						subject.setRealname(realName);
					}
				}
			}
		} catch (IOException | NamingException ex) {
			logException(ldapCredentials.ldapHost, ldapCredentials.principal, ex);
		} finally {
			closeContext();
		}
		return userGroups;
	}

	/**
	 * Fetches the members of a given group and returns them as a List of {@link SubjectImpl} objects. Members are LDAP
	 * Objects in the {@code member} attribute(s) of
	 * <code>{@value #LDAP_ID_ATTRIBUTE}=groupName,{@value #LDAP_GROUP_BASE_DN}</code>.
	 * 
	 * @param site
	 *            the {@link Site} in which the application using this group is running
	 * @param groupName
	 *            the name of the group whose members should be fetched
	 * @return the members of the groupName (may be empty)
	 */
	public List<SubjectImpl> getMembersOfGroup(Site site, String groupName) {
		List<SubjectImpl> subjects = new ArrayList<SubjectImpl>();

		String serviceUser = site.getProperties().getString(LDAP_USER);
		char[] servicePassword = site.getProperties().getString(LDAP_PASSWORD).toCharArray();
		LdapCredentials ldapCredentials = new LdapCredentials(site, serviceUser, servicePassword, true);

		String groupBaseDn = site.getProperties().getString(LDAP_GROUP_BASE_DN);
		String idAttribute = site.getProperties().getString(LDAP_ID_ATTRIBUTE);

		try {
			ctx = getContext(ldapCredentials);
			String groupDn = CN_ATTRIBUTE + "=" + groupName + "," + groupBaseDn;
			Attributes memberAttrs = ctx.getAttributes(groupDn, new String[] { MEMBER_ATTRIBUTE });
			for (String member : getMemberNames(memberAttrs)) {
				Attributes userAttrs = ctx.getAttributes(member);
				String realName = (String) userAttrs.get(CN_ATTRIBUTE).get();
				String username = (String) userAttrs.get(idAttribute).get();
				String email = (String) userAttrs.get(MAIL_ATTRIBUTE).get();
				SubjectImpl ldapSubject = new SubjectImpl();
				ldapSubject.setName(username);
				ldapSubject.setRealname(realName);
				ldapSubject.setEmail(email.toLowerCase());
				subjects.add(ldapSubject);
			}
		} catch (IOException | NamingException ex) {
			logException(ldapCredentials.ldapHost, ldapCredentials.principal, ex);
		} finally {
			closeContext();
		}

		return subjects;
	}

	private LdapContext getContext(LdapCredentials ldapCredentials) throws NamingException, IOException {
		Properties env = ldapCredentials.getLdapEnv();
		ctx = new InitialLdapContext(env, null);

		if (ldapCredentials.useStartTls) {
			tls = (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
			tls.setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true; // Allow STARTTLS with plain IP addresses.
				}
			});
			tls.negotiate();
			ldapCredentials.addToContext(ctx);
			ctx.reconnect(null);
		}
		return ctx;
	}

	private void logException(String host, String principal, Exception e) {
		String message;
		String excInfo = "(" + e.getClass().getName() + ": " + e.getMessage() + ")";
		if (e instanceof AuthenticationException) {
			message = "failed to login user '" + principal + "' on host '" + host + "' " + excInfo;
		} else {
			message = "LDAP operation failed on host '" + host + "' with principal '" + principal + "' " + excInfo;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(message, e);
		} else if (LOG.isInfoEnabled()) {
			LOG.info(message);
		}
	}

	private void closeContext() {
		if (tls != null) {
			try {
				tls.close();
			} catch (IOException ioe) {
				LOG.warn("error closing TLS connection", ioe);
			}
		}
		if (ctx != null) {
			try {
				ctx.close();
			} catch (NamingException ne) {
				LOG.warn("error closing LDAP context", ne);
			}
		}
	}

	private List<String> getMemberNames(Attributes attributes) throws NamingException {
		Attribute memberAttr = attributes.get(MEMBER_ATTRIBUTE);
		if (memberAttr == null)
			return Collections.emptyList();
		else {
			List<String> result = new ArrayList<String>();
			NamingEnumeration<?> memberAttrEnum = memberAttr.getAll();
			while (memberAttrEnum.hasMoreElements())
				result.add(memberAttrEnum.nextElement().toString());
			return result;
		}
	}

}
