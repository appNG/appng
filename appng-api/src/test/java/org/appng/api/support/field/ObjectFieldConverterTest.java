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
package org.appng.api.support.field;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.FieldWrapper;
import org.appng.api.Person;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.api.support.ResultServiceImpl;
import org.appng.api.support.XmlValidator;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.support.DefaultConversionService;

public class ObjectFieldConverterTest extends AbstractFieldConverterTest {

	Person darth;
	Person luke;
	FieldDef fatherField;

	@Before
	public void setup() throws Exception {
		super.setup(FieldType.OBJECT);

		darth = new Person();
		darth.setFirstname("Darth");
		darth.setName("Vader");

		luke = new Person();
		luke.setFirstname("Luke");
		luke.setName("Skywalker");
		FastDateFormat fdf = FastDateFormat.getInstance("yyyy-MM-dd");
		luke.setBirthDate(fdf.parse("3049-04-14"));

		Person leia = new Person();
		leia.setFirstname("Leia");
		leia.setName("Organa");
		leia.setBirthDate(luke.getBirthDate());

		leia.setFather(darth);
		luke.setFather(darth);

		List<Person> offsprings = new ArrayList<>();
		offsprings.add(luke);
		offsprings.add(leia);
		darth.setOffsprings(offsprings);

		beanWrapper = new BeanWrapperImpl(luke);
		fatherField = new FieldDef();
		fatherField.setType(FieldType.OBJECT);
		fatherField.setName("father");

		FieldDef fatherName = new FieldDef();
		fatherName.setType(FieldType.TEXT);
		fatherName.setName("name");
		fatherField.getFields().add(fatherName);

		FieldDef fatherFirstname = new FieldDef();
		fatherFirstname.setType(FieldType.TEXT);
		fatherFirstname.setName("firstname");
		fatherField.getFields().add(fatherFirstname);

		FieldDef childList = new FieldDef();
		childList.setType(FieldType.LIST_OBJECT);
		childList.setName("offsprings");
		fatherField.getFields().add(childList);

		FieldDef childObject = new FieldDef();
		childObject.setType(FieldType.OBJECT);
		childObject.setBinding("father.offsprings[]");
		childObject.setName("offsprings[]");
		childList.getFields().add(childObject);

		FieldDef childName = new FieldDef();
		childName.setType(FieldType.TEXT);
		childName.setName("name");
		childName.setDisplayLength(new BigInteger("30"));
		childObject.getFields().add(childName);

		FieldDef childFirstName = new FieldDef();
		childFirstName.setType(FieldType.TEXT);
		childFirstName.setName("firstname");
		childFirstName.setHidden("false");
		Condition cond = new Condition();
		cond.setExpression("true");
		childFirstName.setCondition(cond);
		childObject.getFields().add(childFirstName);

		FieldDef childBirthDate = new FieldDef();
		childBirthDate.setType(FieldType.DATE);
		childBirthDate.setFormat("yyyy-MM-dd");
		childBirthDate.setName("birthDate");
		childBirthDate.setReadonly("false");
		childObject.getFields().add(childBirthDate);

		setBindings(null, Arrays.asList(fatherField));
		fieldWrapper = new FieldWrapper(fatherField, beanWrapper);
	}

	private void setBindings(String binding, List<FieldDef> fields) {
		for (FieldDef fieldDef : fields) {
			if (null == fieldDef.getBinding()) {
				if (binding != null) {
					fieldDef.setBinding(binding + "." + fieldDef.getName());
				} else {
					fieldDef.setBinding(fieldDef.getName());
				}
			}
			setBindings(fieldDef.getBinding(), fieldDef.getFields());
		}
	}

	@Test
	public void testAddFieldForObjectList() throws Exception {

	}

	@Override
	@Test
	public void testAddField() throws Exception {

		Map<String, Object> variables = new HashMap<>();
		ResultServiceImpl resultService = new ResultServiceImpl(new ExpressionEvaluator(variables));
		resultService.setConversionService(new DefaultConversionService());
		resultService.setEnvironment(environment);
		resultService.afterPropertiesSet();
		MetaData metaData = new MetaData();
		metaData.setBindClass(Person.class.getName());
		metaData.getFields().add(fatherField);
		XmlValidator.validate(metaData, "-metadata");

		Result result = resultService.getResult(new FieldProcessorImpl("test", metaData), luke);

		XmlValidator.validate(metaData, "-metadata-processed");
		XmlValidator.validate(result, "-result");

	}

	@Override
	public Container<?> getContainer() {
		return new Container<Person>() {
		};
	}

	@Override
	@Test
	public void testSetObject() throws Exception {
		darth.getOffsprings().clear();
		Set<String> parameterNames = new HashSet<>();

		parameterNames.add(addParameter("father.name", "Vader"));
		parameterNames.add(addParameter("father.firstname", "Darth"));

		parameterNames.add(addParameter("father.offsprings[0].name", "Skywalker"));
		parameterNames.add(addParameter("father.offsprings[0].firstname", "Luke"));

		parameterNames.add(addParameter("father.offsprings[1].name", "Organa"));
		parameterNames.add(addParameter("father.offsprings[1].firstname", "Leia"));

		Mockito.when(request.getParameterNames()).thenReturn(parameterNames);

		fieldConverter.setObject(fieldWrapper, request);

		Person bindVader = (Person) fieldWrapper.getObject();

		Assert.assertEquals("Darth", bindVader.getFirstname());
		Assert.assertEquals("Vader", bindVader.getName());

		List<Person> offsprings = bindVader.getOffsprings();
		Assert.assertEquals(2, offsprings.size());

		Assert.assertEquals("Luke", offsprings.get(0).getFirstname());
		Assert.assertEquals("Skywalker", offsprings.get(0).getName());

		Assert.assertEquals("Leia", offsprings.get(1).getFirstname());
		Assert.assertEquals("Organa", offsprings.get(1).getName());
	}

	private String addParameter(String name, String value) {
		Mockito.when(request.getParameter(name)).thenReturn(value);
		return name;

	}

	@Override
	public void testSetObjectEmptyValue() throws Exception {

	}

	@Override
	public void testSetObjectInvalidValue() throws Exception {

	}

	@Override
	public void testSetObjectNull() throws Exception {

	}

	@Override
	@Test
	public void testSetString() throws Exception {
		fieldConverter.setString(fieldWrapper);
		Assert.assertNull(fieldWrapper.getStringValue());

	}

	@Override
	public void testSetStringNullObject() throws Exception {

	}

	@Override
	public void testSetStringInvalidType() throws Exception {

	}

}
