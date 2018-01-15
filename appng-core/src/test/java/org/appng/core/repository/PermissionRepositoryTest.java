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

import java.util.Arrays;

import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.PermissionImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = PermissionRepositoryTest.class)
public class PermissionRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	PermissionRepository repository;

	@Autowired
	ApplicationRepository applicationRepository;

	public void test() {
		ApplicationImpl application = new ApplicationImpl();
		application.setName("name");
		applicationRepository.save(application);

		PermissionImpl permission = new PermissionImpl();
		permission.setName("name");
		permission.setApplication(application);
		repository.save(permission);

		Integer applicationId = application.getId();
		Assert.assertEquals(Arrays.asList(permission),
				repository.findByApplicationId(applicationId, new Sort(Direction.ASC, "name")));
		Assert.assertEquals(permission, repository.findByNameAndApplicationId("name", applicationId));
		Assert.assertEquals(permission, repository.findOne(permission.getId()));
		repository.delete(permission);
		Assert.assertNull(repository.findOne(permission.getId()));
	}

}
