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
package org.appng.appngizer.controller;

import org.junit.Test;
import org.springframework.http.HttpStatus;

public class DatabaseControllerTest extends ControllerTest {

	@Test
	public void test() throws Exception {
		ignorePasswordAndInstalledDate();
		getAndVerify("/platform/database", "xml/database.xml", HttpStatus.OK);
	}

	@Test
	public void testInitialize() throws Exception {
		ignorePasswordAndInstalledDate();
		postAndVerify("/platform/database/initialize", "xml/database-init.xml", null, HttpStatus.OK);
	}

	private void ignorePasswordAndInstalledDate() {
		differenceListener.ignoreDifference("/database/password/text()");
		differenceListener.ignoreDifference("/database/versions/version/@installed");
	}
}
