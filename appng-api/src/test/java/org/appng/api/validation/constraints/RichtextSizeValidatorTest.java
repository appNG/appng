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
package org.appng.api.validation.constraints;

import org.appng.api.validation.contraints.RichtextSize;
import org.appng.api.validation.contraints.RichtextSizeValidator;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RichtextSizeValidatorTest {

	@Test
	public void testValidator() {
		RichtextSize mock = Mockito.mock(RichtextSize.class);
		Mockito.when(mock.min()).thenReturn(6L);
		Mockito.when(mock.max()).thenReturn(10L);

		RichtextSizeValidator validator = new RichtextSizeValidator();
		validator.initialize(mock);

		Assert.assertFalse(validator.isValid("short", null));
		Assert.assertFalse(validator.isValid("<h1>tooooo long</h1>", null));
		Assert.assertTrue(validator.isValid("<h1>Yaaay!!</h1>", null));
	}

}
