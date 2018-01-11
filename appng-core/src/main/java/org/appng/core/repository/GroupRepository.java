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
package org.appng.core.repository;

import java.util.List;

import org.appng.core.domain.GroupImpl;
import org.appng.persistence.repository.SearchRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface GroupRepository extends SearchRepository<GroupImpl, Integer> {

	GroupImpl findByName(String name);

	@Query("select g.id from GroupImpl g where g.name in (?1)")
	List<Integer> getGroupIdsForNames(List<String> groupNames);

	@Query("select g from GroupImpl g join g.roles r where r.id = ?1")
	List<GroupImpl> findGroupsForApplicationRole(Integer applicationRoleId);

	@Query("select g from GroupImpl g left join fetch g.roles r left join fetch r.permissions p where g.id = ?1")
	GroupImpl getGroup(Integer groupId);

	List<GroupImpl> findByDefaultAdmin(boolean defaultAdmin);

}
