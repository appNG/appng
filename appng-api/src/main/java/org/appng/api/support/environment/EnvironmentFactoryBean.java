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
package org.appng.api.support.environment;

import org.appng.api.Environment;
import org.springframework.beans.factory.FactoryBean;

public class EnvironmentFactoryBean implements FactoryBean<Environment> {

	Environment environment;

	public EnvironmentFactoryBean() {
		environment = new DefaultEnvironment();
	}

	public Environment getObject() throws Exception {
		return environment;
	}

	public Class<?> getObjectType() {
		return Environment.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
