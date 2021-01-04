/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.Arrays;

import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.RoleImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = GroupRepositoryTest.class)
public class GroupRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	GroupRepository repository;

	@Autowired
	RoleRepository applicationRoleRepository;

	@Autowired
	ApplicationRepository applicationRepository;

	public void test() {
		ApplicationImpl application = new ApplicationImpl();
		application.setName("name");
		applicationRepository.save(application);

		RoleImpl applicationRole = new RoleImpl();
		applicationRole.setName("name");
		applicationRole.setApplication(application);
		applicationRoleRepository.save(applicationRole);
		application.getRoles().add(applicationRole);

		GroupImpl group = new GroupImpl();
		group.setName("name");
		group.getRoles().add(applicationRole);
		repository.save(group);

		Assert.assertEquals(group, repository.findByName(group.getName()));
		Assert.assertEquals(group, repository.findOne(group.getId()));
		Assert.assertEquals(group, repository.getGroup(group.getId()));
		Assert.assertEquals(Arrays.asList(1), repository.getGroupIdsForNames(Arrays.asList("name", "name2")));

		Assert.assertEquals(Arrays.asList(group), repository.findGroupsForApplicationRole(applicationRole.getId()));

		repository.delete(group);
		Assert.assertNull(repository.findOne(group.getId()));
	}
}
