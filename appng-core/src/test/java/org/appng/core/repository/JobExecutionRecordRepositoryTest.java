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

import org.appng.core.domain.JobExecutionRecord;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(initializers = JobExecutionRecordRepositoryTest.class)
public class JobExecutionRecordRepositoryTest extends AbstractRepositoryTest {

	@Autowired
	JobExecutionRecordRepository repository;

	public void test() {
		String site = "site";

		JobExecutionRecord r1 = new JobExecutionRecord();
		r1.setSite(site);
		r1.setApplication("a1");
		r1.setJobName("j1");
		repository.save(r1);

		JobExecutionRecord r2 = new JobExecutionRecord();
		r2.setSite(site);
		r2.setApplication("a1");
		r2.setJobName("j2");
		repository.save(r2);

		Assert.assertEquals(Arrays.asList("a1"), repository.getDistinctApplications(site));
		Assert.assertEquals(Arrays.asList("j1", "j2"), repository.getDistinctJobNames(site));

	}
}
