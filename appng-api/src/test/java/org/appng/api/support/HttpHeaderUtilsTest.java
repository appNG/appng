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
package org.appng.api.support;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class HttpHeaderUtilsTest {

	private static final String TEXT_PLAIN = "text/plain";
	private static final String DATE = "Wed, 22 Jun 1910 12:14:00 GMT";
	private static final String TEST = "test";
	private static final byte[] BYTES = TEST.getBytes();

	@Test
	public void testNotModified() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, HttpHeaderUtils.HTTP_DATE.format(new Date()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpHeaderUtils.HttpResource resource = getResource();
		Assert.assertFalse(resource.needsUpdate());
		Assert.assertNull(resource.getData());

		HttpHeaderUtils.handleModifiedHeaders(request, response, resource, true);

		Assert.assertTrue(resource.needsUpdate());
		Assert.assertArrayEquals(BYTES, resource.getData());
		Assert.assertNull(response.getHeader(HttpHeaders.LAST_MODIFIED));
		Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
		Assert.assertNull(response.getContentType());
		Assert.assertEquals(0, response.getContentLength());
		Assert.assertEquals("", response.getContentAsString());
	}

	@Test
	public void testOK() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpHeaderUtils.HttpResource resource = getResource();
		Assert.assertFalse(resource.needsUpdate());
		Assert.assertNull(resource.getData());

		HttpHeaderUtils.handleModifiedHeaders(request, response, resource, true);

		validateResponseOK(response, resource);
	}

	@Test
	public void testInvalid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "not a date");
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpHeaderUtils.HttpResource resource = getResource();
		Assert.assertFalse(resource.needsUpdate());
		Assert.assertNull(resource.getData());

		HttpHeaderUtils.handleModifiedHeaders(request, response, resource, true);

		validateResponseOK(response, resource);
	}

	protected void validateResponseOK(MockHttpServletResponse response, HttpHeaderUtils.HttpResource resource)
			throws IOException, UnsupportedEncodingException {
		Assert.assertTrue(resource.needsUpdate());
		Assert.assertArrayEquals(BYTES, resource.getData());
		Assert.assertEquals(DATE, response.getHeader(HttpHeaders.LAST_MODIFIED));
		Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Assert.assertEquals(TEXT_PLAIN, response.getContentType());
		Assert.assertEquals(4, response.getContentLength());
		Assert.assertEquals(TEST, response.getContentAsString());
	}

	protected HttpHeaderUtils.HttpResource getResource() throws ParseException {
		final long lastModified = HttpHeaderUtils.HTTP_DATE.parse(DATE).getTime();
		return new HttpHeaderUtils.HttpResource() {
			boolean updated = false;
			byte[] data;

			public long update() throws IOException {
				updated = true;
				data = BYTES;
				return lastModified;
			}

			public boolean needsUpdate() {
				return updated;
			}

			public byte[] getData() throws IOException {
				return data;
			}

			public String getContentType() {
				return TEXT_PLAIN;
			}
		};
	}

}
