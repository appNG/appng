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
package org.appng.api.auth;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link AuthTools}.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class AuthToolsTest {

	private String testPattern;

	@Before
	public void setUp() {
		testPattern = "This is the pattern which will be used for testing! Some special characters: !\"§$%&/()=?";
	}

	@Test
	public void testBase64() throws IOException {
		String base64 = AuthTools.byteToBase64(testPattern.getBytes());
		String expected = "VGhpcyBpcyB0aGUgcGF0dGVybiB3aGljaCB3aWxsIGJlIHVzZWQgZm9yIHRlc3RpbmchIFNvbWUgc3BlY2lhbCBjaGFyYWN0ZXJzOiAhIsKnJCUmLygpPT8=";
		Assert.assertEquals(expected, base64);

		byte[] byteResult = AuthTools.base64ToByte(base64);
		String s = new String(byteResult);
		Assert.assertEquals(testPattern, s);
	}

	@Test
	public void testMd5Digest() {
		String md5Digest = AuthTools.getMd5Digest(testPattern);
		Assert.assertEquals("7C2AD50DBEA658E2F87DDE1609114237", md5Digest);
	}

	@Test
	public void testSha1Digest() {
		String sha1Digest = AuthTools.getSha1Digest(testPattern);
		Assert.assertEquals("746BDF044C80DD81336A522BF27D8C661947D3EF", sha1Digest);
	}

	@Test
	public void testSha512Digest() {
		String sha512Digest = AuthTools.getSha512Digest(testPattern);
		Assert.assertEquals(
				"AB9D85DC074D06B675DAEE7FA4A70C7D0BD8F9A284713DAA0E5689DAA9367DD10258E331D3494053B4F5A1084D7881455DB5AADB84BDFAF5638677ED1D1C4881",
				sha512Digest);
	}

}
