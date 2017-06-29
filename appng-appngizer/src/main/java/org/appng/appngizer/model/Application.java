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
package org.appng.appngizer.model;

import org.appng.core.domain.ApplicationImpl;

public class Application extends org.appng.appngizer.model.xml.Application implements UriAware {

	public Application(String name) {
		this.name = name;
		setSelf("/application/" + name);
	}

	public void addLinks() {
		addLink(new Link("role", "/application/" + name + "/role"));
		addLink(new Link("permission", "/application/" + name + "/permission"));
		addLink(new Link("property", getSelf() + "/property"));
	}

	public Application(String name, String site) {
		this.name = name;
		setSelf("/site/" + site + "/application/" + name);
	}

	public static Application fromDomain(ApplicationImpl appImpl) {
		return fromDomain(appImpl, null);
	}

	public static Application fromDomain(ApplicationImpl appImpl, String site) {
		Application app = new Application(appImpl.getName());
		app.setVersion(appImpl.getApplicationVersion());
		app.setDisplayName(appImpl.getDisplayName());
		app.setCore(appImpl.isCoreApplication());
		app.setFileBased(appImpl.isFileBased());
		app.setHidden(appImpl.isHidden());
		app.setSelf(null == site ? "/application/" + app.getName() : "/site/" + site + "/application/" + app.getName());
		return app;
	}

	public static ApplicationImpl toDomain(Application a) {
		ApplicationImpl app = new ApplicationImpl();
		app.setName(a.getName());
		app.setDisplayName(a.getDisplayName());
		app.setFileBased(a.isFileBased());
		app.setHidden(a.isHidden());
		app.setCoreApplication(a.isCore());
		return app;
	}

}
