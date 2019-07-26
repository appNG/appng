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
package org.appng.core.repository;

import java.util.List;

import org.appng.api.model.UserType;
import org.appng.core.domain.SubjectImpl;
import org.appng.persistence.repository.SearchRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubjectRepository extends SearchRepository<SubjectImpl, Integer> {

	SubjectImpl findByName(String name);

	SubjectImpl findByEmail(String email);

	List<SubjectImpl> findByUserType(UserType type);

	@Query("select distinct(s) from GroupImpl g join g.subjects s join g.roles pr left outer join pr.permissions p where pr.application.id = ?1")
	List<SubjectImpl> findSubjectsForApplication(Integer applicationId);

}
