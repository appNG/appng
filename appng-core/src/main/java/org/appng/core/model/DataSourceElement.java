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
package org.appng.core.model;

import org.appng.api.ParameterSupport;
import org.appng.api.ProcessingException;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.ApplicationRequest;
import org.appng.api.support.CallableDataSource;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.PageDefinition;
import org.appng.xml.platform.PageReference;
import org.appng.xml.platform.Sectionelement;
import org.appng.xml.platform.SectionelementDef;

/**
 * A {@link Sectionelement} that contains a {@link Datasource}. This is mainly a delegate to a
 * {@link CallableDataSource}.
 * 
 * @author Matthias Müller
 */
class DataSourceElement extends Sectionelement {

	private CallableDataSource callableDataSource;

	/**
	 * Creates a new {@code DataSourceElement}.
	 * 
	 * @param site
	 *                           the current {@link Site}
	 * @param application
	 *                           the current {@link Application}
	 * @param applicationRequest
	 *                           the current {@link ApplicationRequest}
	 * @param parameterSupport
	 *                           a {@link ParameterSupport} containing the parameters of the {@link PageDefinition}
	 * @param datasourceRef
	 *                           the {@link DatasourceRef} as given in the {@link SectionelementDef} of a
	 *                           {@link PageDefinition}.
	 * 
	 * @throws ProcessingException
	 *                             if an error occurs while assembling the {@code DataSourceElement}
	 */
	DataSourceElement(Site site, Application application, ApplicationRequest applicationRequest,
			ParameterSupport parameterSupport, DatasourceRef datasourceRef) throws ProcessingException {
		this.callableDataSource = new CallableDataSource(site, application, applicationRequest, parameterSupport,
				datasourceRef);
		this.datasource = callableDataSource.getDatasource();
	}

	/**
	 * Checks if this {@code DataSourceElement} should be included in the {@link PageReference}.
	 * 
	 * @return {@code true} if this {@code DataSourceElement} should be included in the {@link PageReference},
	 *         {@code false} otherwise
	 */
	boolean doInclude() {
		return callableDataSource.doInclude();
	}

	/**
	 * Delegates to {@link CallableDataSource#perform(String)}.
	 */
	Data perform(String pageId) throws ProcessingException {
		return perform(pageId, false);
	}

	/**
	 * Delegates to {@link CallableDataSource#perform(String, boolean)}
	 */
	Data perform(String pageId, boolean addMessagesToSession) throws ProcessingException {
		return callableDataSource.perform(pageId, addMessagesToSession);
	}

}
