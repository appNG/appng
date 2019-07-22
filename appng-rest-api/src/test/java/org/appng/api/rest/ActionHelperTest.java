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
		ActionField field = getSelectionField();
		Option a = getOption("a", false);
		field.getOptions().getEntries().add(a);
		Option b = getOption("b", false);
		field.getOptions().getEntries().add(b);
		Action action = actionEntity.getBody();
		action.setFields(new ArrayList<>());
		action.getFields().add(field);

		ActionHelper actionHelper = ActionHelper.create(actionEntity);
		actionHelper.setFieldSelectionValue(field.getName(), "a");
		Assert.assertTrue(a.isSelected());
		Assert.assertFalse(b.isSelected());

		actionHelper.setFieldSelectionValue(field.getName(), "b");
		Assert.assertTrue(b.isSelected());
		Assert.assertFalse(a.isSelected());

		field.getOptions().setMultiple(true);

		actionHelper.setFieldSelectionValue(field.getName(), "a");
		Assert.assertTrue(a.isSelected());
		Assert.assertTrue(b.isSelected());
	}

	@Test
	public void testDeselectAllOptions() {
		ResponseEntity<Action> actionEntity = new ResponseEntity<>(new Action(), HttpStatus.OK);
		ActionField field = getSelectionField();
		Option a = getOption("a", true);
		field.getOptions().getEntries().add(a);
		Option b = getOption("b", true);
		field.getOptions().getEntries().add(b);
		Action action = actionEntity.getBody();
		action.setFields(new ArrayList<>());
		action.getFields().add(field);

		ActionHelper.create(actionEntity).deselectAllOptions(field.getName());

		Assert.assertFalse(a.isSelected());
		Assert.assertFalse(b.isSelected());
	}

	private ActionField getSelectionField() {
		ActionField field = new ActionField();
		field.setName("selection");
		field.setFieldType(FieldType.LIST_SELECT);
		Options options = new Options();
		field.setOptions(options);
		options.setEntries(new ArrayList<>());
		return field;
	}

	private Option getOption(String value, boolean selected) {
		Option o = new Option();
		o.setSelected(selected);
		o.setValue(value);
		return o;
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

	@Test
	public void testFlatFieldWithNestedName() {
		Action action = new Action();
		ResponseEntity<Action> actionEntity = new ResponseEntity<>(action, HttpStatus.OK);
		ActionField field = new ActionField();
		field.setName("field.name");
		field.setFieldType(FieldType.TEXT);
		action.setFields(new ArrayList<>());
		action.getFields().add(field);

		ActionHelper actionHelper = ActionHelper.create(actionEntity);

		Assert.assertEquals(field, actionHelper.getField("field.name").get());
		Assert.assertEquals(Optional.empty(), actionHelper.getField("fieldd"));
	}

}
