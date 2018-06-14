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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.rest.model.Action;
import org.appng.api.rest.model.ActionField;
import org.appng.api.rest.model.FieldType;
import org.springframework.http.ResponseEntity;

/**
 * Utility class that helps dealing with {@link Action}s.
 */
public class ActionHelper {

	private ResponseEntity<Action> action;

	public ActionHelper(ResponseEntity<Action> action) {
		this.action = action;
	}

	public static ActionHelper create(ResponseEntity<Action> action) {
		return new ActionHelper(action);
	}

	public ActionHelper setFieldValue(String name, Object value) {
		Optional<ActionField> field = getField(name);
		if (field.isPresent() && !isSelectionType(field.get())) {
			field.get().setValue(value);
		}
		return this;
	}

	public ActionHelper setFieldSelectionValue(String name, String value) {
		Optional<ActionField> field = getField(name);
		if (field.isPresent() && isSelectionType(field.get())) {
			field.get().getOptions().getEntries().forEach(o -> {
				if (Objects.equals(value, o.getValue())) {
					o.setSelected(true);
				}
			});
		}
		return this;
	}

	private boolean isSelectionType(ActionField actionField) {
		return Arrays.asList(FieldType.LIST_CHECKBOX, FieldType.LIST_RADIO, FieldType.LIST_SELECT)
				.contains(actionField.getFieldType());
	}

	public Optional<ActionField> getField(List<ActionField> fields, String name) {
		if (!name.contains(".")) {
			return fields.stream().filter(f -> f.getName().equals(name)).findFirst();
		}
		String[] segments = name.split("\\.");
		Optional<ActionField> root = fields.stream().filter(f -> f.getName().equals(segments[0])).findFirst();
		if (root.isPresent()) {
			return getField(root.get().getFields(),
					StringUtils.join(Arrays.copyOfRange(segments, 1, segments.length), "."));
		}
		return Optional.empty();
	}

	public Optional<ActionField> getField(String name) {
		return getField(action.getBody().getFields(), name);
	}

	public Optional<ActionField> getField(Collection<ActionField> fields, String name) {
		return fields.parallelStream().filter(f -> f.getName().equals(name)).findFirst();
	}

}
