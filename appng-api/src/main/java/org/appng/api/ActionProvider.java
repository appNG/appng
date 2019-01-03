/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.api;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.Bean;
import org.appng.xml.platform.BeanOption;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;

/**
 * An {@link ActionProvider} usually performs a CRUD-action on a domain object. The implementing class needs to be
 * defined in the application's {@code beans.xml}. This bean is then being referenced in a concrete {@link Action}:
 * 
 * <pre>
 *  &lt;action id="update">
 *     &lt;config>
 *         &lt;title id="issue.update" />
 *         &lt;params>
 *             &lt;param name="form_action" />
 *             &lt;param name="issueName" />
 *             &lt;param name="edit" />
 *         &lt;/params>
 *     &lt;/config>
 *     &lt;condition expression="${form_action eq 'update' and not empty issueName}" />
 *     &lt;datasource id="issue">
 *         &lt;params>
 *             &lt;param name="issueName">${issueName}&lt;/param>
 *             &lt;param name="edit">${edit}&lt;/param>
 *         &lt;/params>
 *     &lt;/datasource>
 *     &lt;bean id="issues">
 *         &lt;option name="action" id="update" issue="${issueName}" />
 *     &lt;/bean>
 * &lt;/action>
 * </pre>
 * 
 * There is no need to implement one {@link ActionProvider} per {@link Action}, since the {@link Bean} can be
 * parameterized using different {@link BeanOption}s. So for example, the above defined bean could also be used for
 * creating a new domain object, using another {@link BeanOption}:
 * 
 * <pre>
 * &lt;action id="create">
 *     &lt;config>
 *         &lt;title id="issue.create" />
 *     &lt;/config>
 *     &lt;condition expression="${form_action eq 'create'}" />
 *     &lt;datasource id="new_issue">
 *         &lt;params>
 *         &lt;param name="form_action" />
 *         &lt;/params>
 *     &lt;/datasource>
 *     &lt;bean id="issues">
 *         &lt;option name="action" id="create" />
 *     &lt;/bean>
 * &lt;/action>
 * </pre>
 * 
 * The distinction between create and update then can be done like this:
 * 
 * <pre>
 * <code>
 * if ("create".equals(options.getOptionValue("action", "id"))) {
 *     // create new domain object
 * } else if ("update".equals(options.getOptionValue("action", "id"))){
 *     // update existing domain object
 * }</code>
 * </pre>
 * 
 * @author Matthias Müller
 * 
 * @param <T>
 *            the bean type for this {@link ActionProvider}s {@code perform(...)}-method. Must match
 *            {@link MetaData#getBindClass()} of the {@link Datasource} used by the {@link Action}
 */
public interface ActionProvider<T> {

	/**
	 * Performs an action on the given {@code formBean}
	 * 
	 * @param site
	 *            the current {@link Site}
	 * @param application
	 *            the current {@link Application}
	 * @param environment
	 *            the current {@link Environment}
	 * @param options
	 *            the {@link Options} for this {@link ActionProvider}
	 * @param request
	 *            the current {@link Request}
	 * @param formBean
	 *            the bean which has been created
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} containing all writable {@link FieldDef}initions for this action
	 */
	void perform(Site site, Application application, Environment environment, Options options, Request request, T formBean,
			FieldProcessor fieldProcessor);

}
