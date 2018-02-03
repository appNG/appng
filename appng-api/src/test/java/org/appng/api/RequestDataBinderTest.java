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

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.forms.FormUpload;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Test for {@link RequestDataBinder}
 * 
 * @author Matthias Müller
 *
 */
public class RequestDataBinderTest {

	protected static final FastDateFormat FDF = FastDateFormat.getInstance("dd.MM.yyyy");
	protected static final String NAME = "name";
	protected static final String INTEGER_LIST = "integerList";
	protected static final String BIRTH_DATE = "birthDate";
	protected static final String PICTURE = "picture";
	protected static final String MORE_PICTURES = "morePictures";

	@Mock
	private Request request;

	@Test
	public void doTest() {
		MockitoAnnotations.initMocks(this);

		Map<String, List<String>> paramters = new HashMap<>();
		paramters.put(NAME, Arrays.asList("Doe"));
		paramters.put(INTEGER_LIST, Arrays.asList("1", "2", "3"));
		paramters.put(BIRTH_DATE, Arrays.asList("14.05.1944"));
		Mockito.when(request.getParameterNames()).thenReturn(paramters.keySet());
		Mockito.when(request.getParameterList(Mockito.anyString())).then(new Answer<List<String>>() {
			public List<String> answer(InvocationOnMock invocation) throws Throwable {
				return paramters.get(invocation.getArgumentAt(0, String.class));
			}
		});

		Map<String, List<FormUpload>> formUploads = getFormUploads();
		List<FormUpload> pictures = formUploads.get(PICTURE);

		Mockito.when(request.getFormUploads()).thenReturn(formUploads);
		Mockito.when(request.getFormUploads(PICTURE)).thenReturn(pictures);
		Mockito.when(request.getFormUploads(MORE_PICTURES)).thenReturn(pictures);
		ConfigurableConversionService conversionService = getConversionService();
		RequestDataBinder<Person> requestDataBinder = new RequestDataBinder<Person>(new Person(), request,
				conversionService);
		Person person = requestDataBinder.bind();
		validate(pictures, person);
	}

	protected Map<String, List<FormUpload>> getFormUploads() {
		Map<String, List<FormUpload>> formUploads = new HashMap<>();
		FormUpload pictureUpload = Mockito.mock(FormUpload.class);
		formUploads.put(PICTURE, Arrays.asList(pictureUpload));
		formUploads.put(MORE_PICTURES, formUploads.get(PICTURE));
		return formUploads;
	}

	protected void validate(List<FormUpload> pictures, Person person) {
		Assert.assertEquals("Doe", person.getName());
		Assert.assertEquals("14.05.1944", FDF.format(person.getBirthDate()));
		Assert.assertThat(person.getIntegerList(), CoreMatchers.is(Arrays.asList(1, 2, 3)));
		Assert.assertEquals(pictures.get(0), person.getPicture());
		Assert.assertEquals(pictures, person.getMorePictures());
	}

	protected ConfigurableConversionService getConversionService() {
		ConfigurableConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<String, Date>() {
			public Date convert(String source) {
				try {
					return FDF.parse(source);
				} catch (ParseException e) {
				}
				return null;
			}
		});
		return conversionService;
	}

}
