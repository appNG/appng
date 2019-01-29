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
package org.appng.search.indexer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class ParseTagsTest {

	private ParseTags parseTags = new ParseTags("appNG");

	@Test
	public void testFile() throws Exception {
		Map<String, StringBuilder> parsed = parseTags.parse(new File("pages/en/42.jsp"));
		Assert.assertEquals("The Hitchhiker's Guide to the Galaxy", parsed.get("title").toString());
		String contents = parsed.get("contents").toString();
		String expected = "The Hitchhiker's Guide to the Galaxy is a comic science fiction series";
		Assert.assertTrue(contents.startsWith(expected));
	}

	@Test
	public void testNestedTags() throws Exception {
		String in = "<a:searchable index=\"true\" field=\"contents\">"
				+ "<a:searchable index=\"true\" field=\"field1\"> A </a:searchable>"
				+ "<a:searchable index=\"true\" field=\"field2\"> B </a:searchable>"
				+ "<a:searchable index=\"false\"> C </a:searchable>" + " D </a:searchable>";
		Map<String, StringBuilder> parsed = new ParseTags("a").parse(new ByteArrayInputStream(in.getBytes()));
		System.out.println(parsed);
		Assert.assertEquals("A", parsed.get("field1").toString());
		Assert.assertEquals("B", parsed.get("field2").toString());
		Assert.assertNull(parsed.get("field3"));
		Assert.assertEquals("A B D", parsed.get("contents").toString());
	}

	@Test
	public void testNotIndexed() throws Exception {
		String in = "<html><head></head><appNG:searchable index=\"false\">"
				+ "<appNG:searchable index=\"true\" field=\"field1\"> A </appNG:searchable>"
				+ "<appNG:searchable index=\"true\" field=\"field2\"> B </appNG:searchable>"
				+ "<appNG:searchable index=\"false\"> C </appNG:searchable>" + " D </appNG:searchable></html>";
		Map<String, StringBuilder> parsed = parseTags.parse(new ByteArrayInputStream(in.getBytes()));
		System.out.println(parsed);
		Assert.assertNull(parsed.get("field1"));
		Assert.assertNull(parsed.get("field2"));
		Assert.assertNull(parsed.get("field3"));
		Assert.assertNull(parsed.get("contents"));
	}

}
