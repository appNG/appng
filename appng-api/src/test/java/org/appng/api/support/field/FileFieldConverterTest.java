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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appng.api.FieldConverter.DatafieldOwner;
import org.appng.forms.FormUpload;
import org.appng.forms.impl.FormUploadBean;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class FileFieldConverterTest extends AbstractFieldConverterTest {

	private FormUpload textFile;
	private FormUpload pdfFile;

	private FieldType type;

	public void setup(FieldType type) throws Exception {
		this.type = type;
		super.setup(type);
		textFile = new FormUploadBean(new File("test.txt"), "test.txt", "text/plain", Arrays.asList(".txt"), 1024000);
		pdfFile = new FormUploadBean(new File("test.pdf"), "test.pdf", "application/pdf", Arrays.asList(".pdf"),
				1024000);
	}

	public void setFile() throws Exception {
		setup(FieldType.FILE);
		Mockito.when(request.getFormUploads(OBJECT)).thenReturn(Arrays.asList(textFile));
	}

	public void setFiles() throws Exception {
		setup(FieldType.FILE_MULTIPLE);
		Mockito.when(request.getFormUploads(OBJECT)).thenReturn(Arrays.asList(textFile, pdfFile));
	}

	public Container<?> getContainer() {
		if (FieldType.FILE.equals(type)) {
			return new Container<FormUpload>() {
			};
		}
		if (FieldType.FILE_MULTIPLE.equals(type)) {
			return new Container<List<FormUpload>>() {
			};
		}
		return null;
	}

	@Test
	public void testSetObject() throws Exception {
		setFile();
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(textFile, fieldWrapper.getObject());
	}

	@Test
	public void testSetObjects() throws Exception {
		setFiles();
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(Arrays.asList(textFile, pdfFile), fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectEmptyValue() throws Exception {
		setFiles();
		Mockito.when(request.getFormUploads(OBJECT)).thenReturn(new ArrayList<>());
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertEquals(new ArrayList<>(), fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectNull() throws Exception {
		setFile();
		Mockito.when(request.getFormUploads(OBJECT)).thenReturn(null);
		fieldConverter.setObject(fieldWrapper, request);
		Assert.assertNull(fieldWrapper.getObject());
	}

	@Test
	public void testSetObjectInvalidValue() throws Exception {
		// nothing to do
	}

	@Test
	public void testSetString() throws Exception {
		setFile();
		beanWrapper.setPropertyValue(OBJECT, pdfFile);
		fieldWrapper.setBinding("object.originalFilename");
		fieldConverter.setString(fieldWrapper);
		Assert.assertEquals("test.pdf", fieldWrapper.getStringValue());
	}

	@Test
	public void testSetStringNullObject() throws Exception {
		setFile();
		fieldWrapper.setBinding("object.originalFilename");
		fieldConverter.setString(fieldWrapper);
		Assert.assertNull(fieldWrapper.getStringValue());
	}

	@Test
	public void testSetStringInvalidType() throws Exception {
		// nothing to do
	}

	@Test
	public void testAddField() throws Exception {
		setFile();
		fieldWrapper.setBinding("object.originalFilename");
		beanWrapper.setPropertyValue(OBJECT, pdfFile);
		DatafieldOwner dataFieldOwner = getDatafieldOwner();
		fieldConverter.addField(dataFieldOwner, fieldWrapper);
		Datafield datafield = dataFieldOwner.getFields().get(0);
		Assert.assertEquals("object", datafield.getName());
		Assert.assertEquals(pdfFile.getOriginalFilename(), datafield.getValue());
		Assert.assertEquals(0, datafield.getFields().size());
	}

}
