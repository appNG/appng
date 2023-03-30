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

import java.io.Serializable;
import java.util.List;

/**
 * If a {@link Application} want's to authorize a domain object to several {@link Role}s, the domain-object needs to
 * implement {@link Authorizable}.<br/>
 * The {@link Role}s provided by the {@link Application} can be obtained by calling {@link Application#getRoles()}.
 * Since the {@link Role}s are maintained by appNG and not by the {@link Application} itself, the {@link Authorizable}
 * just needs to maintain the IDs of the authorized {@link Role}s.<br/>
 * When using JPA for persisting domain objects, this can easily be done by using a
 * {@code javax.persistence.ElementCollection}.
 * 
 * <pre>
 * &#064;ElementCollection
 * public List&lt;Integer&gt; getRoleIds() {
 * 	return roleIds;
 * }
 * </pre>
 * 
 * @author Matthias Müller
 * 
 * @see Subject#isAuthorized(Authorizable)
 * @see Application#getRoles()
 */
public interface Authorizable<T extends Serializable> extends Named<T> {

	/**
	 * Returns a list containing the IDs of all {@link Role}s which are authorized to access this {@link Authorizable}.
	 * 
	 * @return a list containing the IDs of all {@link Role}s which are authorized to access this {@link Authorizable}
	 */
	public List<Integer> getRoleIds();

}
