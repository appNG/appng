/*
 * Copyright 2011-2020 the original author or authors.
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

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;

public class SubjectImplTest {

	@Test
	public void testIsLocked() {
		SubjectImpl s = new SubjectImpl();
		Date now = new Date();
		Assert.assertFalse(s.isLocked(now));
		s.setLockedAfterDate(now);
		Assert.assertFalse(s.isLocked(now));
		Assert.assertFalse(s.isLocked(DateUtils.addMinutes(now, -1)));

		Assert.assertTrue(s.isLocked(DateUtils.addMilliseconds(now, 1)));
		Assert.assertTrue(s.isLocked(DateUtils.addSeconds(now, 1)));
		Assert.assertTrue(s.isLocked(DateUtils.addMinutes(now, 1)));
	}

	@Test
	public void testIsInactive() {
		SubjectImpl s = new SubjectImpl();
		Date now = new Date();

		s.setLastLogin(now);
		int inactiveLockPeriod = 10;
		Assert.assertFalse(s.isInactive(now,inactiveLockPeriod));

		Assert.assertFalse(s.isInactive(now,inactiveLockPeriod));

		Assert.assertFalse(s.isInactive(now,inactiveLockPeriod));

		Date plusTenDays = DateUtils.addDays(now, inactiveLockPeriod);
		Assert.assertFalse(s.isInactive(plusTenDays,inactiveLockPeriod));

		Assert.assertTrue(s.isInactive(DateUtils.addMilliseconds(plusTenDays, 1),inactiveLockPeriod));
		Assert.assertTrue(s.isInactive(DateUtils.addDays(now, 11),inactiveLockPeriod));
	}

}
