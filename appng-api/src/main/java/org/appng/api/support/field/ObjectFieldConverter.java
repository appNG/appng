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
package org.appng.api.support.field;

import org.appng.api.FieldConverter;
import org.appng.api.FieldWrapper;
import org.appng.forms.RequestContainer;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;

/**
 * 
 * A {@link FieldConverter} for {@link FieldDef}initions of type
 * <ul>
 * <li>{@link FieldType#OBJECT}</li>
 * </ul>
 * 
 * @author Matthias Müller
 * 
 */
class ObjectFieldConverter extends ConverterBase {

	protected static final Logger LOG = LoggerFactory.getLogger(ObjectFieldConverter.class);

	ObjectFieldConverter() {
	}

	@Override
	public void setString(FieldWrapper field) {
		// nothing to do
	}

	@Override
	public void setObject(FieldWrapper field, RequestContainer request) {
		Class<?> targetClass = field.getTargetClass();
		if (null != targetClass) {
			Object currentObject = field.getObject();
			BeanWrapper wrapper = field.getBeanWrapper();
			if (null == currentObject) {
				try {
					currentObject = targetClass.newInstance();
					wrapper.setPropertyValue(field.getBinding(), currentObject);
					logSetObject(field, currentObject);
				} catch (InstantiationException | IllegalAccessException e) {
					LOG.error(String.format("error setting property %s for %s", field.getBinding(),
							wrapper.getWrappedInstance()), e);
				}
			} else {
				LOG.debug("no need to set property '{}' on {} (value is {}, type: {})", field.getBinding(),
						wrapper.getWrappedClass().getName(), currentObject, targetClass.getName());
			}
		}
	}

	protected Logger getLog() {
		return LOG;
	}

}
