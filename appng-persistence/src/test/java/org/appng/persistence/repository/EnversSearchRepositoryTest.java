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
package org.appng.persistence.repository;

import java.util.Arrays;
import java.util.List;

import org.appng.persistence.model.EnversTestEntity;
import org.appng.testsupport.persistence.ConnectionHelper;
import org.appng.testsupport.persistence.HsqlServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.history.Revision;

public class EnversSearchRepositoryTest {

	private int hsqlPort;

	private TestEntityEnversRepo repo;

	private AnnotationConfigApplicationContext ctx;

	@Before
	public void setup() {
		this.hsqlPort = ConnectionHelper.getHsqlPort();
		HsqlServer.start(hsqlPort);

		ctx = new AnnotationConfigApplicationContext();
		ctx.register(EnversRepositoryConfiguration.class);
		ctx.refresh();
		repo = ctx.getBean(TestEntityEnversRepo.class);

		EnversTestEntity t1 = new EnversTestEntity();
		t1.setName("name1");
		t1.setIntegerValue(1);
		EnversTestEntity t2 = new EnversTestEntity();
		t2.setName("name2");
		t2.setIntegerValue(2);
		EnversTestEntity t3 = new EnversTestEntity();
		t3.setName("name3");
		t3.setIntegerValue(3);
		repo.save(Arrays.asList(t1, t2, t3));
	}

	@After
	public void tearDown() {
		ctx.close();
		HsqlServer.stop(hsqlPort);
	}

	@Test
	public void test() {
		List<EnversTestEntity> all = repo.findAll();
		Assert.assertTrue(all.size() > 0);
		Assert.assertNull(all.get(0).getRevision());
		Integer entityId = all.get(0).getId();
		Assert.assertEquals(1, repo.findRevisions(entityId).getContent().size());
		all.get(0).setName(all.get(0).getName() + "_Test");
		repo.save(all.get(0));
		List<Revision<Integer, EnversTestEntity>> entityRevisions = repo.findRevisions(entityId).getContent();
		Assert.assertEquals(2, entityRevisions.size());
		Assert.assertNotNull(entityRevisions.get(0).getMetadata());
		Assert.assertNotNull(repo.findRevision(entityId, entityRevisions.get(0).getRevisionNumber()));
		Assert.assertEquals(1, repo.findRevisions(all.get(1).getId()).getContent().size());
		Assert.assertEquals(entityRevisions.get(entityRevisions.size() - 1).getRevisionNumber(), repo
				.findLastChangeRevision(entityId).getRevisionNumber());
	}

	private void incrementVersion(Integer entityId) {
		EnversTestEntity entity = repo.findOne(entityId);
		entity.setIntegerValue(entity.getIntegerValue() + 1);
		repo.save(entity);
	}
}
