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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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

import org.apache.commons.lang3.StringUtils;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.core.domain.SubjectImpl;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class LdapService {

	private String ldapCtxFactory = "com.sun.jndi.ldap.LdapCtxFactory";

	private static final String CN_ATTRIBUTE = "cn";
	private static final String MEMBER_ATTRIBUTE = "member";
	private static final String MAIL_ATTRIBUTE = "mail";

	private static final String SAM_DOMAIN_SEPARATOR = "\\";
	private static final String LDAP_NETWORK_TIMEOUTS = "8000";
	private static final Pattern DN_PATTERN = Pattern
			.compile("^[a-z0-9+\"\\\\<>; \\n\\d]+?=.+?(,[a-z0-9+\"\\\\<>; \\n\\d]+?=.+?)+$");

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
	 *                       an alternative context factory class to be used.
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

			if (isServiceUser && DN_PATTERN.matcher(username).matches()) {
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
					LOGGER.info(
							"Unknown keyword '{}' in site property '{}.{}'. Falling back to plain username '{}' as principal.",
							principalScheme, siteName, LDAP_PRINCIPAL_SCHEME, username);
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
					LOGGER.info("LDAP Configuration of site '{}' neither uses LDAP over SSL ('ldaps://')"
							+ " nor STARTTLS. Credentials will be transmitted as cleartext.", siteName);
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
	 * @param  site
	 *                  the {@link Site} the user wants to login at
	 * @param  username
	 *                  The plain name of the user without base-DN. This name will be mapped to an LDAP principal
	 *                  according to the value of {@value #LDAP_PRINCIPAL_SCHEME}.
	 *                  <ul>
	 *                  <li>"DN": results in
	 *                  <code>{@value #LDAP_ID_ATTRIBUTE}=username,{@value #LDAP_USER_BASE_DN}</code> (this should work
	 *                  with any LDAP server)</li>
	 *                  <li>"UPN": results in <code>username@{@value #LDAP_DOMAIN}</code> (probably most common name
	 *                  format to log on to Active Directory, @see <a
	 *                  https://msdn.microsoft.com/en-us/library/cc223499.aspx">MSDN on LDAP simple
	 *                  authentication</a>)</li>
	 *                  <li>"SAM": results in <code>{@value #LDAP_DOMAIN}&#92;username</code> (name format including
	 *                  sAMAccountName and NetBios name to logon to active Directory)</li>
	 *                  </ul>
	 * @param  password
	 *                  the password of the user
	 * @return          {@code true} if the user could be successfully logged in, {@code null} otherwise
	 */
	public boolean loginUser(Site site, String username, char[] password) {
		LdapCredentials ldapCredentials = new LdapCredentials(site, username, password, false);
		TlsAwareLdapContext ctx = null;
		try {
			ctx = new TlsAwareLdapContext(ldapCredentials);
			return true;
		} catch (IOException | NamingException ex) {
			logException(ldapCredentials.ldapHost, ldapCredentials.principal, ex);
		} finally {
			closeContext(ctx);
		}
		return false;
	}

	/**
	 * Tries to login the user as a member of at least one of the given groups. Therefore two steps are necessary.
	 * First, the login of the user with the given password must be successful. Second, the user must be a member of at
	 * least one group.<br/>
	 * Note that to determine the memberships a service user with credentials taken from {@value #LDAP_USER} and
	 * {@value #LDAP_PASSWORD}, will be used. This username may be specified as Distinguished Name (DN) e.g. "cn=Service
	 * User, dc=mycompany, dc=com". If this is the case, it will be used as LDAP principal without mapping. If it is not
	 * a DN, it will be mapped as described in {@link #loginUser(Site, String, char[])}.
	 * 
	 * @param  site
	 *                    the {@link Site} the user wants to login at
	 * @param  username
	 *                    the name of the user
	 * @param  password
	 *                    the password of the user
	 * @param  subject
	 *                    a {@link SubjectImpl} where the name and real name are set, in case the user belongs to at
	 *                    least one of the given groups
	 * @param  groupNames
	 *                    a list containing the names of all groups to check group membership for (without base-DN, this
	 *                    is set in the site-property {@value #LDAP_GROUP_BASE_DN})
	 * @return            the names of all groups that the user is a member of (may be empty)
	 */
	public List<String> loginGroup(Site site, String username, char[] password, SubjectImpl subject,
			List<String> groupNames) {
		LdapCredentials ldapCredentials = new LdapCredentials(site, username, password, false);
		TlsAwareLdapContext ctx = null;
		try {
			ctx = new TlsAwareLdapContext(ldapCredentials);
			return getUserGroups(ctx.delegate, username, site, subject, groupNames);
		} catch (NamingException | IOException ex) {
			logException(ldapCredentials.ldapHost, ldapCredentials.principal, ex);
		} finally {
			closeContext(ctx);
		}
		return new ArrayList<>();
	}

	private List<String> getUserGroups(LdapContext ctx, String username, Site site, SubjectImpl subject,
			List<String> groupNames) throws NamingException {
		List<String> userGroups = new ArrayList<>();
		String groupBaseDn = site.getProperties().getString(LDAP_GROUP_BASE_DN);
		String idAttribute = site.getProperties().getString(LDAP_ID_ATTRIBUTE);

		for (String group : groupNames) {
			if (checkGroupMembership(ctx, username, subject, groupBaseDn, idAttribute, group)) {
				userGroups.add(group);
			}
		}
		return userGroups;
	}

	private boolean checkGroupMembership(LdapContext ctx, String username, SubjectImpl subject, String groupBaseDn,
			String idAttribute, String group) throws NamingException {
		String groupDn = getGroupDn(group, groupBaseDn);
		try {
			for (String member : getGroupMembers(ctx, groupDn)) {
				Attributes userAttrs = getUserAttributes(ctx, member, idAttribute);
				String id = getAttribute(userAttrs, idAttribute);
				if (username.equalsIgnoreCase(id)) {
					fillSubjectFromAttributes(subject, idAttribute, userAttrs);
					LOGGER.info("User '{}' ({}) is member of '{}'", username, member, groupDn);
					return true;
				}
			}
		} catch (NamingException e) {
			LOGGER.info(String.format("Cannot evaluate group members of group '%s' (%s: %s)", groupDn,
					e.getClass().getName(), e.getMessage()));
		}
		return false;
	}

	private Attributes getUserAttributes(LdapContext ctx, String member, String idAttribute) throws NamingException {
		return ctx.getAttributes(member, new String[] { idAttribute, CN_ATTRIBUTE, MAIL_ATTRIBUTE });
	}

	/**
	 * Fetches the members of a given group and returns them as a List of {@link SubjectImpl} objects. Members are LDAP
	 * Objects in the {@code member} attribute(s) of
	 * <code>{@value #LDAP_ID_ATTRIBUTE}=groupName,{@value #LDAP_GROUP_BASE_DN}</code>.
	 * 
	 * @param  site
	 *                   the {@link Site} in which the application using this group is running
	 * @param  groupName
	 *                   the name of the group whose members should be fetched
	 * @return           the members of the groupName (may be empty)
	 */
	public List<SubjectImpl> getMembersOfGroup(Site site, String groupName) {
		List<SubjectImpl> subjects = new ArrayList<>();

		String serviceUser = site.getProperties().getString(LDAP_USER);
		char[] servicePassword = site.getProperties().getString(LDAP_PASSWORD).toCharArray();
		LdapCredentials ldapCredentials = new LdapCredentials(site, serviceUser, servicePassword, true);

		String groupBaseDn = site.getProperties().getString(LDAP_GROUP_BASE_DN);
		String idAttribute = site.getProperties().getString(LDAP_ID_ATTRIBUTE);
		String groupDn = getGroupDn(groupName, groupBaseDn);

		TlsAwareLdapContext ctx = null;
		try {
			ctx = new TlsAwareLdapContext(ldapCredentials);
			for (String member : getGroupMembers(ctx.delegate, groupDn)) {
				Attributes userAttrs = getUserAttributes(ctx.delegate, member, idAttribute);
				SubjectImpl ldapSubject = fillSubjectFromAttributes(new SubjectImpl(), idAttribute, userAttrs);
				subjects.add(ldapSubject);
			}
		} catch (IOException | NamingException ex) {
			logException(ldapCredentials.ldapHost, ldapCredentials.principal, ex);
		} finally {
			closeContext(ctx);
		}
		LOGGER.info("Found {} member(s) for group '{}'", subjects.size(), groupDn);
		return subjects;
	}

	private SubjectImpl fillSubjectFromAttributes(SubjectImpl subject, String idAttribute, Attributes userAttrs)
			throws NamingException {
		subject.setName(getAttribute(userAttrs, idAttribute));
		subject.setRealname(getAttribute(userAttrs, CN_ATTRIBUTE));
		subject.setEmail(StringUtils.lowerCase(getAttribute(userAttrs, MAIL_ATTRIBUTE)));
		return subject;
	}

	private List<String> getGroupMembers(LdapContext ctx, String groupDn) throws NamingException {
		Attributes groupAttrs = ctx.getAttributes(groupDn, new String[] { MEMBER_ATTRIBUTE });
		Attribute memberAttr = groupAttrs.get(MEMBER_ATTRIBUTE);
		List<String> members = new ArrayList<>();
		if (memberAttr != null) {
			NamingEnumeration<?> memberIt = memberAttr.getAll();
			while (memberIt.hasMoreElements()) {
				members.add((String) memberIt.nextElement());
			}
		}
		return members;
	}

	private String getGroupDn(String groupName, String groupBaseDn) {
		// if the group base dn is defined in site properties, assemble the attribute name. If this property is not set,
		// the group name must contain a complete valid ldap reference incl. base group dn
		return StringUtils.isBlank(groupBaseDn) ? groupName : (CN_ATTRIBUTE + "=" + groupName + "," + groupBaseDn);
	}

	private String getAttribute(Attributes attrs, String attribute) throws NamingException {
		Attribute attr = attrs.get(attribute);
		return null == attr ? null : ((String) attr.get());
	}

	private void logException(String host, String principal, Exception e) {
		String message;
		String excInfo = "(" + e.getClass().getName() + ": " + e.getMessage() + ")";
		if (e instanceof AuthenticationException) {
			message = "failed to login user '" + principal + "' on host '" + host + "' " + excInfo;
		} else {
			message = "LDAP operation failed on host '" + host + "' with principal '" + principal + "' " + excInfo;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(message, e);
		} else if (LOGGER.isInfoEnabled()) {
			LOGGER.info(message);
		}
	}

	private void closeContext(TlsAwareLdapContext ctx) {
		if (null != ctx) {
			ctx.close();
		}
	}

	private class TlsAwareLdapContext {
		private final LdapContext delegate;
		private StartTlsResponse tls;

		public TlsAwareLdapContext(LdapCredentials ldapCredentials) throws NamingException, IOException {
			delegate = new InitialLdapContext(ldapCredentials.getLdapEnv(), null);

			if (ldapCredentials.useStartTls) {
				tls = (StartTlsResponse) delegate.extendedOperation(new StartTlsRequest());
				tls.setHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true; // Allow STARTTLS with plain IP addresses.
					}
				});
				tls.negotiate();
				ldapCredentials.addToContext(delegate);
				delegate.reconnect(null);
			}
		}

		public void close() {
			if (tls != null) {
				try {
					tls.close();
				} catch (IOException ioe) {
					LOGGER.warn("error closing TLS connection", ioe);
				}
			}
			if (delegate != null) {
				try {
					delegate.close();
				} catch (NamingException ne) {
					LOGGER.warn("error closing LDAP context", ne);
				}
			}

		}
	}

}
