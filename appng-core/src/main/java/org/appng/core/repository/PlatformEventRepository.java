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
package org.appng.core.repository;

import java.util.List;

import org.appng.core.domain.PlatformEvent;
import org.appng.persistence.repository.SearchRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlatformEventRepository extends SearchRepository<PlatformEvent, Integer> {

	@Query("select distinct(e.user) from PlatformEvent e order by e.user")
	List<String> findDistinctUsers();

	@Query("select distinct(e.application) from PlatformEvent e order by e.application")
	List<String> findDistinctApplications();

	@Query("select distinct(e.origin) from PlatformEvent e order by e.origin")
	List<String> findDistinctOrigins();

	@Query("select distinct(e.hostName) from PlatformEvent e order by e.hostName")
	List<String> findDistinctHostNames();

}
