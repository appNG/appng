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
package org.appng.core.domain;

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.model.Application;
import org.junit.Assert;
import org.junit.Test;

public class SubjectImplTest {

	@Test
	public void testIsLocked() {
		SubjectImpl s = new SubjectImpl();
		Date now = new Date();
		Assert.assertFalse(s.isExpired(now));
		s.setExpiryDate(now);
		Assert.assertFalse(s.isExpired(now));
		Assert.assertFalse(s.isExpired(DateUtils.addMinutes(now, -1)));

		Assert.assertTrue(s.isExpired(DateUtils.addMilliseconds(now, 1)));
		Assert.assertTrue(s.isExpired(DateUtils.addSeconds(now, 1)));
		Assert.assertTrue(s.isExpired(DateUtils.addMinutes(now, 1)));
	}

	@Test
	public void testIsInactive() {
		SubjectImpl s = new SubjectImpl();
		Date now = new Date();

		s.setLastLogin(now);
		int inactiveLockPeriod = 10;
		Assert.assertFalse(s.isInactive(now, inactiveLockPeriod));

		Assert.assertFalse(s.isInactive(now, inactiveLockPeriod));

		Assert.assertFalse(s.isInactive(now, inactiveLockPeriod));

		Date plusTenDays = DateUtils.addDays(now, inactiveLockPeriod);
		Assert.assertFalse(s.isInactive(plusTenDays, inactiveLockPeriod));

		Assert.assertTrue(s.isInactive(DateUtils.addMilliseconds(plusTenDays, 1), inactiveLockPeriod));
		Assert.assertTrue(s.isInactive(DateUtils.addDays(now, 11), inactiveLockPeriod));
	}

	@Test
	public void testHasApplication() {
		SubjectImpl s = new SubjectImpl();
		Assert.assertFalse(s.hasApplication(null));
		s.setGroups(null);
		ApplicationImpl app = new ApplicationImpl();
		Assert.assertFalse(s.hasApplication(app));
		GroupImpl group = new GroupImpl();
		s.setGroups(Arrays.asList(group));
		Assert.assertFalse(s.hasApplication(app));
		RoleImpl r = new RoleImpl();
		r.setId(11);
		r.setApplication(app);
		app.getRoles().add(r);
		group.getRoles().add(r);
		Assert.assertTrue(s.hasApplication(app));
		app.setRoles(null);
		Assert.assertFalse(s.hasApplication(app));
		group.setRoles(null);
		Assert.assertFalse(s.hasApplication(app));
	}

	@Test
	public void testGetApplicationRoles() {
		SubjectImpl s = new SubjectImpl();
		Assert.assertTrue(s.getApplicationRoles(null).isEmpty());

		Application app1 = new ApplicationImpl();
		Application app2 = new ApplicationImpl();
		GroupImpl g = new GroupImpl();
		s.getGroups().add(g);
		RoleImpl r1 = new RoleImpl();
		r1.setId(11);
		r1.setApplication(app1);
		app1.getRoles().add(r1);
		RoleImpl r2 = new RoleImpl();
		r2.setId(12);
		r2.setApplication(app2);
		app2.getRoles().add(r2);
		g.getRoles().addAll(Arrays.asList(r1, r2));
		Assert.assertTrue(s.getApplicationRoles(app1).contains(r1));
		Assert.assertTrue(s.getApplicationRoles(app2).contains(r2));
		Assert.assertFalse(s.getApplicationRoles(app1).contains(r2));
		Assert.assertFalse(s.getApplicationRoles(app2).contains(r1));

	}

}
