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
package org.appng.appngizer.model;

import java.nio.charset.StandardCharsets;

import org.appng.appngizer.model.xml.Links;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

public interface UriAware {

	default void applyUriComponents(UriComponentsBuilder builder) {
		if (null != getLinks()) {
			for (org.appng.appngizer.model.xml.Link link : getLinks().getLink()) {
				link.setSelf(builder.cloneBuilder().path(link.getSelf()).build().toUriString());
			}
		}
		setSelf(builder.path(getSelf()).build().toUriString());
	}

	Links getLinks();
	
	void setLinks(Links links);

	default void addLink(org.appng.appngizer.model.xml.Link link) {
		if (null == getLinks()) {
			setLinks(new Links());
		}
		getLinks().getLink().add(link);
	}


	default String encode(String name) {
		try {
			return UriUtils.encode(name, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			// never ever
		}
		return name;
	}

	String getSelf();

	void setSelf(String self);
}
