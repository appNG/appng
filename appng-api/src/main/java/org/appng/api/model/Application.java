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
package org.appng.api.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.appng.api.Environment;

/**
 * A {@link Application} is a component that adds certain functionality to the platform. It owns several
 * {@link Resource}s of different types. For each {@link Application}, an own
 * {@link org.springframework.context.ApplicationContext} is build, based upon the {@link Application}'s
 * {@value org.appng.api.model.ResourceType#BEANS_XML_NAME}.
 * 
 * @author Matthias Müller
 */
public interface Application extends Identifier {

	/**
	 * Returns the {@link Permission}s for this {@link Application}.
	 * 
	 * @return the {@link Permission}s
	 */
	Set<Permission> getPermissions();

	/**
	 * Returns {@link Role}s for this {@link Application}.
	 * 
	 * @return the {@link Role}s
	 */
	Set<Role> getRoles();

	/**
	 * the {@link Resources} for this {@link Application}.
	 * 
	 * @return the {@link Resources} for this {@link Application}
	 */
	Resources getResources();

	/**
	 * Returns the {@link Resource}s for this {@link Application}.
	 * 
	 * @return the {@link Resource}s
	 */
	Set<Resource> getResourceSet();

	/**
	 * Returns the {@link Properties}s for this {@link Application}.
	 * 
	 * @return the {@link Properties}s
	 */
	Properties getProperties();

	/**
	 * Retrieves the bean of the given name and type from the underlying
	 * {@link org.springframework.context.ApplicationContext}, if any.
	 * 
	 * @param name
	 *             the name of the bean to retrieve
	 * @param type
	 *             the type of the bean to retrieve
	 * 
	 * @return the bean with the given name of the given type, if any.
	 */
	<T> T getBean(String name, Class<T> type);

	/**
	 * Returns the names for all beans of the given type.
	 * 
	 * @param type
	 *             the type to get the bean names for
	 * 
	 * @return the bean names
	 */
	String[] getBeanNames(Class<?> type);

	/**
	 * Returns the single bean of the given type, if any.
	 * 
	 * @param type
	 *             the type of the bean
	 * 
	 * @return the bean of the given type, or {@code null} if no such bean exists.
	 */
	<T> T getBean(Class<T> type);

	/**
	 * Returns the single bean with the given name, if any.
	 * 
	 * @param beanName
	 *                 the name of the bean
	 * 
	 * @return the bean with the given name, or {@code null} if no such bean exists.
	 */
	Object getBean(String beanName);

	/**
	 * Checks whether a bean with the given name exists for this {@link Application}.
	 * 
	 * @param beanName
	 *                 the name of the bean to check existence
	 * 
	 * @return {@code true} if such a bean exists, {@code false} otherwise
	 */
	boolean containsBean(String beanName);

	/**
	 * Returns {@code true} if this {@link Application} is filebased, which means it's original {@link Resource}s are
	 * stored on disk, {@code false} if the {@link Resource}s are stored in the database.
	 * 
	 * @return {@code true} if this {@code Application} is filebased, {@code false} otherwise
	 */
	boolean isFileBased();

	/**
	 * Returns {@code true} if this {@link Application} is a privileged application, which means it has access to the
	 * platform's root context and the platform-scoped {@link Environment}.
	 * 
	 * @return {@code true} if this {@code Application} is a privileged application, {@code false} otherwise
	 */
	boolean isPrivileged();

	/**
	 * Returns {@code true} if this {@link Application} is hidden, which means it does not appear in the navigation
	 * menu.
	 * 
	 * @return {@code true} if this {@link Application} is hidden
	 */
	boolean isHidden();

	/**
	 * Retrieves a message from the underlying {@link org.springframework.context.MessageSource}.
	 * 
	 * @param locale
	 *               the {@link Locale} for the message
	 * @param key
	 *               the message-key
	 * @param args
	 *               the arguments for the message
	 * 
	 * @return the message
	 * 
	 * @see ResourceType#DICTIONARY
	 */
	String getMessage(Locale locale, String key, Object... args);

	/**
	 * Returns the key to retrieve the session-parameters for this {@code Application} within the given {@link Site}
	 * from an {@link Environment}.
	 * 
	 * @param site
	 *             the {@link Site} to retrieve the key for
	 * 
	 * @return the key to retrieve the session-parameters
	 * 
	 * @see #getSessionParams(Site, Environment)
	 */
	String getSessionParamKey(Site site);

	/**
	 * Retrieves the session-parameters for for this {@code Application} within the given {@link Site} from the given
	 * {@link Environment}.
	 * 
	 * @param site
	 *                    the {@link Site} to retrieve the session parameters for
	 * @param environment
	 *                    the current {@link Environment}
	 * 
	 * @return the session-parameters for this {@code Application} within the given {@link Site}
	 * 
	 * @see #getSessionParamKey(Site)
	 */
	Map<String, String> getSessionParams(Site site, Environment environment);

	/**
	 * Returns the {@link FeatureProvider} for this {@link Application}.
	 * 
	 * @return the {@link FeatureProvider}
	 */
	FeatureProvider getFeatureProvider();

	/**
	 * Returns the {@link ApplicationSubject}s for this {@link Application}
	 * 
	 * @return the {@link ApplicationSubject}s
	 */
	List<ApplicationSubject> getApplicationSubjects();

}
