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
package org.appng.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.appng.xml.platform.Condition;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Icon;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Sort;
import org.appng.xml.platform.SortOrder;

public class MetaDataProvider {

	public static final String YYYY_MM_DD = "yyyy.MM.dd";
	public static final DateFormat SDF = new SimpleDateFormat(YYYY_MM_DD);

	public static MetaData getMetaData() {
		MetaData metaData = new MetaData();
		metaData.setBindClass(Person.class.getName());
		FieldDef nameField = getField("name", FieldType.TEXT);
		metaData.getFields().add(nameField);
		Sort sort = new Sort();
		sort.setOrder(SortOrder.DESC);
		sort.setPrio(1);
		nameField.setSort(sort);

		FieldDef firstnameField = getField("firstname", FieldType.TEXT);
		Sort sort2 = new Sort();
		sort2.setOrder(SortOrder.ASC);
		sort2.setPrio(0);
		firstnameField.setSort(sort2);

		metaData.getFields().add(firstnameField);
		FieldDef field = getField("birthDate", FieldType.DATE);
		field.setFormat(YYYY_MM_DD);
		metaData.getFields().add(field);
		metaData.getFields().add(getField("size", FieldType.DECIMAL));
		FieldDef savings = getField("savings", FieldType.DECIMAL);
		savings.setFormat("###,###.###");
		metaData.getFields().add(savings);
		FieldDef age = getField("age", FieldType.LONG);
		Condition condition = new Condition();
		condition.setExpression("${1 eq 2}");
		age.setCondition(condition);
		metaData.getFields().add(age);
		FieldDef offsprings = getField("offsprings", FieldType.LIST_SELECT);
		Condition c2 = new Condition();
		c2.setExpression("${not empty current.offsprings}");
		offsprings.setCondition(c2);

		metaData.getFields().add(offsprings);
		metaData.getFields().add(getField("integerList", FieldType.LIST_CHECKBOX));
		metaData.getFields().add(getField("picture", FieldType.FILE));
		metaData.getFields().add(getField("morePictures", FieldType.FILE_MULTIPLE));
		metaData.getFields().add(getField("strings", FieldType.LIST_TEXT));
		metaData.getFields().add(getField("thepanel", FieldType.LINKPANEL));

		FieldDef coordField = getField("coordinate", FieldType.COORDINATE);
		metaData.getFields().add(coordField);
		FieldDef latitude = getField("coordinate.latitude", FieldType.DECIMAL);
		latitude.setName("latitude");
		coordField.getFields().add(latitude);
		FieldDef longitude = getField("coordinate.longitude", FieldType.DECIMAL);
		longitude.setName("longitude");
		coordField.getFields().add(longitude);

		FieldDef imageField = getField("images", FieldType.IMAGE);
		Icon image1 = new Icon();
		image1.setContent("/foo/bar1.jpg");
		imageField.getIcons().add(image1);
		Icon image2 = new Icon();
		image2.setContent("/foo/bar2.jpg");
		image2.setCondition("${true}");
		imageField.getIcons().add(image2);
		Icon image3 = new Icon();
		image3.setContent("/foo/bar3.jpg");
		image3.setCondition("${1 eq 2}");
		imageField.getIcons().add(image3);
		metaData.getFields().add(imageField);

		FieldDef fatherField = getField("father", null);
		FieldDef fatherName = getField("name", FieldType.TEXT);
		fatherName.setBinding("father.name");
		fatherField.getFields().add(fatherName);
		FieldDef fatherFirstname = getField("firstname", FieldType.TEXT);
		fatherFirstname.setBinding("father.firstname");
		fatherField.getFields().add(fatherFirstname);
		metaData.getFields().add(fatherField);
		return metaData;
	}

	public static FieldDef getField(String name, FieldType type) {
		return getField(name, type, null);
	}

	public static FieldDef getField(String name, FieldType type, String condition) {
		FieldDef field = new FieldDef();
		field.setName(name);
		field.setBinding(name);
		field.setType(type);
		if (null != condition) {
			Condition c = new Condition();
			c.setExpression(condition);
			field.setCondition(c);
		}
		return field;
	}

}
