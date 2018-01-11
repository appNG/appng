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
 * 
 * A {@code FieldWrapper} extends a {@link FieldDef} and adds the ability to read and set the field's
 * string-representation ( {@link #getStringValue()}/{@link #setStringValue(String)}) and also the object-representation
 * ( {@link #getObject()}/ {@link #setObject(Object)}). This is achieved by using a {@link BeanWrapper}.
 * 
 * @author Matthias Müller
 * 
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
	 *            the {@link FieldDef}
	 * @param beanWrapper
	 *            the {@link BeanWrapper}
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
	 *            the property
	 */
	public void setObject(Object object) {
		if (beanWrapper.isReadableProperty(getBinding())) {
			beanWrapper.setPropertyValue(getBinding(), object);
		} else {
			throw new IllegalArgumentException("can not write property '" + getBinding() + "' to "
					+ beanWrapper.getWrappedInstance().toString());
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
	 *            the {@link String}-value to set
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

	public int hashCode() {
		return inner.hashCode();
	}

	public boolean equals(Object obj) {
		return inner.equals(obj);
	}

	public Sort getSort() {
		return inner.getSort();
	}

	public void setSort(Sort value) {
		inner.setSort(value);
	}

	public Label getLabel() {
		return inner.getLabel();
	}

	public void setLabel(Label value) {
		inner.setLabel(value);
	}

	public List<Icon> getIcons() {
		return inner.getIcons();
	}

	public Condition getCondition() {
		return inner.getCondition();
	}

	public void setCondition(Condition value) {
		inner.setCondition(value);
	}

	public List<FieldPermissions> getPermissions() {
		return inner.getPermissions();
	}

	public Validation getValidation() {
		return inner.getValidation();
	}

	public void setValidation(Validation value) {
		inner.setValidation(value);
	}

	public Messages getMessages() {
		return inner.getMessages();
	}

	public void setMessages(Messages value) {
		inner.setMessages(value);
	}

	public List<FieldDef> getFields() {
		return inner.getFields();
	}

	public String getName() {
		return inner.getName();
	}

	public void setName(String value) {
		inner.setName(value);
	}

	public FieldType getType() {
		return inner.getType();
	}

	public void setType(FieldType value) {
		inner.setType(value);
	}

	public BigInteger getDisplayLength() {
		return inner.getDisplayLength();
	}

	public void setDisplayLength(BigInteger value) {
		inner.setDisplayLength(value);
	}

	public String getFormat() {
		return inner.getFormat();
	}

	public void setFormat(String value) {
		inner.setFormat(value);
	}

	public String getReadonly() {
		return inner.getReadonly();
	}

	public void setReadonly(String value) {
		inner.setReadonly(value);
	}

	public String getHidden() {
		return inner.getHidden();
	}

	public void setHidden(String value) {
		inner.setHidden(value);
	}

	public String getBinding() {
		return inner.getBinding();
	}

	public void setBinding(String value) {
		inner.setBinding(value);
	}

	public String toString() {
		return toString(this);
	}

	public static String toString(FieldDef fieldDef) {
		return fieldDef.getBinding() + " (type: " + fieldDef.getType() + ", format: " + fieldDef.getFormat()
				+ ", readonly:" + fieldDef.getReadonly()
				+ ", hidden: " + fieldDef.getHidden() + ")";
	}

	public Linkpanel getLinkpanel() {
		return linkpanel;
	}

	public void backupFields() {
		this.originalFields = new ArrayList<FieldDef>(getFields());
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
