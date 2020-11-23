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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.appng.xml.platform.Condition;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldPermissions;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Icon;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.Messages;
import org.appng.xml.platform.Sort;
import org.appng.xml.platform.Validation;
import org.springframework.beans.BeanWrapper;
import org.springframework.core.convert.TypeDescriptor;

/**
 * A {@code FieldWrapper} extends a {@link FieldDef} and adds the ability to read and set the field's
 * string-representation ( {@link #getStringValue()}/{@link #setStringValue(String)}) and also the object-representation
 * ( {@link #getObject()}/ {@link #setObject(Object)}). This is achieved by using a {@link BeanWrapper}.
 * 
 * @author Matthias Müller
 */
public class FieldWrapper extends FieldDef {

	private String stringValue;
	private FieldDef inner;
	private BeanWrapper beanWrapper;
	private Linkpanel linkpanel;
	private List<FieldDef> originalFields;

	/**
	 * Creates a new {@code FieldWrapper} for the given {@link FieldDef}, using the given {@link BeanWrapper}.
	 * 
	 * @param fieldDef
	 *                    the {@link FieldDef}
	 * @param beanWrapper
	 *                    the {@link BeanWrapper}
	 */
	public FieldWrapper(FieldDef fieldDef, BeanWrapper beanWrapper) {
		this.inner = fieldDef;
		this.beanWrapper = beanWrapper;
	}

	/**
	 * Returns the property that this {@code FieldWrapper} handles.
	 * 
	 * @return the property
	 */
	public Object getObject() {
		if (beanWrapper.isReadableProperty(getBinding())) {
			return beanWrapper.getPropertyValue(getBinding());
		}
		return null;
	}

	/**
	 * Sets the property that this {@code FieldWrapper} handles.
	 * 
	 * @param object
	 *               the property
	 */
	public void setObject(Object object) {
		if (beanWrapper.isReadableProperty(getBinding())) {
			beanWrapper.setPropertyValue(getBinding(), object);
		} else {
			throw new IllegalArgumentException(
					"can not write property '" + getBinding() + "' to " + beanWrapper.getWrappedInstance().toString());
		}
	}

	/**
	 * Returns the {@link String}-value of the property that this {@code FieldWrapper} handles.
	 * 
	 * @return the {@link String}-value
	 */
	public String getStringValue() {
		return stringValue;
	}

	/**
	 * Sets the {@link String}-value of the property that this {@code FieldWrapper} handles.
	 * 
	 * @param stringValue
	 *                    the {@link String}-value to set
	 */
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	/**
	 * Returns the {@link BeanWrapper} used by this {@code FieldWrapper} to access the property.
	 * 
	 * @return the {@link BeanWrapper}
	 */
	public BeanWrapper getBeanWrapper() {
		return beanWrapper;
	}

	/**
	 * Returns the target-type for the property that this {@code FieldWrapper} handles.
	 * 
	 * @return the target-type
	 */
	public Class<?> getTargetClass() {
		TypeDescriptor propertyTypeDescriptor = beanWrapper.getPropertyTypeDescriptor(getBinding());
		if (null == propertyTypeDescriptor) {
			return null;
		}
		return propertyTypeDescriptor.getType();
	}

	@Override
	public int hashCode() {
		return inner.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return inner.equals(obj);
	}

	@Override
	public Sort getSort() {
		return inner.getSort();
	}

	@Override
	public void setSort(Sort value) {
		inner.setSort(value);
	}

	@Override
	public Label getLabel() {
		return inner.getLabel();
	}

	@Override
	public void setLabel(Label value) {
		inner.setLabel(value);
	}

	@Override
	public List<Icon> getIcons() {
		return inner.getIcons();
	}

	@Override
	public Condition getCondition() {
		return inner.getCondition();
	}

	@Override
	public void setCondition(Condition value) {
		inner.setCondition(value);
	}

	@Override
	public List<FieldPermissions> getPermissions() {
		return inner.getPermissions();
	}

	@Override
	public Validation getValidation() {
		return inner.getValidation();
	}

	@Override
	public void setValidation(Validation value) {
		inner.setValidation(value);
	}

	@Override
	public Messages getMessages() {
		return inner.getMessages();
	}

	@Override
	public void setMessages(Messages value) {
		inner.setMessages(value);
	}

	@Override
	public List<FieldDef> getFields() {
		return inner.getFields();
	}

	@Override
	public String getName() {
		return inner.getName();
	}

	@Override
	public void setName(String value) {
		inner.setName(value);
	}

	@Override
	public FieldType getType() {
		return inner.getType();
	}

	@Override
	public void setType(FieldType value) {
		inner.setType(value);
	}

	@Override
	public BigInteger getDisplayLength() {
		return inner.getDisplayLength();
	}

	@Override
	public void setDisplayLength(BigInteger value) {
		inner.setDisplayLength(value);
	}

	@Override
	public String getFormat() {
		return inner.getFormat();
	}

	@Override
	public void setFormat(String value) {
		inner.setFormat(value);
	}

	@Override
	public String getReadonly() {
		return inner.getReadonly();
	}

	@Override
	public void setReadonly(String value) {
		inner.setReadonly(value);
	}

	@Override
	public String getHidden() {
		return inner.getHidden();
	}

	@Override
	public void setHidden(String value) {
		inner.setHidden(value);
	}

	@Override
	public String getBinding() {
		return inner.getBinding();
	}

	@Override
	public void setBinding(String value) {
		inner.setBinding(value);
	}

	@Override
	public String toString() {
		return toString(this);
	}

	public static String toString(FieldDef fieldDef) {
		String condition = null == fieldDef.getCondition() ? ""
				: ", condition: " + fieldDef.getCondition().getExpression();
		return fieldDef.getBinding() + " (type: " + fieldDef.getType() + ", format: " + fieldDef.getFormat()
				+ ", readonly:" + fieldDef.getReadonly() + ", hidden: " + fieldDef.getHidden() + condition + ")";
	}

	public Linkpanel getLinkpanel() {
		return linkpanel;
	}

	public void backupFields() {
		this.originalFields = new ArrayList<>(getFields());
	}

	public void restoreFields() {
		if (null != originalFields) {
			getFields().clear();
			getFields().addAll(originalFields);
		}
	}

	public void setLinkpanel(Linkpanel linkpanel) {
		this.linkpanel = linkpanel;
	}
}
