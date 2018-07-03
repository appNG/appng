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
package org.appng.api.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RestClientTest {

	@Test
	public void testCookies() {
		Map<String, String> cookies = new HashMap<>();
		cookies.put("foo", "bar");
		cookies.put("lore", "ipsum");
		RestClient restClient = new RestClient("foo", cookies);
		List<String> cookieList = restClient.getHeaders().get(HttpHeaders.COOKIE);
		Assert.assertTrue(cookieList.contains("foo=bar"));
		Assert.assertTrue(cookieList.contains("lore=ipsum"));
	}

	@Test
	public void testSetCookies() {
		RestClient restClient = new RestClient("foo");
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, "foo=bar");
		headers.add(HttpHeaders.SET_COOKIE, "lore=ipsum;");
		restClient.setCookies(new ResponseEntity<>(headers, HttpStatus.OK));
		Map<String, String> cookies = restClient.getCookies();
		Assert.assertEquals("bar", cookies.get("foo"));
		Assert.assertEquals("ipsum", cookies.get("lore"));
	}
}
