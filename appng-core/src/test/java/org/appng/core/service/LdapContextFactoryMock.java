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
import java.util.HashMap;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

public class LdapContextFactoryMock implements InitialContextFactory {

	private static HashMap<String, LdapContextMock> contexts = new HashMap<>();

	public static LdapContextMock setup(String userPrincipal, String userPassword, String servicePrincipal,
			String servicePassword, HashMap<String, Object> siteProperties) throws NamingException, IOException {
		// We need something to identify the context to be returned, when JNDI calls getInitialContext().
		// PROVIDER_URL is used as key, because it is irrelevant for the tests (will be in Context.PROVIDER_URL later).
		String testId = (String) siteProperties.get(LdapService.LDAP_HOST);

		LdapContextMock ctx;
		if (!contexts.containsKey(testId)) {
			ctx = new LdapContextMock(userPrincipal, userPassword, servicePrincipal, servicePassword, siteProperties);
			contexts.put(testId, ctx);
		} else {
			ctx = contexts.get(testId);
		}
		return ctx;
	}

	@Override
	public Context getInitialContext(Hashtable<?, ?> env) throws NamingException {
		String testId = (String) env.get(Context.PROVIDER_URL);
		if (!contexts.containsKey(testId))
			throw new NamingException("No context prepared for testId '" + testId + "'. Error in unit tests!");

		LdapContextMock ctx = contexts.get(testId);
		ctx.init(env);
		if (!ctx.useTls)
			ctx.reconnect(null);

		return ctx;
	}
}
