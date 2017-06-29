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
package org.appng.api;

import java.sql.ResultSet;
import java.util.Collection;

import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.springframework.data.domain.Page;

/**
 * 
 * Provides methods to transform a bindobject to a {@link Result} and a {@link Page} of bindobjects to a
 * {@link Resultset}.
 * 
 * @author Matthias Müller
 * 
 */
public interface ResultService {

	/**
	 * Builds a {@link ResultSet} from the items provided by the {@link Page}, using the {@link FieldProcessor}'s
	 * {@link MetaData}.
	 * 
	 * @param fp
	 *            a {@link FieldProcessor}
	 * @param page
	 *            a {@link Page} of bindobjects
	 * @return a {@link Resultset} containing a {@link Result} for each item of the {@link Page}. Every {@link Result}
	 *         contains a {@link Datafield} for each {@link FieldDef} provided by the {@link FieldProcessor}'s
	 *         {@link MetaData}.
	 */
	Resultset getResultset(FieldProcessor fp, Page<?> page);

	/**
	 * Builds a {@link ResultSet} from the items provided by the {@link Collection}, using the {@link FieldProcessor}'s
	 * {@link MetaData}.<br />
	 * <b>Note that pagination will not be available.</b>
	 * 
	 * @param fp
	 *            a {@link FieldProcessor}
	 * @param items
	 *            a {@link Collection} of bindobjects
	 * @return a {@link Resultset} containing a {@link Result} for each item of the {@link Collection}. Every
	 *         {@link Result} contains a {@link Datafield} for each {@link FieldDef} provided by the
	 *         {@link FieldProcessor}'s {@link MetaData}.
	 */
	Resultset getResultset(FieldProcessor fp, Collection<?> items);

	/**
	 * Builds a {@link Result} from the provided bindObject, using the {@link FieldProcessor}'s {@link MetaData}.
	 * 
	 * @param fp
	 *            a {@link FieldProcessor}
	 * @param bindObject
	 *            a bindobject
	 * @return a {@link Result} containing a {@link Datafield} for each {@link FieldDef} provided by the
	 *         {@link FieldProcessor}'s {@link MetaData}.
	 */
	Result getResult(FieldProcessor fp, Object bindObject);

}
