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
package org.appng.core.domain;

import java.io.Serializable;
import java.util.Date;

import org.appng.api.model.Named;
import org.appng.api.model.Versionable;

/**
 * Marker interface for domain objects that should be audited using {@link PlatformEventListener}.
 * 
 * @author Matthias Müller
 *
 * @param <T>
 *            the type of the ID
 */
public interface Auditable<T extends Serializable> extends Named<T>, Versionable<Date> {

	default String getAuditName() {
		return getClass().getSimpleName().replace("Impl", "");
	}

}
