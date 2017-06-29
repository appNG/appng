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
package org.appng.cli.validators;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * An {@code com.beust.jcommander.IParameterValidator} assuring that a file exists.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class FileExists implements IParameterValidator {

	public void validate(String name, String value) throws ParameterException {
		File file = new File(value);
		if (!file.exists()) {
			throw new ParameterException("Parameter " + name + " should refer to an existing file. File not found: "
					+ FilenameUtils.normalize(file.getAbsolutePath()));
		}
	}
}
