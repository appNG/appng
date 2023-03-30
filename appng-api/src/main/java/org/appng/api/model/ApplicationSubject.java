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

import java.util.List;

/**
 * A {@link ApplicationSubject} is an {@link Account} which owns several {@link Role}s. The {@link ApplicationSubject}s
 * of a {@link Application} can be retrieved by calling {@link Application#getApplicationSubjects()}.
 * 
 * @author Matthias Müller
 * 
 * @see Application#getApplicationSubjects()
 */
public interface ApplicationSubject extends Account {

	/**
	 * Returns a list of all {@link Role}s for this {@link ApplicationSubject}.
	 * 
	 * @return a list of all {@link Role}s for this {@link ApplicationSubject}
	 */
	List<Role> getRoles();

}
