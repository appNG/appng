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
package org.appng.api.model;

import java.util.Set;

/**
 * A {@link Role} is used for grouping the {@link Permission}s of a {@link Application}. It is getting assigned to
 * multiple {@link Group}s.
 * 
 * @author Matthias Müller
 * 
 * @see Permission
 * @see Group
 */
public interface Role extends Named<Integer> {

	/**
	 * Returns the {@link Application} this {@link Role} belongs to.
	 * 
	 * @return the {@link Application}
	 */
	Application getApplication();

	/**
	 * Returns the {@link Permission}s for this {@link Role}, which are a subset of all {@link Permission}s of the
	 * {@link Application}.
	 * 
	 * @return the {@link Permission}s
	 */
	Set<Permission> getPermissions();
}
