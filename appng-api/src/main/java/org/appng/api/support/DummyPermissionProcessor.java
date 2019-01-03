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
package org.appng.api.support;

import org.appng.api.PermissionOwner;
import org.appng.api.PermissionProcessor;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A {@link PermissionProcessor} that always returns {@code true} for
 * <ul>
 * <li>{@link #hasPermission(String)}
 * <li>{@link #hasPermissions(PermissionOwner)}
 * </ul>
 * 
 * @author Matthias Müller
 * 
 */
public class DummyPermissionProcessor extends DefaultPermissionProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DummyPermissionProcessor.class);

	public DummyPermissionProcessor(Subject subject, Site site, Application application) {
		super(subject, site, application);
		LOGGER.debug("creating instance for subject {}, site {}, application {}", subject.getAuthName(),
				site.getName(), application.getName());
	}

	public boolean hasPermissions(PermissionOwner permissionOwner) {
		LOGGER.debug("granting permission for ", permissionOwner.getName());
		return true;
	}

	public boolean hasPermission(String reference) {
		LOGGER.debug("granting permission ", reference);
		return true;
	}

}
