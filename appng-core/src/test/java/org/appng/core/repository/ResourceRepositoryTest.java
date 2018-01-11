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

import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.ResourceImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = ResourceRepositoryTest.class)
public class ResourceRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	ResourceRepository repository;

	@Autowired
	ApplicationRepository applicationRepository;

	public void test() {
		ApplicationImpl application = new ApplicationImpl();
		application.setName("name");
		applicationRepository.save(application);

		ResourceImpl applicationResource = new ResourceImpl();
		applicationResource.setApplication(application);
		applicationResource.setName("name");
		applicationResource.setBytes("".getBytes());
		repository.save(applicationResource);

		Integer applicationId = application.getId();
		Assert.assertEquals(applicationResource, repository.findByNameAndApplicationId("name", applicationId));
		Assert.assertEquals(applicationResource, repository.findOne(applicationResource.getId()));
		repository.delete(applicationResource);
		Assert.assertNull(repository.findOne(applicationResource.getId()));
	}

}
