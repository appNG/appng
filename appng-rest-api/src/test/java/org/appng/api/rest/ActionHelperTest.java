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
package org.appng.api.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.appng.api.rest.model.Action;
import org.appng.api.rest.model.ActionField;
import org.appng.api.rest.model.FieldType;
import org.appng.api.rest.model.Option;
import org.appng.api.rest.model.Options;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ActionHelperTest {

	@Test
	public void testSelectFieldValue() {
		ResponseEntity<Action> actionEntity = new ResponseEntity<>(new Action(), HttpStatus.OK);
		ActionField field = new ActionField();
		field.setName("selection");
		field.setFieldType(FieldType.LIST_SELECT);
		Options options = new Options();
		field.setOptions(options);
		Option a = new Option();
		a.setValue("a");
		options.setEntries(new ArrayList<>());
		options.getEntries().add(a);
		Option b = new Option();
		b.setValue("b");
		options.getEntries().add(b);
		Action action = actionEntity.getBody();
		action.setFields(new ArrayList<>());
		action.getFields().add(field);

		ActionHelper actionHelper = ActionHelper.create(actionEntity);
		actionHelper.setFieldSelectionValue("selection", "a");
		Assert.assertTrue(a.isSelected());
		Assert.assertFalse(b.isSelected());

		actionHelper.setFieldSelectionValue("selection", "b");
		Assert.assertTrue(b.isSelected());
		Assert.assertFalse(a.isSelected());

		options.setMultiple(true);

		actionHelper.setFieldSelectionValue("selection", "a");
		Assert.assertTrue(a.isSelected());
		Assert.assertTrue(b.isSelected());
	}

	@Test
	public void testSetFieldValue() {
		Action action = new Action();
		ResponseEntity<Action> actionEntity = new ResponseEntity<>(action, HttpStatus.OK);
		ActionField field = new ActionField();
		field.setName("field");
		field.setFieldType(FieldType.TEXT);
		action.setFields(new ArrayList<>());
		action.getFields().add(field);

		ActionHelper actionHelper = ActionHelper.create(actionEntity);

		Assert.assertNull(actionHelper.getField("field").get().getValue());

		actionHelper.setFieldValue("field", "a");
		Assert.assertEquals("a", actionHelper.getField("field").get().getValue());
	}

	@Test
	public void testGetNestedField() {
		Action action = new Action();
		ResponseEntity<Action> actionEntity = new ResponseEntity<>(action, HttpStatus.OK);
		ActionField field = new ActionField();
		field.setName("field");
		field.setFieldType(FieldType.OBJECT);
		action.setFields(new ArrayList<>());
		action.getFields().add(field);
		ActionField nested = new ActionField();
		nested.setName("nested");
		field.setFields(Arrays.asList(nested));

		ActionHelper actionHelper = ActionHelper.create(actionEntity);
		Assert.assertEquals(nested, actionHelper.getField("field.nested").get());

		Assert.assertEquals(Optional.empty(), actionHelper.getField("field.xxx"));
		Assert.assertEquals(Optional.empty(), actionHelper.getField("xxx.nested"));
	}

}
