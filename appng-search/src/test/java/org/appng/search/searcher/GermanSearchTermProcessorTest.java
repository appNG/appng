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
package org.appng.search.searcher;

import org.junit.Assert;
import org.junit.Test;

public class GermanSearchTermProcessorTest {

	@Test
	public void test() {
		SearchTermProcessor searchTermProcessor = new GermanSearchTermProcessor();
		validate(searchTermProcessor, "käse", "(*kas OR kase*)");
		validate(searchTermProcessor, "käse scheiben", "(*kas OR kase*) AND (*scheib OR scheib*)");
		validate(searchTermProcessor, "tomaten", "(*tomat OR tomat*)");
		validate(searchTermProcessor, "tomaten dosen", "(*tomat OR tomat*) AND (*dos OR dos*)");
		validate(searchTermProcessor, "gemüse", "(*gemus OR gemuse*)");
		validate(searchTermProcessor, "gemüse brühe", "(*gemus OR gemuse*) AND (*bruh OR bruhe*)");
		validate(searchTermProcessor, "gemüse huhn", "(*gemus OR gemuse*) AND (*huhn OR huhn*)");
		validate(searchTermProcessor, "dinkel", "(*dinkel OR dinkel*)");
		validate(searchTermProcessor, "kassler", "(*kassl OR kassl*)");
		validate(searchTermProcessor, "karotten kartoffeln rindfleisch",
				"(*karott OR karott*) AND (*kartoffeln OR kartoffeln*) AND (*rindfleisch OR rindfleisch*)");
	}

	private void validate(SearchTermProcessor searchTermProcessor, String term, String expected) {
		String searchTerm = searchTermProcessor.getSearchTerm(term);
		System.out.println("'" + term + "' -> " + searchTerm);
		Assert.assertEquals(expected, searchTerm);
	}

}
