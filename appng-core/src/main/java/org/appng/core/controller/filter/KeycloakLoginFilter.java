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
package org.appng.core.controller.filter;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.service.CoreService;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * A {@link Filter} that looks for a {@link KeycloakPrincipal} (respectively an {@link AccessToken}) in the current
 * {@link HttpServletRequest}.
 * <p>
 * <strong>Prerequisites</strong>
 * <ul>
 * <li>The <a href="https://www.keycloak.org/docs/latest/securing_apps/index.html#_tomcat_adapter">Keycloak Tomcat
 * Adapter</a> must be installed and set up, defining the {@code KeycloakAuthenticatorValve} in the server's
 * {@code context.xml}
 * <li>A Keycloak client must be set up, the corresponding {@code keycloak.json} must be present in {@code WEB-INF}
 * <li>Keycloak must be enabled in {@code WEB-INF/web.xml} by removing the comments from the Keycloak related elements.
 * <li>For the Keycloak client, a role named 'appNG Keycloak User' must exist, since this role is used in the
 * {@code <security-constraint>}. The name of that role can be configured using
 * {@link Platform.Property#KEYCLOAK_SECURITY_ROLE}.
 * </ul>
 * </p>
 * If a {@link KeycloakPrincipal} is found in the request (and there is no authenticated user), there are two possible
 * scenarios:
 * <ol>
 * <li>The token contains <strong>no</strong> appNG group names<br/>
 * In that case, the filter tries to log-in the <strong>local</strong> user identified by
 * {@link AccessToken#getPreferredUsername()}.<br/>
 * In other words, the Keycloak username must match the appNG username.</li>
 * <li>The token contains some appNG group names<br/>
 * In that case, the filter tries to log-in a user identified by {@link AccessToken#getPreferredUsername()} with the
 * given groups.<br/>
 * To make this work,
 * <ul>
 * <li>create roles in the Keycloak client whose names match the names of appNG user groups
 * <li>create a mapper in the Keycloak client of type 'User Client Role'
 * <ul>
 * <li>set the 'Client Role prefix' to 'appng_' (see {@link Platform.Property#KEYCLOAK_GROUP_PREFIX})</li>
 * <li>set the 'Token Claim Name' to 'appNG Groups' (see {@link Platform.Property#KEYCLOAK_GROUP_CLAIM_NAME})</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * </ol>
 * 
 * @author Matthias Müller
 */
public class KeycloakLoginFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakLoginFilter.class);
	private String groupPrefix;
	private String claimName;
	private String securityRole;
	private ApplicationContext appngContext;

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Environment env = EnvironmentFilter.environment();
		Principal principal = ((HttpServletRequest) request).getUserPrincipal();
		if (!env.isSubjectAuthenticated() && null != principal) {
			if (KeycloakPrincipal.class.isAssignableFrom(principal.getClass())) {
				KeycloakPrincipal<?> keycloakPrincipal = KeycloakPrincipal.class.cast(principal);
				KeycloakSecurityContext securityContext = keycloakPrincipal.getKeycloakSecurityContext();
				AccessToken token = securityContext.getToken();
				List<String> appNGGroups = extractGroupNames(token);
				String userName = token.getPreferredUsername();
				if (appNGGroups.isEmpty()) {
					getCoreService().loginByUserName(env, userName);
					LOGGER.info("Logging in {}", userName);
				} else {
					String realName = token.getGivenName() + " " + token.getFamilyName();
					boolean success = getCoreService().loginUserWithGroups(env, userName, token.getEmail(), realName,
							appNGGroups);
					LOGGER.info("Logging in {} with groups {} {}.", userName, appNGGroups,
							success ? "succeeded" : "failed");
				}
			}
		}
		chain.doFilter(request, response);
	}

	private CoreService getCoreService() {
		return appngContext.getBean(CoreService.class);
	}

	@SuppressWarnings("unchecked")
	private List<String> extractGroupNames(AccessToken token) {
		List<String> claimGroups = (List<String>) token.getOtherClaims().get(claimName);
		if (null != claimGroups) {
			return claimGroups.stream().filter(g -> !g.equals(securityRole))
					.map(g -> g.replace(groupPrefix, StringUtils.EMPTY)).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		DefaultEnvironment env = DefaultEnvironment.getGlobal();
		Properties platformConfig = env.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		claimName = platformConfig.getString(Platform.Property.KEYCLOAK_GROUP_CLAIM_NAME, "appNG Groups");
		groupPrefix = platformConfig.getString(Platform.Property.KEYCLOAK_GROUP_PREFIX, "appng_");
		securityRole = platformConfig.getString(Platform.Property.KEYCLOAK_SECURITY_ROLE, "appNG Keycloak User");
		appngContext = env.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
	}

	public void destroy() {
	}

}
