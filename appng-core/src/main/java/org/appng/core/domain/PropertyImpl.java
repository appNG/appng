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
package org.appng.core.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.appng.api.ValidationMessages;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;

/**
 * A persistent {@link Property} JPA-{@link Entity}.
 * 
 * @author Matthias Müller
 */
@Entity
@Table(name = "property")
@EntityListeners(PlatformEventListener.class)
public class PropertyImpl extends SimpleProperty implements Property, Auditable<String>, Comparable<Property> {

	public PropertyImpl() {
		setMandatory(false);
	}

	public PropertyImpl(String name, String value) {
		this(name, null, value);

	}

	public PropertyImpl(String name, String value, String defaultValue) {
		setName(name);
		setActualString(value);
		setDefaultString(defaultValue);
	}

	@Override
	@Id
	@NotNull(message = ValidationMessages.VALIDATION_NOT_NULL)
	@Size(min = 3, max = 255, message = ValidationMessages.VALIDATION_STRING_MIN_MAX)
	public String getName() {
		return super.getName();
	}

	@Override
	public boolean isMandatory() {
		return super.isMandatory();
	}

	@Override
	@Transient
	public String getId() {
		return getName();
	}

	@Override
	@Version
	public Date getVersion() {
		return super.getVersion();
	}

	@Override
	@Column(name = "value")
	public String getActualString() {
		return super.getActualString();
	}

	@Override
	@Column(name = "defaultValue")
	public String getDefaultString() {
		return super.getDefaultString();
	}

	@Override
	@Column(name = "description", length = 1024)
	public String getDescription() {
		return super.getDescription();
	}

	@Override
	@Column(name = "blobValue")
	@Lob
	public byte[] getBlob() {
		return super.getBlob();
	}

	@Override
	@Column(name = "clobValue")
	@Lob
	public String getClob() {
		return super.getClob();
	}

	@Override
	@Column(name = "prop_type")
	@Enumerated(EnumType.STRING)
	public Type getType() {
		return super.getType();
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCode(this);
	}

	@Override
	public boolean equals(Object o) {
		return ObjectUtils.equals(this, o);
	}

}
