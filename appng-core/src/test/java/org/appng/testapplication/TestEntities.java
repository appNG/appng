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
package org.appng.testapplication;

import java.util.ArrayList;
import java.util.List;

import org.appng.api.ActionProvider;
import org.appng.api.ApplicationException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.SelectionFactory;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionType;

public class TestEntities implements ActionProvider<TestEntity>, DataProvider {

	private static TestEntity t1 = new TestEntity(1, "entity1", 5, 5.5d, false);

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		DataContainer dataContainer = new DataContainer(fp);
		String action = options.getOptionValue("action", "id");
		if ("2".equals(options.getOptionValue("entity", "id"))) {
			fp.addOkMessage("this message should not appear");
			throw new ApplicationException("adfsf", null);
		}
		String id = options.getOptionValue("entity", "id");

		if ("create".equals(action)) {
			dataContainer.setItem(new TestEntity());
		} else if (null == id) {
			List<TestEntity> entities = new ArrayList<>();
			entities.add(t1);
			entities.add(new TestEntity(2, "entity2", 7, 7.8d, true));
			dataContainer.setPage(entities, fp.getPageable());
		} else if (id.equals("1")) {
			Selection simpleSelection = new SelectionFactory().fromObjects("integerValue", "integerValue",
					new String[] { "1", "2", "3", "4", "5" }, "5");
			simpleSelection.setType(SelectionType.CHECKBOX);
			dataContainer.getSelections().add(simpleSelection);
			dataContainer.setItem(t1);
		}
		return dataContainer;
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			TestEntity valueHolder, FieldProcessor fp) {
		String name = valueHolder.getName();
		if ("exception".equals(name)) {
			fp.addOkMessage("this message should not appear");
			throw new ApplicationException("adfsf", null);
		} else {
			t1.setName(name);
		}
		fp.addOkMessage("executed action " + options.getOptionValue("action", "id"));
	}

}
