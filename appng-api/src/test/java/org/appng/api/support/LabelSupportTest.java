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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.appng.api.Request;
import org.appng.el.ExpressionEvaluator;
import org.appng.xml.platform.Config;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Labels;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.support.StaticMessageSource;

public class LabelSupportTest {

	private static final String KEY = "key";

	private static final String RESULT = "foo - bar - 5";

	@Mock
	private Request request;

	private ExpressionEvaluator expressionEvaluator;

	@Before
	public void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void testLabelParams() {
		LabelSupport labelSupport = getLabelSupport();
		Label label = new Label();
		label.setId(KEY);
		label.setParams("foo,'bar',${id}");
		labelSupport.setLabel(label, expressionEvaluator, null);
		Assert.assertEquals(RESULT, label.getValue());
	}

	@Test
	public void testLabelParamsCurrent() {
		LabelSupport labelSupport = getLabelSupport();
		Label label = new Label();
		label.setId(KEY);
		label.setParams("foo,'bar',${current.toString()}");
		labelSupport.setLabel(label, expressionEvaluator, null);
		Assert.assertEquals(RESULT.replace("5", "${current.toString()}"), label.getValue());
		expressionEvaluator.setVariable(AdapterBase.CURRENT, "5");
		labelSupport.setLabel(label, expressionEvaluator, null);
		Assert.assertEquals(RESULT, label.getValue());
	}

	@Test
	public void testFieldParams() {
		LabelSupport labelSupport = getLabelSupport();
		Map<String, String> params = new HashMap<>();
		params.put("name", "foo");
		params.put("name.with.dots", "foo");
		Label label = new Label();
		label.setId(KEY);
		label.setParams("#{name},'bar',${id}");
		HashParameterSupport fieldParameters = new HashParameterSupport(params);
		labelSupport.setLabel(label, expressionEvaluator, fieldParameters);
		Assert.assertEquals(RESULT, label.getValue());

		label.setParams("#{name.with.dots},'bar',${id}");
		labelSupport.setLabel(label, expressionEvaluator, fieldParameters);
		Assert.assertEquals(RESULT.replace("foo", "#{name.with.dots}"), label.getValue());

		fieldParameters.allowDotInName();
		labelSupport.setLabel(label, expressionEvaluator, fieldParameters);
		Assert.assertEquals(RESULT, label.getValue());
	};

	@Test
	public void testI18n() {
		LabelSupport labelSupport = getLabelSupport();
		Map<String, String> params = new HashMap<>();
		params.put("name", "foo");
		Label label = new Label();
		label.setId(KEY);
		label.setValue("${i18n.message('key','#{name}','bar',id)}");
		HashParameterSupport fieldParameters = new HashParameterSupport(params);
		labelSupport.setLabel(label, expressionEvaluator, fieldParameters);
		Assert.assertEquals(RESULT, label.getValue());

		label.setValue("${i18n.formatDate(date,i18n.message('dateFormat'))}");
		labelSupport.setLabel(label, expressionEvaluator, fieldParameters);
		Assert.assertEquals("2013-03-08", label.getValue());

		label.setValue("${i18n.formatNumber(number,'0,000,000.00')}");
		labelSupport.setLabel(label, expressionEvaluator, fieldParameters);
		Assert.assertEquals("42.000.000,42", label.getValue());

		label.setValue("${i18n.format('%1$td.%1$tm.%1$tY %2$,.2f %3$s', date, number, 'foo')}");
		labelSupport.setLabel(label, expressionEvaluator, fieldParameters);
		Assert.assertEquals("08.03.2013 42.000.000,42 foo", label.getValue());
	};

	@Test
	public void testLabels() {
		LabelSupport labelSupport = getLabelSupport();
		Labels labels = new Labels();
		Config config = new Config();
		config.setLabels(labels);
		Label l1 = new Label();
		l1.setId(KEY);
		l1.setParams("foo,'bar',${id}");
		labels.getLabels().add(l1);
		Label l2 = new Label();
		l2.setId("key2");
		labels.getLabels().add(l2);
		labelSupport.setLabels(config, expressionEvaluator, null);

		Assert.assertEquals(RESULT, l1.getValue());
		Assert.assertEquals("some value", l2.getValue());
	}

	@Test
	public void testLabelNoKey() {
		LabelSupport labelSupport = getLabelSupport();
		Label label = new Label();
		label.setValue("key2");
		labelSupport.setLabel(label, expressionEvaluator, null);
		Assert.assertEquals("some value", label.getValue());
	}

	@Test
	public void testLabelValueIsSet() {
		LabelSupport labelSupport = getLabelSupport();
		Label label = new Label();
		label.setValue("some value");
		label.setId("key2");
		labelSupport.setLabel(label, expressionEvaluator, null);
		Assert.assertEquals("some value", label.getValue());
	}

	public LabelSupport getLabelSupport() {
		final Locale locale = Locale.GERMAN;
		final StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage(KEY, locale, "{0} - {1} - {2}");
		messageSource.addMessage("key2", locale, "some value");
		messageSource.addMessage("dateFormat", locale, "yyyy-MM-dd");

		LabelSupport labelSupport = new LabelSupport(messageSource, locale);

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("id", 5);
		parameters.put("date", new GregorianCalendar(2013, Calendar.MARCH, 8).getTime());
		parameters.put("number", Double.valueOf(42000000.42d));
		expressionEvaluator = new ExpressionEvaluator(parameters);
		Mockito.when(request.getLocale()).thenReturn(locale);
		expressionEvaluator.setVariable(ApplicationRequest.I18N_VAR, new I18n(request));
		Mockito.when(request.getExpressionEvaluator()).thenReturn(expressionEvaluator);
		Mockito.when(request.getMessage(Mockito.anyString(), Mockito.any())).thenAnswer(new Answer<String>() {

			public String answer(InvocationOnMock invocation) throws Throwable {
				String key = (String) invocation.getArguments()[0];
				int length = invocation.getArguments().length;
				Object[] args = ArrayUtils.subarray(invocation.getArguments(), 1, length);
				return messageSource.getMessage(key, args, locale);
			}
		});
		return labelSupport;
	}
}
