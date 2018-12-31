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

import java.io.File;

import org.appng.api.BusinessException;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.PackageWrapper;
import org.appng.core.model.RepositoryCache;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;
import org.appng.core.xml.repository.PackageType;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryCacheTest {

	@Test
	public void testCache() throws BusinessException {
		RepositoryImpl repository = new RepositoryImpl();
		repository.setRepositoryType(RepositoryType.LOCAL);
		repository.setActive(true);
		repository.setRepositoryMode(RepositoryMode.ALL);
		repository.setName("local");
		repository.setStrict(false);
		repository.setUri(new File("src/test/resources/zip").toURI());
		RepositoryCacheFactory.init(null, null, null, null, false);
		RepositoryCache cache = RepositoryCacheFactory.instance().getCache(repository);

		Assert.assertEquals(1, cache.getApplications().size());
		Assert.assertEquals(1, cache.getApplications().size());
		Assert.assertEquals(1, cache.getApplications(null).size());
		Assert.assertEquals(1, cache.getApplications("").size());
		Assert.assertEquals(1, cache.getApplications("demo-").size());
		Assert.assertEquals(1, cache.getApplications("*").size());
		Assert.assertEquals(1, cache.getApplications("demo-*").size());
		Assert.assertEquals(0, cache.getApplications("notfound").size());

		PackageWrapper applicationWrapper = cache.getPublishedApplicationWrapper("demo-application");
		String latest = "1.5.4";
		Assert.assertEquals(latest, applicationWrapper.getLatestRelease());
		Assert.assertNull(applicationWrapper.getLatestSnapshot());

		String v1_5_3 = "1.5.3";
		PackageArchive archive = cache.getApplicationArchive(applicationWrapper.getName(), v1_5_3, null);
		Assert.assertEquals(PackageType.APPLICATION, archive.getType());
		Assert.assertEquals("2013-01-13-1303", archive.getPackageInfo().getTimestamp());
		Assert.assertEquals(v1_5_3, archive.getPackageInfo().getVersion());

		String v1_5_2 = "1.5.2";
		archive = cache.getApplicationArchive(applicationWrapper.getName(), v1_5_2, null);
		Assert.assertEquals(PackageType.APPLICATION, archive.getType());
		Assert.assertEquals("2012-11-27-1305", archive.getPackageInfo().getTimestamp());
		Assert.assertEquals(v1_5_2, archive.getPackageInfo().getVersion());

		String v1_5_1 = "1.5.1";
		archive = cache.getApplicationArchive(applicationWrapper.getName(), v1_5_1, null);
		Assert.assertEquals(PackageType.APPLICATION, archive.getType());
		Assert.assertEquals("2012-08-03-1408", archive.getPackageInfo().getTimestamp());
		Assert.assertEquals(v1_5_1, archive.getPackageInfo().getVersion());
	}

}
