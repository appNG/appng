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
package org.appng.cli.commands.property;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.core.domain.PropertyImpl;

import com.beust.jcommander.ParameterException;

public class PropertyHelper {

	static void validate(String value, String clob) {
		if (StringUtils.isNoneEmpty(value, clob) || (StringUtils.isBlank(value) && StringUtils.isBlank(clob))) {
			throw new ParameterException("Use either -c or -v!");
		}
	}

	static void update(PropertyImpl property, String value, String clob, boolean isDefault) throws BusinessException {
		if (null != clob) {
			File path = new File(clob).getAbsoluteFile();
			try {
				property.setClob(FileUtils.readFileToString(path, StandardCharsets.UTF_8));
				property.setDefaultString(null);
				property.setString(null);
			} catch (IOException e) {
				throw new BusinessException("Error while reading: " + path.toString(), e);
			}
		} else {
			if(isDefault){
				property.setDefaultString(value);
			}else{
				property.setString(value);
			}
			property.setClob(null);
		}
	}

}
