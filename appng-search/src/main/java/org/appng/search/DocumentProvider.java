/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.search;

import java.util.concurrent.TimeoutException;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.search.Document;
import org.appng.api.search.DocumentProducer;

/**
 * When building the global Lucene index for a {@link Site}, every {@link DocumentProvider} provided by the site's
 * {@link Application}s is able to add/remove some {@link Document}s to be indexed.
 * 
 * @author Matthias Müller
 * 
 * @see Document
 * @see DocumentProducer
 * @see SearchProvider
 */
public interface DocumentProvider {

	/**
	 * Returns some {@link DocumentProducer}s to take into account when building the {@link Site}'s global lucene index.
	 * 
	 * @param site
	 *            the current {@link Site}
	 * @param application
	 *            the current {@link Application}
	 * @return some {@link DocumentProducer}s
	 */
	Iterable<DocumentProducer> getDocumentProducers(Site site, Application application) throws InterruptedException,
			TimeoutException;

}
