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

import org.appng.appngizer.model.xml.Application;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class ApplicationControllerTest extends ControllerTest {

	@Test
	public void test() throws Exception {
		differenceListener.ignoreDifference("/properties/property/description/text()");
		installApplication();
		getAndVerify("/application", "xml/application-list.xml", HttpStatus.OK);

		getAndVerify("/application/demo-application", "xml/application-show.xml", HttpStatus.OK);
		Application app = new Application();
		app.setName("this has no effect");
		app.setDisplayName("ACME app");
		app.setPrivileged(true);
		app.setHidden(true);
		app.setFileBased(false);
		app.setVersion("this has no effect");

		putAndVerify("/application/demo-application", "xml/application-update.xml", app, HttpStatus.OK);

		deleteAndVerify("/application/demo-application", null, HttpStatus.NO_CONTENT);
	}
}
