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
package org.appng.appngizer.controller;

import java.io.IOException;
import java.util.Properties;

import org.appng.core.controller.PlatformStartup;
import org.appng.core.service.MigrationService;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class AppNGizerConfigurer extends PropertyPlaceholderConfigurer {

	@Override
	protected void loadProperties(Properties props) throws IOException {
		super.loadProperties(props);
		String dbType = props.getProperty(MigrationService.DATABASE_TYPE).toUpperCase();
		props.put(MigrationService.DATABASE_TYPE, dbType);
		PlatformStartup.applySystem(props);
		setProperties(props);
	}

	public Properties getProps() {
		return localProperties[0];
	}

}
