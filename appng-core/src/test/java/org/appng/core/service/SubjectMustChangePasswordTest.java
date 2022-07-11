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
package org.appng.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.AuthSubject.PasswordChangePolicy;
import org.appng.api.model.Properties;
import org.appng.api.model.UserType;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.domain.SubjectImpl;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = BusinessException.class)
@Rollback(false)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PlatformTestConfig.class, initializers = TestInitializer.class)
@DirtiesContext
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SubjectMustChangePasswordTest {

	@Autowired
	private CoreService coreService;

	@Mock
	private DefaultEnvironment environment;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		java.util.Properties defaultOverrides = new java.util.Properties();
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + Platform.Property.PASSWORD_MAX_VALIDITY, "3");
		defaultOverrides.put(PropertySupport.PREFIX_PLATFORM + Platform.Property.FORCE_CHANGE_PASSWORD, "true");
		Properties platformConfig = coreService.initPlatformConfig(defaultOverrides, "target/ROOT", false, true, false);
		Mockito.when(environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG))
				.thenReturn(platformConfig);

	}

	@Test
	public void testMustChangePassword() {
		SubjectImpl subject = new SubjectImpl();
		subject.setName("shady");
		subject.setRealname("The r€@l Sim Shady");
		subject.setEmail("slim@shady.org");
		subject.setUserType(UserType.LOCAL_USER);
		subject.setLanguage("en");
		String password = "tester";
		subject.setDigest(new BCryptPasswordEncoder(4).encode(password));
		coreService.updateSubject(subject);

		boolean success = coreService.login(null, environment, subject.getName(), password);
		assertTrue(success);
		subject = coreService.getSubjectByName(subject.getName(), false);
		assertEquals(PasswordChangePolicy.MAY, subject.getPasswordChangePolicy());

		subject.setPasswordLastChanged(DateUtils.addDays(new Date(), -2));
		coreService.updateSubject(subject);
		success = coreService.login(null, environment, subject.getName(), password);
		assertTrue(success);
		subject = coreService.getSubjectByName(subject.getName(), false);
		assertEquals(PasswordChangePolicy.MAY, subject.getPasswordChangePolicy());

		subject.setPasswordLastChanged(DateUtils.addDays(new Date(), -3));
		coreService.updateSubject(subject);
		success = coreService.login(null, environment, subject.getName(), password);
		assertTrue(success);
		subject = coreService.getSubjectByName(subject.getName(), false);
		assertEquals(PasswordChangePolicy.MUST, subject.getPasswordChangePolicy());
	}

}
