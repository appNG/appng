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
package org.appng.api.support;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.MetaDataProvider;
import org.appng.api.Person;
import org.appng.api.Request;
import org.appng.el.ExpressionEvaluator;
import org.appng.tools.locator.Coordinate;
import org.appng.xml.platform.Condition;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Link;
import org.appng.xml.platform.Linkmode;
import org.appng.xml.platform.Linkpanel;
import org.appng.xml.platform.MetaData;
import org.appng.xml.platform.PanelLocation;
import org.appng.xml.platform.Result;
import org.appng.xml.platform.Resultset;
import org.appng.xml.platform.SortOrder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

public class ResultServiceTest {

	private ResultServiceImpl rss;

	@Mock
	private Request request;

	@Mock
	private Environment env;

	@Before
	final public void setUp() throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		MockitoAnnotations.initMocks(this);

		ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
		conversionServiceFactoryBean.afterPropertiesSet();
		ConversionService conversionService = conversionServiceFactoryBean.getObject();

		Mockito.when(request.getEnvironment()).thenReturn(env);
		ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(new HashMap<>());
		Mockito.when(request.getExpressionEvaluator()).thenReturn(expressionEvaluator);
		Mockito.when(env.getTimeZone()).thenReturn(TimeZone.getDefault());
		Mockito.when(env.getLocale()).thenReturn(Locale.getDefault());
		rss = new ResultServiceImpl(expressionEvaluator);
		rss.setConversionService(conversionService);
		rss.setEnvironment(env);
		rss.setMessageSource(new StaticMessageSource());
		rss.afterPropertiesSet();
	}

	public FieldProcessor getFieldProcessor() {
		return new FieldProcessorImpl("action", MetaDataProvider.getMetaData());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTimeZone() throws ParseException {
		String pattern = MetaDataProvider.YYYY_MM_DD + " HH:mm:ss";
		MetaData metaData = new MetaData();
		metaData.setBindClass(Person.class.getName());
		metaData.getFields().add(MetaDataProvider.getField("name", FieldType.TEXT));
		FieldDef birthDate = MetaDataProvider.getField("birthDate", FieldType.DATE);
		birthDate.setFormat(pattern);
		metaData.getFields().add(birthDate);
		FieldProcessorImpl fp = new FieldProcessorImpl("", metaData);
		Person p = new Person();
		p.setName("John");
		p.setBirthDate(new SimpleDateFormat(pattern).parse("2012.12.12 13:14:15"));
		Mockito.when(request.getExpressionEvaluator())
				.thenReturn(new ExpressionEvaluator(new HashMap<>()));
		Mockito.when(env.getTimeZone()).thenReturn(TimeZone.getTimeZone("GMT+3"));
		Result result = rss.getResult(fp, p);
		XmlValidator.validate(result);
	}

	@Test
	public void testDefaultSort() {
		FieldProcessorImpl fp = new FieldProcessorImpl("action", MetaDataProvider.getMetaData());
		fp.setPageable(PageRequest.of(1, 50));
		fp.getField("firstname").getSort().setIgnoreCase(true);
		Sort expectedSort = Sort.by(new Order(Direction.ASC, "firstname").ignoreCase(),
				new Order(Direction.DESC, "name"));
		Pageable pageable = fp.getPageable();
		Assert.assertEquals(PageRequest.of(1, 50, expectedSort), pageable);
		org.appng.xml.platform.Sort nameSort = fp.getField("name").getSort();
		Assert.assertNull(nameSort.getPrio());
		Assert.assertNull(nameSort.getOrder());
		Assert.assertNull(nameSort.getName());
	}

	@Test
	public void testSort() {
		FieldProcessorImpl fp = new FieldProcessorImpl("action", MetaDataProvider.getMetaData());
		PageRequest pageable = PageRequest.of(1, 50, Sort.by(new Order(Direction.ASC, "firstname").ignoreCase()));
		fp.setPageable(pageable);
		Assert.assertEquals(pageable, fp.getPageable());
		FieldDef field = fp.getField("firstname");
		org.appng.xml.platform.Sort firstnameSort = field.getSort();
		Assert.assertEquals(Integer.valueOf(0), firstnameSort.getPrio());
		Assert.assertEquals(SortOrder.ASC, firstnameSort.getOrder());
		Assert.assertNull(firstnameSort.getName());
		org.appng.xml.platform.Sort nameSort = fp.getField("name").getSort();
		Assert.assertNull(nameSort.getPrio());
		Assert.assertNull(nameSort.getOrder());
		Assert.assertNull(nameSort.getName());
		Assert.assertNull(nameSort.isIgnoreCase());
		Assert.assertTrue(fp.getField("firstname").getSort().isIgnoreCase());
	}

	@Test
	public void testGetResultset() {
		FieldProcessorImpl fp = new FieldProcessorImpl("action", MetaDataProvider.getMetaData());
		List<Linkpanel> linkpanels = new ArrayList<>();
		Linkpanel panel = new Linkpanel();
		panel.setId("thepanel");
		panel.setLocation(PanelLocation.INLINE);

		Link link = new Link();
		link.setId("1");
		link.setMode(Linkmode.INTERN);
		link.setDefault("true");
		link.setTarget("/foo/bar");
		Condition condition = new Condition();
		condition.setExpression("${current.father.name ne 'Darth'}");
		link.setCondition(condition);
		panel.getLinks().add(link);

		Link link2 = new Link();
		link2.setId("2");
		link2.setMode(Linkmode.INTERN);
		link2.setTarget("/foobar/foo/${current.id}/#{firstname}/${current.name}");
		Label conf = new Label();
		conf.setId("some.label[#{name}]");
		conf.setValue("May the force be with you, {0}!");
		link2.setConfirmation(conf);

		Condition c2 = new Condition();
		c2.setExpression("${empty current.offsprings}");
		link2.setCondition(c2);
		panel.getLinks().add(link2);

		linkpanels.add(panel);

		fp.addLinkPanels(linkpanels);
		Person vader = getDarkLord();
		fp.getMetaData().setResultSelector("${current.id == 1}");
		Resultset result = rss.getResultset(fp, vader.getOffsprings());
		XmlValidator.validate(result);
		XmlValidator.validate(fp.getMetaData(), "-metadata");
	}

	@Test
	public void testGetEmptyResultset() {
		FieldProcessorImpl fp = new FieldProcessorImpl("action", MetaDataProvider.getMetaData());
		Resultset result = rss.getResultset(fp, new ArrayList<>());
		XmlValidator.validate(result);
	}

	private Person getDarkLord() {
		Person vader = new Person(3, "Vader", "Darth");
		vader.setAge(234234L);
		vader.setSavings(2342535634D);
		vader.setSize(1.98f);
		vader.setBirthDate(new Date());
		ArrayList<Person> offsprings = new ArrayList<>();
		Person lea = new Person(1, "Lea", "Princess");
		lea.setFather(vader);
		lea.setCoordinate(new Coordinate(12.23d, 34.36d));
		offsprings.add(lea);
		Person luke = new Person(2, "Luke", "Skywalker");
		luke.setFather(vader);
		luke.setSize(1.85f);
		luke.setCoordinate(new Coordinate(17.23d, 14.36d));
		luke.setSavings(123456.3567d);
		offsprings.add(luke);
		vader.setOffsprings(offsprings);
		return vader;
	}

}
