/*
 * Copyright 2011-2017 the original author or authors.
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
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A {@link FieldConverter} for {@link FieldDef}initions of type {@link FieldType#COORDINATE}.
 * 
 * @author Matthias Müller
 * 
 */
class CoordinateFieldConverter extends ConverterBase {

	protected static final Logger LOG = LoggerFactory.getLogger(CoordinateFieldConverter.class);

	protected Logger getLog() {
		return LOG;
	}

	@Override
	public void setString(FieldWrapper field) {
		// do nothing
	}

}
