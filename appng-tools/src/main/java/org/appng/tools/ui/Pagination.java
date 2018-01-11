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
package org.appng.tools.ui;

public class Pagination {

	protected int chunk;
	protected String chunkname;
	protected int chunksize;
	protected int nextchunk;
	protected int previouschunk;
	protected int firstchunk;
	protected int lastchunk;
	protected int hits;
	protected int maxhits;
	protected int hitRangeStart;
	protected int hitRangeEnd;

	public Pagination(Chunk chunkParam) {
		chunksize = chunkParam.getChunkSize();
		chunk = chunkParam.getChunk();
		chunkname = chunkParam.getChunkName();
	}

	public static final Pagination getPagination(int hits, int maxHits, Chunk chunkParam) {
		Pagination pagination = new Pagination(chunkParam);
		pagination.calculatePagination(hits, maxHits);
		return pagination;
	}

	public void calculatePagination(int hits, int maxHits) {

		int firstchunk = 0;

		if (chunksize == 0) {
			chunksize = hits;
		}
		int lastchunk = getLastChunk(hits, chunksize);

		if (hits <= chunksize) {
			lastchunk = firstchunk;
		}

		if ((chunk < firstchunk) || (chunk > lastchunk)) {
			chunk = firstchunk;
		}

		int nextchunk = lastchunk;
		if (chunk < lastchunk) {
			nextchunk = chunk + 1;
		}

		int previouschunk = 0;
		if (chunk > 0) {
			previouschunk = chunk - 1;
		}

		if ((chunkname == null) || (chunkname.equals(""))) {
			chunkname = "chunk";
		}

		int hitRangeStart = chunk * chunksize;
		int hitRangeEnd = hitRangeStart - 1 + chunksize;
		if (hitRangeEnd >= hits) {
			hitRangeEnd = hits - 1;
		}

		setChunk(chunk);
		setChunkname(chunkname);
		setChunksize(chunksize);

		setHitRangeStart(hitRangeStart);
		setHitRangeEnd(hitRangeEnd);

		setFirstchunk(firstchunk);
		setLastchunk(lastchunk);
		setPreviouschunk(previouschunk);
		setNextchunk(nextchunk);

		setHits(hits);
		setMaxhits(maxHits);

	}

	public int getChunk() {
		return chunk;
	}

	public void setChunk(int chunk) {
		this.chunk = chunk;
	}

	public String getChunkname() {
		return chunkname;
	}

	public void setChunkname(String chunkname) {
		this.chunkname = chunkname;
	}

	public int getChunksize() {
		return chunksize;
	}

	public void setChunksize(int chunksize) {
		this.chunksize = chunksize;
	}

	public int getNextchunk() {
		return nextchunk;
	}

	public void setNextchunk(int nextchunk) {
		this.nextchunk = nextchunk;
	}

	public int getPreviouschunk() {
		return previouschunk;
	}

	public void setPreviouschunk(int previouschunk) {
		this.previouschunk = previouschunk;
	}

	public int getFirstchunk() {
		return firstchunk;
	}

	public void setFirstchunk(int firstchunk) {
		this.firstchunk = firstchunk;
	}

	public int getLastchunk() {
		return lastchunk;
	}

	public void setLastchunk(int lastchunk) {
		this.lastchunk = lastchunk;
	}

	public int getHits() {
		return hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
	}

	public int getMaxhits() {
		return maxhits;
	}

	public void setMaxhits(int maxhits) {
		this.maxhits = maxhits;
	}

	public int getHitRangeStart() {
		return hitRangeStart;
	}

	public void setHitRangeStart(int hitRangeStart) {
		this.hitRangeStart = hitRangeStart;
	}

	public int getHitRangeEnd() {
		return hitRangeEnd;
	}

	public void setHitRangeEnd(int hitRangeEnd) {
		this.hitRangeEnd = hitRangeEnd;
	}

	private static int getLastChunk(int hits, int chunksize) {
		if (chunksize > 0) {
			return (hits / chunksize + ((hits % chunksize == 0) ? 0 : 1) - 1);
		}
		return 0;
	}
}
