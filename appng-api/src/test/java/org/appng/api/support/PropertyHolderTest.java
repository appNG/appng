/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.appng.api.model.Property;
import org.appng.api.model.Property.Type;
import org.appng.api.model.SimpleProperty;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PropertyHolderTest {

	private static final String PREFIX = "foo.bar.";
	private PropertyHolder propertyHolder;
	private Properties plainProperties;

	@Test
	public void testInteger() {
		Assert.assertEquals(Integer.valueOf(1), propertyHolder.getInteger("integer"));
		Assert.assertEquals(Integer.valueOf(1), propertyHolder.getObject("integer"));
		Assert.assertEquals("the property integer", propertyHolder.getDescriptionFor("integer"));
		Assert.assertEquals(Integer.valueOf(2), propertyHolder.getInteger("bla", 2));
	}

	@Test
	public void testString() {
		Assert.assertEquals("string", propertyHolder.getString("string"));
		Assert.assertEquals("string", propertyHolder.getObject("string"));
		Assert.assertEquals("the property string", propertyHolder.getDescriptionFor("string"));
		Assert.assertEquals("bla", propertyHolder.getString("bla", "bla"));
	}

	@Test
	public void testCustomString() {
		Assert.assertEquals("custom", propertyHolder.getString("customString"));
		Assert.assertEquals("string", propertyHolder.getString("emptyCustomString"));
	}

	@Test
	public void testBoolean() {
		Assert.assertEquals(true, propertyHolder.getBoolean("boolean"));
		Assert.assertEquals(Boolean.TRUE, propertyHolder.getObject("boolean"));
		Assert.assertEquals(false, propertyHolder.getBoolean("bla", false));
	}

	@Test
	public void testFloat() {
		Assert.assertEquals(Float.valueOf(4.5f), propertyHolder.getFloat("float"));
		Assert.assertEquals(Float.valueOf(1.2f), propertyHolder.getFloat("bla", 1.2f));
		Assert.assertEquals(Double.valueOf(4.5f), propertyHolder.getObject("float"));
	}

	@Test
	public void testDouble() {
		Assert.assertEquals(Double.valueOf(7.9d), propertyHolder.getDouble("double"));
		Assert.assertEquals(Double.valueOf(7.9d), propertyHolder.getObject("double"));
		Assert.assertEquals(Double.valueOf(1.2d), propertyHolder.getDouble("bla", 1.2d));
	}

	@Test
	public void testList() {
		Assert.assertEquals(Arrays.asList("1", "2"), propertyHolder.getList("list", ","));
		Assert.assertEquals(Arrays.asList("3", "4"), propertyHolder.getList("bla", "3,4", ","));
	}

	@Test
	public void testProperties() {
		Properties props = new Properties();
		props.put("a", "1");
		props.put("b", "2");
		Assert.assertEquals(props, propertyHolder.getProperties("properties"));
		Assert.assertEquals(null, propertyHolder.getProperties("bla"));
	}

	@Test
	public void testPlainProperties() {
		Properties plainProperties = propertyHolder.getPlainProperties();
		Assert.assertEquals(plainProperties, propertyHolder.getPlainProperties());
	}

	@Before
	public void setup() {
		this.plainProperties = new Properties();
		plainProperties.put("integer",1);
		plainProperties.put("string", "string");
		plainProperties.put("emptyCustomString", "string");
		plainProperties.put("customString", "string");
		plainProperties.put("float", 4.5f);
		plainProperties.put("double", 7.9d);
		plainProperties.put("boolean", true);
		plainProperties.put("list", "1,2");
		plainProperties.put("properties", "a = 1\r\nb=2");

		List<Property> properties = new ArrayList<>();
		addProperty(properties, "integer", false, null);
		addProperty(properties, "string", false, null);
		addProperty(properties, "emptyCustomString", false, "");
		addProperty(properties, "customString", false, "custom");
		addProperty(properties, "float", false, null);
		addProperty(properties, "double", false, null);
		addProperty(properties, "boolean", false, null);
		addProperty(properties, "list", false, null);
		addProperty(properties, "properties", true, null);
		propertyHolder = new PropertyHolder(PREFIX, properties);
	}

	private void addProperty(List<Property> properties, String name, boolean clob, String customValue) {
		Object value = plainProperties.get(name);
		SimpleProperty prop = new SimpleProperty();
		prop.setName(PREFIX + name);
		if (clob) {
			prop.setClob(value.toString());
			prop.setType(Type.MULTILINE);
		} else {
			prop.setDefaultString(value.toString());
			prop.setString(customValue);
			prop.setType(Type.forObject(value));
		}
		prop.setDescription("the property " + name);
		properties.add(prop);
	}
}
