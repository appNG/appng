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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.NoSuchElementException;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsResponse;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/*
 * Implementation of the LdapContext Interface.
 * Substitutes what is normally {@code com.sun.jndi.ldap.LdapCtx} within the private field {@code defaultInitCtx} of
 * {@InitialContext} after it has been initialized.
 * 
 * This is a partial mock with auto-generated stubs for the {@LdapContext} methods to be more flexible than a complete mock.
 * (We could cheat and extend the concrete class {@link InitialLdapContext}, but a facade class inside a facade looks funny to me.)
 */
public class LdapContextMock implements LdapContext {

	public static final String MSG_WRONG_USER = "Sorry kiddo. You got the gift, but it looks like you're waiting for something.";
	public static final String MSG_WRONG_PASS = "Ah ah ah, you didn't say the magic word!";

	public static final String MOCKED_ID_ATTR = "sAMAccountName";
	public static final String MOCKED_GROUP_NAME = "logingroup";
	public static final String[] MOCKED_GROUP_MEMBERS = new String[] { "uid=aziz,ou=users,l=egypt",
			"uid=aziz' brother,ou=users,l=egypt" };

	public boolean useTls;

	@Mock
	private StartTlsResponse mockTlsResponse;

	private Hashtable<?, ?> env;

	private String mockedUserPrincipal;
	private String mockedUserPassword;
	private String mockedServicePrincipal;
	private String mockedServicePassword;

	private String userBaseDn;
	private String groupBaseDn;

	private class MemberEnumerationMock implements NamingEnumeration<String> {
		int idx;
		String[] names;

		public MemberEnumerationMock(String[] names) {
			idx = 0;
			this.names = names;
		}

		@Override
		public boolean hasMoreElements() {
			return idx < names.length;
		}

		@Override
		public String nextElement() throws NoSuchElementException {
			if (hasMoreElements())
				return names[idx++];
			else
				throw new NoSuchElementException("Naming cache is 'a few raisins short of a full scoop'.");
		}

		@Override
		public boolean hasMore() throws NamingException {
			return idx < names.length;
		}

		@Override
		public String next() throws NamingException {
			if (hasMoreElements())
				return names[idx++];
			else
				throw new NamingException("Naming cache is 'a few raisins short of a full scoop'.");
		}

		@Override
		public void close() throws NamingException {
		}
	}

	// Keeps track of (simulated) Exceptions to make them available to the unit tests.
	public ArrayList<Exception> exceptionHistory = new ArrayList<>(2);

	public LdapContextMock(String userPrincipal, String userPassword, String servicePrincipal, String servicePassword,
			HashMap<String, Object> siteProperties) throws NamingException, IOException {
		mockedUserPrincipal = userPrincipal != null ? userPrincipal : "not set";
		mockedUserPassword = userPassword != null ? userPassword : "not set";
		mockedServicePrincipal = servicePrincipal != null ? servicePrincipal : "not set";
		mockedServicePassword = servicePassword != null ? servicePassword : "not set";

		userBaseDn = (String) siteProperties.get(LdapService.LDAP_USER_BASE_DN);
		groupBaseDn = (String) siteProperties.get(LdapService.LDAP_GROUP_BASE_DN);

		MockitoAnnotations.initMocks(this);
		Object startTlsProp = siteProperties.get(LdapService.LDAP_START_TLS);
		if (startTlsProp != null && ((Boolean) startTlsProp).booleanValue()) {
			useTls = true;
			Mockito.when(mockTlsResponse.negotiate()).thenReturn(null);
		} else {
			useTls = false;
			Mockito.when(mockTlsResponse.negotiate()).thenThrow(new IOException("TLS intentionally failed."));
		}
	}

	public void init(Hashtable<?, ?> env) {
		this.env = env;

	}

	private <T extends Exception> void stackAndThrow(T ex) throws T {
		exceptionHistory.add(ex);
		throw ex;
	}

	@Override
	public ExtendedResponse extendedOperation(ExtendedRequest request) throws NamingException {
		return mockTlsResponse;
	}

	@Override
	public void reconnect(Control[] connCtls) throws NamingException {
		String envPrincipal = (String) env.get(Context.SECURITY_PRINCIPAL);
		String envPassword = (String) env.get(Context.SECURITY_CREDENTIALS);

		if (mockedServicePrincipal.equalsIgnoreCase(envPrincipal)) {
			if (!mockedServicePassword.equals(envPassword))
				stackAndThrow(new NamingException(MSG_WRONG_PASS));
		} else {
			if (!mockedUserPrincipal.equalsIgnoreCase(envPrincipal))
				stackAndThrow(new NamingException(MSG_WRONG_USER));
			if (!mockedUserPassword.equals(envPassword))
				stackAndThrow(new NamingException(MSG_WRONG_PASS));
		}
	}

	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		if (name.endsWith(groupBaseDn) && null != attrIds) {
			Answer<NamingEnumeration<?>> attrEnumAnswer;
			Attributes groupAttrs = Mockito.mock(Attributes.class);
			Attribute memberAttr = Mockito.mock(Attribute.class);

			if (name.toLowerCase().startsWith(("cn=" + MOCKED_GROUP_NAME).toLowerCase())) {
				attrEnumAnswer = new Answer<NamingEnumeration<?>>() {
					public NamingEnumeration<String> answer(InvocationOnMock invocation) throws Throwable {
						return new MemberEnumerationMock(MOCKED_GROUP_MEMBERS);
					}
				};
			} else {
				attrEnumAnswer = new Answer<NamingEnumeration<?>>() {

					public NamingEnumeration<String> answer(InvocationOnMock invocation) throws Throwable {
						return new MemberEnumerationMock(new String[0]);
					}
				};
			}

			Mockito.when(groupAttrs.get("member")).thenReturn(memberAttr);
			Mockito.when(memberAttr.getAll()).thenAnswer(attrEnumAnswer);

			return groupAttrs;
		} else if (name.endsWith(userBaseDn)) {

			Attributes memberAttrs = Mockito.mock(Attributes.class);

			Attribute idAttr = Mockito.mock(Attribute.class);
			if (name.equals(MOCKED_GROUP_MEMBERS[0])) {
				Mockito.when(idAttr.get()).thenReturn("aziz");
				Mockito.when(memberAttrs.get(MOCKED_ID_ATTR)).thenReturn(idAttr);
			} else {
				Mockito.when(idAttr.get()).thenReturn("aziz' brother");
				Mockito.when(memberAttrs.get(MOCKED_ID_ATTR)).thenReturn(idAttr);
			}

			Attribute dummyAttr = Mockito.mock(Attribute.class);
			Mockito.when(dummyAttr.get()).thenReturn("Dummy");
			// Is there a way to say "Mockito.notmatches()"? The negative lookaround regex works, but is a bit overkill.
			Mockito.when(memberAttrs.get(Mockito.matches("^((?!" + MOCKED_ID_ATTR + ").)*$"))).thenReturn(dummyAttr);

			return memberAttrs;
		} else {
			throw new NamingException(
					"No attributes are prepared to be be returned from DN '" + name + "'. Error in unit tests!");
		}
	}

	@Override
	public void bind(Name arg0, Object arg1, Attributes arg2) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void bind(String arg0, Object arg1, Attributes arg2) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public DirContext createSubcontext(Name arg0, Attributes arg1) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public DirContext createSubcontext(String arg0, Attributes arg1) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Attributes getAttributes(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Attributes getAttributes(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Attributes getAttributes(Name arg0, String[] arg1) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public DirContext getSchema(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public DirContext getSchema(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public DirContext getSchemaClassDefinition(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public DirContext getSchemaClassDefinition(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void modifyAttributes(Name arg0, ModificationItem[] arg1) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void modifyAttributes(String arg0, ModificationItem[] arg1) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void modifyAttributes(Name arg0, int arg1, Attributes arg2) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void modifyAttributes(String arg0, int arg1, Attributes arg2) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void rebind(Name arg0, Object arg1, Attributes arg2) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void rebind(String arg0, Object arg1, Attributes arg2) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public NamingEnumeration<SearchResult> search(Name arg0, Attributes arg1) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(String arg0, Attributes arg1) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name arg0, Attributes arg1, String[] arg2) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(String arg0, Attributes arg1, String[] arg2) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name arg0, String arg1, SearchControls arg2) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(String arg0, String arg1, SearchControls arg2)
			throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name arg0, String arg1, Object[] arg2, SearchControls arg3)
			throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<SearchResult> search(String arg0, String arg1, Object[] arg2, SearchControls arg3)
			throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Object addToEnvironment(String arg0, Object arg1) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void bind(Name arg0, Object arg1) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void bind(String arg0, Object arg1) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void close() throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public Name composeName(Name arg0, Name arg1) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public String composeName(String arg0, String arg1) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Context createSubcontext(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Context createSubcontext(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void destroySubcontext(Name arg0) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void destroySubcontext(String arg0) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public Hashtable<?, ?> getEnvironment() throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public String getNameInNamespace() throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NameParser getNameParser(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NameParser getNameParser(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<NameClassPair> list(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<Binding> listBindings(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public NamingEnumeration<Binding> listBindings(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Object lookup(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Object lookup(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupLink(Name arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupLink(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void rebind(Name arg0, Object arg1) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void rebind(String arg0, Object arg1) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public Object removeFromEnvironment(String arg0) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void rename(Name arg0, Name arg1) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void rename(String arg0, String arg1) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void unbind(Name arg0) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public void unbind(String arg0) throws NamingException {
		// Auto-generated method stub

	}

	@Override
	public Control[] getConnectControls() throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Control[] getRequestControls() throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public Control[] getResponseControls() throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public LdapContext newInstance(Control[] requestControls) throws NamingException {
		// Auto-generated method stub
		return null;
	}

	@Override
	public void setRequestControls(Control[] requestControls) throws NamingException {
		// Auto-generated method stub

	}

}
