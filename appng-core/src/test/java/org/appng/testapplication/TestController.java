/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.testapplication;

import org.appng.api.ApplicationController;
import org.appng.api.Environment;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.stereotype.Component;

@Component
public class TestController implements ApplicationController {

	public boolean start(Site site, Application application, Environment environment) {
		return true;
	}

	public boolean shutdown(Site site, Application application, Environment environment) {
		return true;
	}

	public boolean removeSite(Site site, Application application, Environment environment) {
		return true;
	}

	public boolean addSite(Site site, Application application, Environment environment) {
		return true;
	}

}
