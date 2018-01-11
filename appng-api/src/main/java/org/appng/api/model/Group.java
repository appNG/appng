/*
 * Copyright 2011-2018 the original author or authors.
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
 * A {@link Group} is a set of {@link Subject}s which has several {@link Role}s.
 * 
 * @author Matthias Müller
 * 
 */
public interface Group extends Named<Integer> {

	/**
	 * Returns all {@link Subject}s belonging to this {@link Group}.
	 * 
	 * @return all {@link Subject}s belonging to this {@link Group}
	 */
	Set<Subject> getSubjects();

	/**
	 * Returns all {@link Role}s belonging to this {@link Group}.
	 * 
	 * @return all {@link Role}s belonging to this {@link Group}.
	 */
	Set<Role> getRoles();

}
