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
package org.appng.forms;

import java.io.File;
import java.util.Arrays;

import org.appng.forms.impl.FormUploadBean;
import org.junit.Assert;
import org.junit.Test;

public class FormUploadTest {

	int maxSize = 10 * 1024 * 1024;

	@Test
	public void testFormUploadOK() throws Exception {
		String[] type = new String[] { "jpg" };
		File file = new File(getClass().getClassLoader().getResource("deathstar.jpg").toURI());
		FormUpload formUpload = new FormUploadBean(file, "test.jpg", "image/jpg", Arrays.asList(type), maxSize);

		Assert.assertTrue(formUpload.isValidFile());
		Assert.assertTrue(formUpload.isValidSize());
		Assert.assertTrue(formUpload.isValidType());
		Assert.assertTrue(formUpload.isValid());

		Assert.assertTrue(formUpload.isValid(type, 0, maxSize));
	}

	@Test
	public void testFormUploadError() throws Exception {
		File file = new File(getClass().getClassLoader().getResource("deathstar.jpg").toURI());
		FormUpload formUpload = new FormUploadBean(file, "test.jpg", "image/jpg", Arrays.asList("png"), 10);
		String[] type = new String[] { "png" };

		Assert.assertFalse(new FormUploadBean(null, "test.jpg", "image/png", Arrays.asList("png"), 10).isValidFile());
		Assert.assertFalse(formUpload.isValidSize());
		Assert.assertFalse(formUpload.isValidType());
		Assert.assertFalse(formUpload.isValid());

		Assert.assertFalse(formUpload.isValid(type, 0, 10));
	}
}
