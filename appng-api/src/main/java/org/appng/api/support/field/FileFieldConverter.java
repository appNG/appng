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
package org.appng.api.support.field;

import java.util.List;

import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.forms.FormUpload;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.slf4j.Logger;
import org.springframework.core.convert.ConversionService;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Base {@link FieldConverter} for {@link FieldDef}initions of type {@link FieldType#FILE} and
 * {@link FieldType#FILE_MULTIPLE}.
 * 
 * @author Matthias Müller
 * 
 */
@Slf4j
class FileFieldConverter extends ConverterBase {

	public FileFieldConverter(ConversionService conversionService) {
		setConversionService(conversionService);
	}

	@Override
	public void setObject(FieldWrapper field, RequestContainer request) {
		List<FormUpload> formUploads = request.getFormUploads(field.getBinding());
		if (null != formUploads) {
			Object object = null;
			if (FieldType.FILE.equals(field.getType())) {
				if (formUploads.size() == 1) {
					object = formUploads.get(0);
				}
			} else if (FieldType.FILE_MULTIPLE.equals(field.getType())) {
				object = formUploads;
			}
			logSetObject(field, object);
			field.setObject(object);
		}
	}

	protected Logger getLog() {
		return LOGGER;
	}

}
