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
package org.appng.api.model;

import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link Property}-implementation
 * 
 * @author Matthias Müller
 * 
 */
public class SimpleProperty implements Property, Identifiable<String>, Comparable<Property> {

	private static final Logger log = LoggerFactory.getLogger(SimpleProperty.class);

	private String value;
	private String defaultValue;
	private String description;
	private String name;
	private boolean mandatory;
	private Date version;
	private byte[] blob;
	private String clob;

	public SimpleProperty() {
		this.mandatory = false;
	}

	public SimpleProperty(String name, String value) {
		this();
		this.name = name;
		this.value = value;
		this.defaultValue = null;
	}

	public SimpleProperty(String name, String value, String defaultValue) {
		this();
		this.name = name;
		this.value = value;
		this.defaultValue = defaultValue;
	}

	@NotNull
	@Size(min = 3, max = 255)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public String getId() {
		return getName();
	}

	public void setId(String id) {
		setName(id);
	}

	public Date getVersion() {
		return version;
	}

	public void setVersion(Date version) {
		this.version = version;
	}

	public String getActualString() {
		return value;
	}

	public void setActualString(String value) {
		this.value = value;
	}

	public String getDefaultString() {
		return defaultValue;
	}

	public void setDefaultString(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public byte[] getBlob() {
		return blob;
	}

	public void setBlob(byte[] blob) {
		this.blob = ArrayUtils.clone(blob);
	}

	public String getClob() {
		return clob;
	}

	public void setClob(String clob) {
		this.clob = clob;
	}

	public String getString() {
		if (!StringUtils.isEmpty(value)) {
			return value;
		} else {
			return defaultValue;
		}
	}

	public void setString(String value) {
		setActualString(value);
	}

	public Integer getInteger() {
		if (null != getString()) {
			try {
				return Integer.parseInt(getString());
			} catch (NumberFormatException e) {
				log.warn("could not convert property '" + getName() + "' to an Integer");
			}
		}
		return null;
	}

	public Float getFloat() {
		if (null != getString()) {
			try {
				return Float.parseFloat(getString());
			} catch (NumberFormatException e) {
				log.warn("could not convert property '" + getName() + "' to a Float");
			}
		}
		return null;
	}

	public Double getDouble() {
		if (null != getString()) {
			try {
				return Double.parseDouble(getString());
			} catch (NumberFormatException e) {
				log.warn("could not convert property '" + getName() + "' to a Double");
			}
		}
		return null;
	}

	public Boolean getBoolean() {
		if (null != getString()) {
			try {
				return "true".equalsIgnoreCase(getString()) || "1".equals(getString());
			} catch (Exception e) {
				log.warn("could not convert property '" + getName() + "' to a Boolean");
			}
		}
		return null;
	}

	public Boolean getChangedValue() {
		return StringUtils.isEmpty(value);
	}

	public String toString() {
		String value = getString();
		if (null == value) {
			value = getClob();
		}
		return getName() + ": " + (value == null ? "(blob-content)" : value);
	}

	public int compareTo(Property other) {
		if (other == null) {
			return 1;
		}
		return getName().compareToIgnoreCase(other.getName());
	}
}
