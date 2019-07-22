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
package org.appng.api.rest;

import java.util.ArrayList;
import java.util.Optional;

import org.appng.api.rest.model.Datasource;
import org.appng.api.rest.model.Element;
import org.appng.api.rest.model.FieldValue;
import org.appng.api.rest.model.Link;
import org.appng.api.rest.model.Page;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class DataSourceHelperTest {

	@Test
	public void testSelectFieldValue() {

		Datasource datasource = new Datasource();
		Page page = new Page();
		page.setSize(1);
		page.setElements(new ArrayList<>());
		Element element = new Element();
		element.setLinks(new ArrayList<>());
		Link link = new Link();
		link.setId("foo");
		element.getLinks().add(link);
		element.setFields(new ArrayList<>());
		FieldValue field = new FieldValue();
		field.setName("name");
		field.setValue("foo");
		field.setValues(new ArrayList<>());
		FieldValue nested = new FieldValue();
		nested.setName("nested");
		nested.setValue("bar");
		field.getValues().add(nested);
		element.getFields().add(field);
		page.getElements().add(element);
		datasource.setPage(page);
		DataSourceHelper datasourceHelper = DataSourceHelper.create(new ResponseEntity<>(datasource, HttpStatus.OK));

		Optional<Element> result = datasourceHelper.getElement(0);
		Assert.assertEquals(Optional.empty(), datasourceHelper.getElement(1));
		Assert.assertEquals(Optional.empty(), datasourceHelper.getElement(-1));
		Assert.assertEquals(element, result.get());

		Assert.assertEquals(link, datasourceHelper.getLink(element, "foo").get());
		Assert.assertEquals(Optional.empty(), datasourceHelper.getLink(element, "bar"));

		Assert.assertEquals(field, datasourceHelper.getField(result.get(), "name").get());
		Assert.assertEquals(nested, datasourceHelper.getField(result.get(), "name.nested").get());

		Assert.assertEquals(Optional.empty(), datasourceHelper.getField(result.get(), "xxx"));
		Assert.assertEquals(Optional.empty(), datasourceHelper.getField(result.get(), "name.xxx"));
		Assert.assertEquals(Optional.empty(), datasourceHelper.getField(result.get(), "xxx.nested"));
	}

}
