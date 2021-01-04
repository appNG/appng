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
package org.appng.appngizer.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Home extends org.appng.appngizer.model.xml.Home implements UriAware {

	public Home(String version, boolean dbInitialized) {
		this.version = version;
		setSelf("/");
		addLink(new Link("platform", "/platform"));
		if (dbInitialized) {
			addLink(new Link("site", "/site"));
			addLink(new Link("application", "/application"));
			addLink(new Link("repository", "/repository"));
			addLink(new Link("subject", "/subject"));
			addLink(new Link("group", "/group"));
		}
	}

}
