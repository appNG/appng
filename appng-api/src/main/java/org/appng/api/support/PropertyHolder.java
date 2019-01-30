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
package org.appng.api.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appng.api.model.Properties;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;

/**
 * 
 * Default {@link Properties} implementation, internally holding a {@link Map} of {@link Property}-objects.
 * 
 * @author Matthias Müller
 * 
 */
public class PropertyHolder implements Properties {

	private Map<String, Property> propMap = new HashMap<String, Property>();
	private String prefix;
	private boolean isFinal;

	/**
	 * Creates a new {@link PropertyHolder}
	 * 
	 * @param prefix
	 *            the prefix to use
	 * @param properties
	 *            the {@link Property}-instances to hold
	 */
	public PropertyHolder(String prefix, Iterable<? extends Property> properties) {
		this.prefix = prefix;
		for (Property p : properties) {
			propMap.put(p.getName(), p);
		}
	}

	public PropertyHolder() {
		this("", Collections.<Property> emptyList());
	}

	/**
	 * Sets this {@link PropertyHolder} to final, which means no more properties can be added using
	 * {@link #addProperty(String, Object, String)}.
	 * 
	 * @return this {@code PropertyHolder}
	 */
	public PropertyHolder setFinal() {
		this.isFinal = true;
		return this;
	}

	public Set<String> getPropertyNames() {
		return propMap.keySet();
	}

	public boolean propertyExists(String name) {
		if (name.startsWith(prefix)) {
			return propMap.containsKey(name);
		}
		return propMap.containsKey(prefix + name);
	}

	public Property getProperty(String name) {
		if (!name.startsWith(prefix)) {
			return propMap.get(prefix + name);
		}
		return propMap.get(name);
	}

	public String getString(String name, String defaultValue) {
		Property property = getProperty(name);
		if (null != property) {
			return property.getString();
		}
		return defaultValue;
	}

	/**
	 * @see PropertyHolder#addProperty(String, Object, String, boolean)
	 */
	public final Property addProperty(String name, Object defaultValue, String description) {
		return addProperty(name, defaultValue, description, false);
	}

	/**
	 * As long as {@link #setFinal()} has not be called, this method can be used to add new properties
	 * 
	 * @param name
	 *            the name of the property, <b>without prefix</b>
	 * @param defaultValue
	 *            the default value for the property to add, must not be {@code null}
	 * @param description
	 *            the description for the property
	 * @param asClob
	 *            if the property should be created as clob
	 * @throws IllegalArgumentException
	 *             if defaultValue is {@code null} or if {@link #setFinal()} has been called before.
	 */
	public final Property addProperty(String name, Object defaultValue, String description, boolean asClob) {
		if (!isFinal) {
			String fullName = prefix + name;
			if (null != defaultValue) {
				SimpleProperty prop = null;
				if (propMap.containsKey(fullName)) {
					prop = (SimpleProperty) propMap.get(fullName);
				} else {
					prop = getNewProperty(name);
					prop.setName(fullName);
					if (asClob) {
						prop.setClob(defaultValue.toString());
					}
					propMap.put(fullName, prop);
				}
				if (!asClob) {
					prop.setDefaultString(defaultValue.toString());
				}
				prop.setDescription(description);
			} else {
				throw new IllegalArgumentException("defaultValue can not be null!");
			}
			return propMap.get(fullName);
		} else {
			throw new IllegalArgumentException("this PropertyHolder is final, no more properties can be added!");
		}
	}

	protected SimpleProperty getNewProperty(String name) {
		SimpleProperty property = new SimpleProperty();
		property.setName(name);
		return property;
	}

	public Boolean getBoolean(String name) {
		return getBoolean(name, null);
	}

	public Boolean getBoolean(String name, Boolean defaultValue) {
		Property property = getProperty(name);
		if (null != property) {
			return property.getBoolean();
		}
		return defaultValue;
	}

	public Integer getInteger(String name, Integer defaultValue) {
		Property property = getProperty(name);
		if (null != property) {
			return property.getInteger();
		}
		return defaultValue;
	}

	public Float getFloat(String name, Float defaultValue) {
		Property property = getProperty(name);
		if (null != property) {
			return property.getFloat();
		}
		return defaultValue;
	}

	public Double getDouble(String name, Double defaultValue) {
		Property property = getProperty(name);
		if (null != property) {
			return property.getDouble();
		}
		return defaultValue;
	}

	public byte[] getBlob(String name) {
		Property property = getProperty(name);
		if (null != property) {
			return property.getBlob();
		}
		return null;
	}

	public String getClob(String name, String defaultValue) {
		Property property = getProperty(name);
		if (null != property) {
			return property.getClob();
		}
		return defaultValue;
	}

	public String getString(String name) {
		return getString(name, null);
	}

	public Integer getInteger(String name) {
		return getInteger(name, null);
	}

	public Float getFloat(String name) {
		return getFloat(name, null);
	}

	public Double getDouble(String name) {
		return getDouble(name, null);
	}

	public String getClob(String name) {
		return getClob(name, null);
	}

	@Override
	public String toString() {
		return propMap.values().toString();
	}

	public List<String> getList(String name, String defaultValue, String delimiter) {
		List<String> result = new ArrayList<>();
		String string = getString(name, defaultValue);
		if (null != string && string.length() > 0) {
			String[] splitted = string.split(delimiter);
			for (String value : splitted) {
				result.add(value.trim());
			}
		}
		return Collections.unmodifiableList(result);
	}

	public List<String> getList(String name, String delimiter) {
		return getList(name, "", delimiter);
	}

	public java.util.Properties getPlainProperties() {
		java.util.Properties props = new java.util.Properties();
		Set<String> propertyNames = getPropertyNames();
		for (String name : propertyNames) {
			Property property = getProperty(name);
			if (null != property) {
				String value = property.getString();
				if (null == value) {
					value = property.getClob();
				}
				if (null != value) {
					String shortName = name.substring(name.lastIndexOf(".") + 1);
					props.put(shortName, value);
				}
			}
		}
		return props;
	}

	public java.util.Properties getProperties(String name) {
		String clob = getClob(name);
		if (null != clob) {
			java.util.Properties properties = new java.util.Properties();
			try {
				properties.load(new ByteArrayInputStream(clob.getBytes()));
			} catch (IOException e) {
				throw new IllegalArgumentException("failed converting property '" + name + "' to java.util.Properties",
						e);
			}
			return properties;
		}
		return null;
	}

	@Override
	public String getDescriptionFor(String name) {
		Property property = getProperty(name);
		return null == property ? null : property.getDescription();
	}

}
