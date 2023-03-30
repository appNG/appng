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
package org.appng.core.security;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

public class SaltedDigestTest {

	private static final String COMPLEX_SECRET = "wiCked p@$sw0rD";
	private static final String SIMPLE_SECRET = "a";
	private static final int ITERATIONS = 500;

	@Test
	public void testGetSha1Digest() {
		SaltedDigest saltedDigest = new SaltedDigestSha1();
		testGetDigest(SIMPLE_SECRET, "JzmK5goBUdo8x9iGZ9CSorSGu+g=", saltedDigest);
		testGetDigest(COMPLEX_SECRET, "rpghkZf6sEmQvF3trUF8KM6jrxU=", saltedDigest);
	}

	private void testGetDigest(String secret, String expected, SaltedDigest saltedDigest) {
		String salt = "QJlqAdqV4kA=";
		String digest = saltedDigest.getDigest(secret, salt);
		Assert.assertEquals(expected, digest);
	}

	@Test
	public void testGetRandomDigestSha1() {
		testGetRandomDigest(SIMPLE_SECRET, new SaltedDigestSha1());
		testGetRandomDigest(COMPLEX_SECRET, new SaltedDigestSha1());
	}

	private void testGetRandomDigest(String secret, SaltedDigest saltedDigest) {
		Collection<String> randomDigests = new HashSet<>(ITERATIONS);
		Collection<String> randomSalts = new HashSet<>(ITERATIONS);
		for (int i = 0; i < ITERATIONS; i++) {
			String salt = saltedDigest.getSalt();
			String digest = saltedDigest.getDigest(secret, salt);
			if (randomDigests.contains(digest)) {
				Assert.fail("Random string generation failed. Digest already exists: " + digest);
			} else if (randomSalts.contains(salt)) {
				Assert.fail("Random string generation failed. Salt already exists: " + salt);
			} else {
				randomDigests.add(digest);
				randomSalts.add(salt);
			}
		}
		assertTrue(true);
	}

}
