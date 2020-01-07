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
package org.appng.api;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.xml.platform.Action;
import org.appng.xml.platform.Data;
import org.appng.xml.platform.DataConfig;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.DatasourceRef;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.SectionelementDef;

/**
 * An {@link DataProvider} usually retrieves some data from the persistence layer and returns it as a
 * {@link DataContainer}. The implementing class needs to be defined in the application's {@code beans.xml}. This bean is
 * then being referenced within an {@link Action} or a {@link SectionelementDef} using a {@link DatasourceRef}.
 * <p>
 * 
 * @author Matthias Müller
 * 
 */
public interface DataProvider {

	/**
	 * Retrieves some data from the application, wrapped in a {@link DataContainer}.
	 * <p>
	 * Either {@link DataContainer#setItem(Object)} or one of
	 * <ul>
	 * <li> {@link DataContainer#setItems(java.util.Collection)}
	 * <li>
	 * {@link DataContainer#setPage(java.util.Collection, org.springframework.data.domain.Pageable)}
	 * <li> {@link DataContainer#setPage(org.springframework.data.domain.Page)}
	 * </ul>
	 * 
	 * must have been called on the returned {@link DataContainer}.
	 * <p>
	 * Those item(s) need(s) to be of the type defined in {@link MetaData#getBindClass()} of the {@link Datasource}'s
	 * {@link DataConfig}.
	 * 
	 * @param site
	 *            the current {@link Site}
	 * @param application
	 *            the current {@link Application}
	 * @param environment
	 *            the current {@link Environment}
	 * @param options
	 *            the {@link Options} for this {@link DataProvider}
	 * @param request
	 *            the current {@link Request}
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} containing all readable {@link FieldDef}initions for this DataProvider
	 * @return a {@link DataContainer} holding the {@link Data} for the defining {@link Datasource}
	 */
	DataContainer getData(Site site, Application application, Environment environment, Options options, Request request,
			FieldProcessor fieldProcessor);

}
