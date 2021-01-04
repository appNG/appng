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
package org.appng.cli;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

/**
 * Utility-class to support {@link CliBootstrap}.
 * 
 * @author Matthias Herlitzius
 */
class CliBootstrapEnvironment {

	/**
	 * Retrieves the property with the given name from the system environment and returns a new {@link File} created
	 * from the retrieved value.
	 * 
	 * @param name
	 *             the name of the system property
	 * 
	 * @return the file, or {@code null} if no such system property exists.
	 */
	File getFileFromEnv(String name) {
		return getAbsoluteFile(System.getenv(name));
	}

	File getAbsoluteFile(String name) {
		if (null != name) {
			return getAbsoluteFile(new File(name));
		} else {
			return null;
		}
	}

	File getAbsoluteFile(File file) {
		if (null != file) {
			return new File(FilenameUtils.normalize(file.getAbsolutePath()));
		} else {
			return null;
		}
	}

}
