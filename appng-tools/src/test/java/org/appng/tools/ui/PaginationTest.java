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
package org.appng.tools.ui;

import org.junit.Assert;
import org.junit.Test;

public class PaginationTest {

	@Test
	public void testEmpty() {
		Pagination pagination = new Pagination(new Chunk(10, 0, "chunk"));
		pagination.calculatePagination(0, 1000);
		Assert.assertEquals("chunk", pagination.getChunkname());
		Assert.assertEquals(0, pagination.getChunk());
		Assert.assertEquals(10, pagination.getChunksize());
		Assert.assertEquals(0, pagination.getFirstchunk());
		Assert.assertEquals(0, pagination.getLastchunk());
		Assert.assertEquals(0, pagination.getPreviouschunk());
		Assert.assertEquals(0, pagination.getNextchunk());
		Assert.assertEquals(0, pagination.getHitRangeStart());
		Assert.assertEquals(-1, pagination.getHitRangeEnd());
		Assert.assertEquals(0, pagination.getHits());
		Assert.assertEquals(1000, pagination.getMaxhits());
	}

	@Test
	public void test() {
		Pagination pagination = new Pagination(new Chunk(10, 2, "chunk"));
		pagination.calculatePagination(80, 1000);
		Assert.assertEquals("chunk", pagination.getChunkname());
		Assert.assertEquals(2, pagination.getChunk());
		Assert.assertEquals(10, pagination.getChunksize());
		Assert.assertEquals(0, pagination.getFirstchunk());
		Assert.assertEquals(7, pagination.getLastchunk());
		Assert.assertEquals(1, pagination.getPreviouschunk());
		Assert.assertEquals(3, pagination.getNextchunk());
		Assert.assertEquals(20, pagination.getHitRangeStart());
		Assert.assertEquals(29, pagination.getHitRangeEnd());
		Assert.assertEquals(80, pagination.getHits());
		Assert.assertEquals(1000, pagination.getMaxhits());
	}

	@Test
	public void testZeroChunk() {
		Pagination pagination = new Pagination(new Chunk(0, 2, "chunk"));
		pagination.calculatePagination(80, 1000);
		Assert.assertEquals("chunk", pagination.getChunkname());
		Assert.assertEquals(0, pagination.getChunk());
		Assert.assertEquals(80, pagination.getChunksize());
		Assert.assertEquals(0, pagination.getFirstchunk());
		Assert.assertEquals(0, pagination.getLastchunk());
		Assert.assertEquals(0, pagination.getPreviouschunk());
		Assert.assertEquals(0, pagination.getNextchunk());
		Assert.assertEquals(0, pagination.getHitRangeStart());
		Assert.assertEquals(79, pagination.getHitRangeEnd());
		Assert.assertEquals(80, pagination.getHits());
		Assert.assertEquals(1000, pagination.getMaxhits());
	}

	@Test
	public void testNegativeChunk() {
		Pagination pagination = Pagination.getPagination(80, 1000, new Chunk(10, -1, "chunk"));
		Assert.assertEquals(0, pagination.getChunk());
	}
}
