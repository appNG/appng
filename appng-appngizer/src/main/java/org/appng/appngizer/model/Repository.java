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
package org.appng.appngizer.model;

import java.net.URI;
import java.net.URISyntaxException;

import org.appng.appngizer.model.xml.Links;
import org.appng.appngizer.model.xml.RepositoryMode;
import org.appng.appngizer.model.xml.RepositoryType;
import org.appng.core.domain.RepositoryImpl;
import org.springframework.web.util.UriComponentsBuilder;

public class Repository extends org.appng.appngizer.model.xml.Repository implements UriAware {

	public static Repository fromDomain(org.appng.core.model.Repository r) {
		Repository repo = new Repository();
		repo.setName(r.getName());
		repo.setEnabled(r.isActive());
		repo.setPublished(r.isPublished());
		repo.setStrict(r.isStrict());
		repo.setDescription(r.getDescription());
		repo.setRemoteName(r.getRemoteRepositoryName());
		repo.setMode(RepositoryMode.valueOf(r.getRepositoryMode().name()));
		repo.setType(RepositoryType.valueOf(r.getRepositoryType().name()));
		repo.setUri(r.getUri().toString());
		repo.setSelf("/repository/{name}");
		repo.setLinks(new Links());
		repo.getLinks().getLink().add(new Link("install", "/install"));
		if (org.appng.core.model.RepositoryType.LOCAL.equals(r.getRepositoryType())) {
			repo.getLinks().getLink().add(new Link("upload", "/upload"));
		}
		return repo;
	}

	public static RepositoryImpl toDomain(org.appng.appngizer.model.xml.Repository repo) throws URISyntaxException {
		RepositoryImpl r = new RepositoryImpl();
		r.setName(repo.getName());
		r.setActive(repo.isEnabled());
		r.setPublished(repo.isPublished());
		r.setStrict(repo.isStrict());
		r.setDescription(repo.getDescription());
		r.setRemoteRepositoryName(repo.getRemoteName());
		r.setRepositoryMode(org.appng.core.model.RepositoryMode.valueOf(repo.getMode().name()));
		r.setRepositoryType(org.appng.core.model.RepositoryType.valueOf(repo.getType().name()));
		r.setUri(new URI(repo.getUri()));
		return r;
	}

	@Override
	public void applyUriComponents(UriComponentsBuilder builder) {
		String uriString = builder.path("/repository/{name}").buildAndExpand(encode(name)).toUriString();
		if (null != packages) {
			for (org.appng.appngizer.model.xml.Package p : packages.getPackage()) {
				((UriAware) p).setSelf(uriString + "/" + encode(p.getName()));
			}
		}
		if (null != getLinks()) {
			for (org.appng.appngizer.model.xml.Link link : getLinks().getLink()) {
				link.setSelf(uriString + link.getSelf());
			}
		}
		setSelf(uriString);
	}
}
