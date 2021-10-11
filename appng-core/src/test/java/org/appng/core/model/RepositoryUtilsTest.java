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
package org.appng.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.appng.xml.application.ApplicationInfo;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryUtilsTest {

	@Test
	public void testIsSnapshot() {
		Assert.assertFalse(RepositoryUtils.isSnapshot("snapshot"));
		Assert.assertFalse(RepositoryUtils.isSnapshot("SNAPSHOT"));
		Assert.assertFalse(RepositoryUtils.isSnapshot("sNaPsHoT"));
		Assert.assertTrue(RepositoryUtils.isSnapshot(RepositoryUtils.SNAPSHOT));
	}

	@Test
	public void testGetDate() {
		ApplicationInfo app = new ApplicationInfo();
		app.setTimestamp("19700101-0100");
		Date date = RepositoryUtils.getDate(app);
		Assert.assertEquals(0, date.getTime());
	}

	@Test
	public void testSemverSort() {
		ApplicationInfo a = new ApplicationInfo();
		a.setVersion("1.14.1-SNAPSHOT");
		a.setTimestamp("20190826-1159");

		ApplicationInfo b = new ApplicationInfo();
		b.setVersion("1.14.0-SNAPSHOT");
		b.setTimestamp("20190826-1203");

		ApplicationInfo c = new ApplicationInfo();
		c.setVersion("1.14.0-SNAPSHOT");
		c.setTimestamp("20190826-1157");

		List<ApplicationInfo> arrayList = new ArrayList<>(Arrays.asList(c, b, a));
		Collections.sort(arrayList, RepositoryUtils.getVersionComparator());
		Assert.assertEquals(a.getTimestamp(), arrayList.get(0).getTimestamp());
		Assert.assertEquals(b.getTimestamp(), arrayList.get(1).getTimestamp());
		Assert.assertEquals(c.getTimestamp(), arrayList.get(2).getTimestamp());
	}

	@Test
	public void testIsNewer() {
		ApplicationInfo v1_0_0_a = new ApplicationInfo();
		v1_0_0_a.setVersion("1.0.0");
		v1_0_0_a.setTimestamp("20180111-0416");
		ApplicationInfo v1_0_0_b = new ApplicationInfo();
		v1_0_0_b.setVersion("1.0.0");
		v1_0_0_b.setTimestamp("20180111-1016");
		ApplicationInfo v1_0_0_snapshot = new ApplicationInfo();
		v1_0_0_snapshot.setVersion("1.0.0-SNAPSHOT");
		v1_0_0_snapshot.setTimestamp("20180111-1116");
		ApplicationInfo v1_1_0 = new ApplicationInfo();
		v1_1_0.setVersion("1.1.0");
		v1_1_0.setTimestamp("20180111-1416");
		ApplicationInfo v2_0_0 = new ApplicationInfo();
		v2_0_0.setVersion("2.0.0");
		v2_0_0.setTimestamp("20180111-1216");

		Assert.assertFalse(RepositoryUtils.isNewer(v1_0_0_snapshot, v1_0_0_b));
		Assert.assertFalse(RepositoryUtils.isNewer(v1_0_0_b, v1_0_0_b));
		Assert.assertFalse(RepositoryUtils.isNewer(v1_0_0_a, v1_0_0_a));

		Assert.assertFalse(RepositoryUtils.isNewer(v1_0_0_a, v1_0_0_b));

		Assert.assertFalse(RepositoryUtils.isNewer(v1_0_0_a, v2_0_0));
		Assert.assertFalse(RepositoryUtils.isNewer(v1_0_0_b, v2_0_0));
		Assert.assertFalse(RepositoryUtils.isNewer(v1_1_0, v2_0_0));

		Assert.assertTrue(RepositoryUtils.isNewer(v1_0_0_b, v1_0_0_a));
		Assert.assertTrue(RepositoryUtils.isNewer(v1_1_0, v1_0_0_a));
		Assert.assertTrue(RepositoryUtils.isNewer(v1_1_0, v1_0_0_b));
		Assert.assertTrue(RepositoryUtils.isNewer(v2_0_0, v1_0_0_a));
		Assert.assertTrue(RepositoryUtils.isNewer(v2_0_0, v1_0_0_b));
		Assert.assertTrue(RepositoryUtils.isNewer(v2_0_0, v1_1_0));

		ApplicationInfo v0_9_0 = new ApplicationInfo();
		v0_9_0.setVersion("0.9.0");
		ApplicationInfo v0_40_0 = new ApplicationInfo();
		v0_40_0.setVersion("0.40.0");

		Assert.assertTrue(RepositoryUtils.isNewer(v0_40_0, v0_9_0));
		Assert.assertTrue(RepositoryUtils.isNewer(v0_40_0, v0_9_0));
		Assert.assertTrue(RepositoryUtils.isNewer(v0_40_0, v0_9_0));
	}

	@Test
	public void testIsNewerNotSemVer() {
		ApplicationInfo v1_0 = new ApplicationInfo();
		v1_0.setVersion("1.0-SNAPSHOT");
		v1_0.setTimestamp("20180111-0416");

		ApplicationInfo v1_0_ = new ApplicationInfo();
		v1_0_.setVersion("1.0-SNAPSHOT");
		v1_0_.setTimestamp("20180111-0816");

		ApplicationInfo v1_1 = new ApplicationInfo();
		v1_1.setVersion("1.1-SNAPSHOT");
		v1_1.setTimestamp("20180111-1016");

		Assert.assertTrue(RepositoryUtils.isNewer(v1_0_, v1_0));
		Assert.assertFalse(RepositoryUtils.isNewer(v1_0, v1_0_));
		Assert.assertFalse(RepositoryUtils.isNewer(v1_0_, v1_1));
		Assert.assertTrue(RepositoryUtils.isNewer(v1_1, v1_0));
		Assert.assertFalse(RepositoryUtils.isNewer(v1_0, v1_1));
	}

}
