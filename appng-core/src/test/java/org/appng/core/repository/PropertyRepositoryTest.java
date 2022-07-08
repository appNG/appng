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

import org.appng.core.domain.PropertyImpl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = PropertyRepositoryTest.class)
public class PropertyRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	PropertyRepository repository;

	public void test() {
		PropertyImpl property = new PropertyImpl();
		property.setName("foo.bar");
		property = repository.save(property);

		Assert.assertEquals(property, repository.findByName(property.getName()));
		Assert.assertEquals(property, repository.getOne(property.getId()));

		repository.delete(property);

		Assert.assertNull(repository.getOne(property.getId()));
	}
}
