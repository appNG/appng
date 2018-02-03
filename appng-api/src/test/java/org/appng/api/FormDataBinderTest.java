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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.forms.FormUpload;
import org.appng.forms.Request;
import org.appng.formtags.Form;
import org.appng.formtags.FormData;
import org.appng.formtags.FormElement;
import org.appng.formtags.FormElement.InputType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.support.ConfigurableConversionService;

/**
 * Test for {@link FormDataBinder}
 * 
 * @author Matthias Müller
 *
 */
public class FormDataBinderTest extends RequestDataBinderTest {

	private static final String FIRSTNAME = "firstname";

	@Mock
	private Form form;

	@Mock
	private Request request;

	@Test
	public void doTest() {
		MockitoAnnotations.initMocks(this);
		FormData formData = Mockito.mock(FormData.class);
		Mockito.when(form.getFormData()).thenReturn(formData);
		Mockito.when(form.getRequest()).thenReturn(request);

		Map<String, List<FormUpload>> formUploads = getFormUploads();
		List<FormUpload> pictures = formUploads.get("picture");

		FormElement name = mockElement(NAME, Arrays.asList("Doe"));
		FormElement integerList = mockElement(INTEGER_LIST, Arrays.asList("1", "2", "3"));
		FormElement birthDate = mockElement(BIRTH_DATE, Arrays.asList("14.05.1944"));
		FormElement picture = mockUpload(PICTURE, pictures);
		FormElement morePictures = mockUpload(MORE_PICTURES, pictures);

		Mockito.when(formData.getElements())
				.thenReturn(Arrays.asList(name, integerList, birthDate, picture, morePictures));

		Map<String, List<String>> paramters = new HashMap<>();
		paramters.put(FIRSTNAME, Arrays.asList("John"));
		Mockito.when(request.getParameterNames()).thenReturn(paramters.keySet());
		Mockito.when(request.getParameterList(FIRSTNAME)).thenReturn(paramters.get(FIRSTNAME));

		ConfigurableConversionService conversionService = getConversionService();
		FormDataBinder<Person> formDataBinder = new FormDataBinder<Person>(new Person(), form, conversionService);
		formDataBinder.setBindAdditionalParams(true);

		Person person = formDataBinder.bind();
		validate(pictures, person);
		Assert.assertEquals("John", person.getFirstname());
	}

	private FormElement mockElement(String name, List<String> values) {
		FormElement element = Mockito.mock(FormElement.class);
		Mockito.when(element.getName()).thenReturn(name);
		Mockito.when(element.getRequestValues()).thenReturn(values);
		return element;
	}

	private FormElement mockUpload(String name, List<FormUpload> values) {
		FormElement element = Mockito.mock(FormElement.class);
		Mockito.when(element.getName()).thenReturn(name);
		Mockito.when(element.getFormUploads()).thenReturn(values);
		Mockito.when(element.getInputType()).thenReturn(InputType.FILE);
		return element;
	}

}
