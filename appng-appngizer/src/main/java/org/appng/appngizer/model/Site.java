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
package org.appng.appngizer.model;

import org.appng.core.domain.SiteImpl;

public class Site extends org.appng.appngizer.model.xml.Site implements UriAware {

	public Site(String name) {
		this.name = name;
		addLink(new Link("properties", "/site/" + name + "/property"));
		addLink(new Link("applications", "/site/" + name + "/application"));
		addLink(new Link("databases", "/site/" + name + "/database"));
	}

	public static Site fromDomain(SiteImpl siteImpl) {
		Site site = new Site(siteImpl.getName());
		site.setActive(siteImpl.isActive());
		site.setHost(siteImpl.getHost());
		site.setDomain(siteImpl.getDomain());
		site.setDescription(siteImpl.getDescription());
		site.setSelf("/site/" + site.getName());
		site.setCreateRepositoryPath(siteImpl.isCreateRepository());
		return site;
	}

	public static SiteImpl toDomain(org.appng.appngizer.model.xml.Site s) {
		SiteImpl site = new SiteImpl();
		site.setName(s.getName());
		site.setActive(s.isActive());
		site.setHost(s.getHost());
		site.setDomain(s.getDomain());
		site.setDescription(s.getDescription());
		site.setCreateRepository(s.isCreateRepositoryPath());
		return site;
	}

}
