/*
 * Copyright 2011-2019 the original author or authors.
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
package org.appng.core.security;

import org.junit.Assert;
import org.junit.Test;

public class DigestValidatorTest {

	private String sharedSecret = "foobar";
	private String username = "admin";

	@Test
	public void test() {
		String digest = DigestUtil.getDigest(username, sharedSecret);
		boolean isValid = new DigestValidator(digest,60).validate(sharedSecret);
		Assert.assertTrue("digest must be valid", isValid);
	}

	@Test
	public void testInvalid() {
		String digest = DigestUtil.getDigest(username, sharedSecret);
		boolean isValid = new DigestValidator(digest).validate("jinfizz");
		Assert.assertFalse("digest must invalid", isValid);
	}

	@Test
	public void testInvalidByOffset() {
		String digest = DigestUtil.getDigest(username, sharedSecret);
		DigestValidator digestValidator = new DigestValidator(digest, 60);
		boolean isValid = digestValidator.validate(sharedSecret);
		Assert.assertTrue("digest must be valid", isValid);

		String timestamp= digestValidator.getTimestamp()+"|"+digestValidator.getUtcOffset().replace('+','-');
		String digestNoOffset = DigestUtil.getDigest(username, sharedSecret, timestamp);
		isValid = new DigestValidator(digestNoOffset, 60).validate(sharedSecret);
		Assert.assertFalse("digest must be invalid because of utc offset", isValid);
	}

}
