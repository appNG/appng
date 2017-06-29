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
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * 
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
 * 
 */
class ListFieldConverter extends ConverterBase {

	protected static final Logger LOG = LoggerFactory.getLogger(ListFieldConverter.class);

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
		if (null != values) {
			if (wrapper.isReadableProperty(name))
				if (FieldType.LIST_OBJECT.equals(field.getType())) {
					List<FieldDef> innerFields = new ArrayList<FieldDef>(field.getFields());
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
					LOG.debug("can not convert from {} to {}", String.class, propertyType);
				}
		}
	}

	@Override
	protected Logger getLog() {
		return LOG;
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
				List<FieldDef> indexedFields = new ArrayList<FieldDef>(fieldWrapper.getFields());
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

	private String getIndexedField(String s, int i) {
		String format = String.format("%s[%d]", s, i);
		return format;
	}

	private void addNestedFields(FieldDef parentField, List<FieldDef> indexedFields, int index) {
		for (FieldDef fieldDef : indexedFields) {
			FieldDef indexedField = copyField(fieldDef);
			if (index > -1) {
				indexedField.setBinding(getIndexedField(parentField.getBinding(), index));
				indexedField.setName(getIndexedField(parentField.getName(), index));
			} else {
				indexedField.setBinding(parentField.getBinding() + "." + fieldDef.getName());
			}
			if (FieldType.OBJECT.equals(indexedField.getType())) {
				addNestedFields(indexedField, fieldDef.getFields(), -1);
			}
			parentField.getFields().add(indexedField);
			LOG.debug("adding nested field {} to {}", FieldWrapper.toString(indexedField),
					FieldWrapper.toString(parentField));
		}
	}

	private FieldDef copyField(FieldDef fieldDef) {
		FieldDef copy = new FieldDef();
		copy.setBinding(fieldDef.getBinding());
		copy.setName(fieldDef.getName());
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
			throw new IllegalArgumentException("collection '" + name + "' of object " + wrapper.getWrappedInstance()
					+ " is null, can not add value '" + result + "'!");
		}
		collection.add(result);
	}
}