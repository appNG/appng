/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.core.controller.filter.session;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.support.environment.DefaultEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A {@link Filter} that delegates the actual work to a {@link SessionRepositoryFilter}, but only if the requested path
 * does not represent a template resource.
 * 
 * <p>
 * Add the following to {@code web.xml}
 * 
 * <pre>
 * 	&lt;filter>
 *	    &lt;filter-name>SessionFilterDelegate&lt;/filter-name>
 *	    &lt;filter-class>org.appng.core.controller.filter.session.SessionFilterDelegate&lt;/filter-class>
 *	&lt;/filter>
 *	&lt;filter-mapping>
 *	    &lt;filter-name>SessionRepositoryFilter&lt;/filter-name>
 *	    &lt;servlet-name>controller&lt;/servlet-name>
 *	    &lt;dispatcher>REQUEST&lt;/dispatcher>
 *	&lt;/filter-mapping>
 * </pre>
 * 
 * In {@code platformContext.xml} a {@link MongoOperationsSessionRepository} and a {@link SessionRepositoryFilter} need
 * to be defined:
 * 
 * <pre>
 * &lt;bean id="sessionRepository" class="org.springframework.session.data.mongo.MongoOperationsSessionRepository">
 *	&lt;constructor-arg>
 *		&lt;bean class="org.springframework.data.mongodb.core.MongoTemplate">
 *			&lt;constructor-arg name="mongo">
 *				&lt;bean class="com.mongodb.MongoClient">
 *					&lt;constructor-arg type="com.mongodb.ServerAddress">
 *						&lt;bean class="com.mongodb.ServerAddress" />
 *					&lt;/constructor-arg>
 *				&lt;/bean>
 *			&lt;/constructor-arg>
 *			&lt;constructor-arg name="databaseName" value="spring_tomcat_sessions" />
 *		&lt;/bean>
 *	&lt;/constructor-arg>
 *	&lt;property name="collectionName" value="sessions" />
 * &lt;/bean>
 *
 * &lt;bean class="org.springframework.session.web.http.SessionRepositoryFilter">
 *	&lt;constructor-arg ref="sessionRepository" />
 * &lt;/bean>
 * </pre>
 * 
 * @author Matthias Müller
 *
 */
public class SessionFilterDelegate extends OncePerRequestFilter {

	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		DefaultEnvironment environment = DefaultEnvironment.get(request.getServletContext());
		Properties platformConfig = environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		String templatePrefix = platformConfig.getString(Platform.Property.TEMPLATE_PREFIX);

		if (!request.getServletPath().startsWith(templatePrefix)) {
			ApplicationContext ctx = environment.getAttribute(Scope.PLATFORM,
					Platform.Environment.CORE_PLATFORM_CONTEXT);
			SessionRepositoryFilter<?> delegate = ctx.getBean(SessionRepositoryFilter.class);
			delegate.doFilter(request, response, filterChain);
		} else {
			filterChain.doFilter(request, response);
		}
	}

}
