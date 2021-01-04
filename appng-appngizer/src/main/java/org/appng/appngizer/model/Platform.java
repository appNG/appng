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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="platform")
public class Platform extends org.appng.appngizer.model.xml.Platform implements UriAware {
	
	
	public Platform(boolean dbInitialized, boolean platformReloadAvailable){
		setSelf("/platform");
		if (dbInitialized) {
			addLink(new Link("database", "/platform/database"));
			addLink(new Link("property", "/platform/property"));
		}
		if(platformReloadAvailable){
			addLink(new Link("reload", "/platform/reload"));
		}
		addLink(new Link("system", "/platform/system"));
		addLink(new Link("environment", "/platform/environment"));
	}

}
