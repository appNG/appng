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
package org.appng.api.support.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Label;
import org.slf4j.Logger;
import org.springframework.beans.BeanWrapper;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link FieldConverter} for {@link FieldDef}initions of type
 * <ul>
 * <li>{@link FieldType#LIST_CHECKBOX}</li>
 * <li>{@link FieldType#LIST_RADIO}</li>
 * <li>{@link FieldType#LIST_SELECT}</li>
 * <li>{@link FieldType#LIST_TEXT}</li>
 * <li>{@link FieldType#LIST_OBJECT}</li>
 * </ul>
 * 
 * @author Matthias Müller
 */
@Slf4j
class ListFieldConverter extends ConverterBase {

	private static final int NOT_INDEXED = -1;

	ListFieldConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void setString(FieldWrapper field) {
		if (!FieldType.LIST_OBJECT.equals(field.getType())) {
			super.setString(field);
		}
	}

	@Override
	public void setObject(FieldWrapper field, RequestContainer request) {
		String name = field.getBinding();
		BeanWrapper wrapper = field.getBeanWrapper();
		wrapper.setAutoGrowNestedPaths(true);
		List<String> values = request.getParameterList(name);
		Class<?> propertyType = getType(wrapper, name);
		if (null != values && wrapper.isReadableProperty(name)) {
			if (FieldType.LIST_OBJECT.equals(field.getType())) {
				List<FieldDef> innerFields = new ArrayList<>(field.getFields());
				field.getFields().clear();
				int maxIndex = 0;
				Pattern pattern = Pattern.compile("^" + Pattern.quote(field.getBinding()) + "\\[(\\d+)\\]\\..+$");
				for (String paramName : request.getParameterNames()) {
					Matcher matcher = pattern.matcher(paramName);
					if (matcher.matches()) {
						Integer index = Integer.parseInt(matcher.group(1));
						maxIndex = Math.max(maxIndex, index);
					}
				}
				for (int i = 0; i <= maxIndex; i++) {
					addNestedFields(field, innerFields, i);
				}
			} else if (conversionService.canConvert(String.class, propertyType)) {
				if (isCollection(wrapper, name)) {
					values.forEach(value -> {
						Object result = conversionService.convert(value, propertyType);
						if (null != result) {
							addCollectionValue(wrapper, name, result);
						}
					});
					logSetObject(field, wrapper.getPropertyValue(name));
				} else if (values.size() == 1) {
					Object result = conversionService.convert(values.get(0), propertyType);
					logSetObject(field, result);
					field.setObject(result);
				}
			} else {
				LOGGER.debug("can not convert from {} to {}", String.class, propertyType);
			}
		}

	}

	@Override
	protected Logger getLog() {
		return LOGGER;
	}

	@Override
	public Datafield addField(DatafieldOwner dataFieldOwner, FieldWrapper fieldWrapper) {
		Object object = fieldWrapper.getObject();
		final Datafield datafield = createDataField(fieldWrapper);
		boolean isObjectList = FieldType.LIST_OBJECT.equals(fieldWrapper.getType());
		datafield.setValue(isObjectList ? null : "");
		dataFieldOwner.getFields().add(datafield);
		if (null != object) {
			BeanWrapper beanWrapper = fieldWrapper.getBeanWrapper();
			String binding = fieldWrapper.getBinding();
			TypeDescriptor propertyTypeDescriptor = beanWrapper.getPropertyTypeDescriptor(binding);
			if (propertyTypeDescriptor.isCollection()) {
				Collection<?> collection = (Collection<?>) object;
				Datafield child = null;
				List<FieldDef> indexedFields = new ArrayList<>(fieldWrapper.getFields());
				if (isObjectList) {
					fieldWrapper.getFields().clear();
				}
				Iterator<?> iterator = collection.iterator();
				for (int i = 0; i < collection.size(); i++) {
					Object element = iterator.next();
					if (isObjectList) {
						addNestedFields(fieldWrapper, indexedFields, i);
					} else if (conversionService.canConvert(element.getClass(), String.class)) {
						child = new Datafield();
						child.setValue(conversionService.convert(element, String.class));
					} else {
						child = new Datafield();
						child.setValue(element.toString());
					}
					if (null != child) {
						child.setName(getIndexedField(fieldWrapper.getName(), i));
						datafield.getFields().add(child);
					}
				}
			} else {
				datafield.setValue(conversionService.convert(object, String.class));
			}
			return datafield;
		}
		return null;
	}

	private String getIndexedField(String nameOrBinding, int index) {
		return String.format("%s[%d]", nameOrBinding, index);
	}

	private void addNestedFields(FieldDef parent, List<FieldDef> childDefinitions, int index) {
		for (FieldDef fieldDef : childDefinitions) {
			boolean indexed = index > NOT_INDEXED;
			String binding = indexed ? getIndexedField(parent.getBinding(), index)
					: String.format("%s.%s", parent.getBinding(), fieldDef.getName());
			String name = indexed ? getIndexedField(parent.getName(), index) : fieldDef.getName();

			FieldDef child = copyField(fieldDef, name, binding);
			addNestedFields(child, fieldDef.getFields(), NOT_INDEXED);
			List<FieldDef> children = parent.getFields();
			if (!children.stream().filter(b -> b.getBinding().equals(binding)).findAny().isPresent()) {
				children.add(child);
				LOGGER.debug("adding nested field {} to {}", FieldWrapper.toString(child),
						FieldWrapper.toString(parent));
			}
		}
	}

	private FieldDef copyField(FieldDef fieldDef, String name, String binding) {
		FieldDef copy = new FieldDef();
		copy.setBinding(binding);
		copy.setName(name);
		copy.setType(fieldDef.getType());
		copy.setReadonly(fieldDef.getReadonly());
		copy.setHidden(fieldDef.getHidden());
		copy.setFormat(fieldDef.getFormat());
		copy.setDisplayLength(fieldDef.getDisplayLength());
		Condition condition = fieldDef.getCondition();
		if (null != condition) {
			Condition newCondition = new Condition();
			newCondition.setExpression(condition.getExpression());
			copy.setCondition(newCondition);
		}
		Label label = fieldDef.getLabel();
		if (null != label) {
			Label l = new Label();
			l.setId(label.getId());
			l.setValue(label.getValue());
			l.setParams(label.getParams());
			copy.setLabel(l);
		}
		return copy;
	}

	private boolean isCollection(BeanWrapper wrapper, String property) {
		TypeDescriptor typeDescriptor = wrapper.getPropertyTypeDescriptor(property);
		return Collection.class.isAssignableFrom(typeDescriptor.getType());
	}

	private Class<?> getType(BeanWrapper wrapper, String property) {
		TypeDescriptor typeDescriptor = wrapper.getPropertyTypeDescriptor(property);
		Class<?> type = typeDescriptor.getType();
		if (isCollection(wrapper, property)) {
			return typeDescriptor.getElementTypeDescriptor().getType();
		} else {
			return type;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addCollectionValue(BeanWrapper wrapper, String name, Object result) {
		Collection collection = (Collection) wrapper.getPropertyValue(name);
		if (null == collection) {
			throw new IllegalArgumentException(
					String.format("collection '%s' of object %s is null, can not add value '%s'!", name,
							wrapper.getWrappedInstance(), result));
		}
		collection.add(result);
	}
}