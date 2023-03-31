/*
 * Copyright 2011-2023 the original author or authors.
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
package org.appng.api.support.validation;

import java.io.File;
import java.util.Arrays;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.appng.api.FileUpload;
import org.appng.api.FileUpload.Unit;
import org.appng.forms.FormUpload;
import org.appng.forms.impl.FormUploadBean;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class FileUploadValidatorTest {

	@Test
	public void testExactMatch() {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		BeanDescriptor constraintsForClass = validator.getConstraintsForClass(FileUploadValidatorTest.class);
		PropertyDescriptor constraintsForProperty = constraintsForClass.getConstraintsForProperty("formUpload");
		ConstraintDescriptor<?> desc = constraintsForProperty.getConstraintDescriptors().iterator().next();
		FileUpload fileUpload = (FileUpload) desc.getAnnotation();

		FileUploadValidator fileUploadValidator = new FileUploadValidator();
		fileUploadValidator.initialize(fileUpload);

		File file1 = Mockito.mock(File.class);
		Mockito.when(file1.length()).thenReturn(Unit.MB.getFactor() * 10 + 1);
		Mockito.when(file1.exists()).thenReturn(true);
		Mockito.when(file1.isFile()).thenReturn(true);
		long maxSize = Unit.MB.getFactor() * 20;
		FormUpload upload1 = new FormUploadBean(file1, "text.txt", "txt", Arrays.asList("text/plain"), maxSize);
		Assert.assertFalse(fileUploadValidator.isValid(upload1, null));

		File file2 = Mockito.mock(File.class);
		Mockito.when(file2.length()).thenReturn(Unit.MB.getFactor() * 10);
		Mockito.when(file2.exists()).thenReturn(true);
		Mockito.when(file2.isFile()).thenReturn(true);
		FormUpload upload2 = new FormUploadBean(file2, "text.txt", "txt", Arrays.asList("text/plain"), maxSize);
		Assert.assertTrue(fileUploadValidator.isValid(upload2, null));
	}

	@FileUpload(fileTypes = "txt")
	public FormUpload getFormUpload() {
		return null;
	}
}
