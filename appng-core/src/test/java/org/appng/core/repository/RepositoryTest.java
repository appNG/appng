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

import org.appng.core.domain.ApplicationImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = RepositoryTest.class)
public class RepositoryTest extends AbstractRepositoryTest {

	@Autowired
	ApplicationRepository repository;

	public void test() {
		ApplicationImpl application = new ApplicationImpl();
		application.setName("name");
		repository.save(application);

		Assert.assertEquals(application, repository.findByName(application.getName()));
		Assert.assertEquals(application, repository.findOne(application.getId()));
		repository.delete(application);
		Assert.assertNull(repository.findOne(application.getId()));
	}

}
