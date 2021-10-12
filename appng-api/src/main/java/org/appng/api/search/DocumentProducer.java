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
package org.appng.api.search;

import org.apache.lucene.analysis.Analyzer;

/**
 * A {@link Producer} that produces {@link DocumentEvent}s. TODO appng-search is the better place for this
 * 
 * @author Matthias Müller
 * 
 * @see Producer
 */
public class DocumentProducer extends Producer<DocumentEvent> {

	private final Class<? extends Analyzer> analyzerClass;
	private String name;

	/**
	 * Creates a new {@link DocumentProducer}
	 * 
	 * @param queueSize
	 *                      the queue size for this producer
	 * @param analyzerClass
	 *                      the type of the {@link Analyzer} to use for indexing the document
	 * @param name
	 *                      the name for this producer
	 */
	public DocumentProducer(int queueSize, Class<? extends Analyzer> analyzerClass, String name) {
		super(queueSize);
		this.name = name;
		this.analyzerClass = analyzerClass;
	}

	/**
	 * Creates a new {@link DocumentProducer} with a queue size of 500
	 * 
	 * @param analyzerClass
	 *                      the type of the {@link Analyzer} to use for indexing the document
	 * @param name
	 *                      the name for this producer
	 */
	public DocumentProducer(Class<? extends Analyzer> analyzerClass, String name) {
		this(500, analyzerClass, name);
	}

	/**
	 * Returns the type of the {@link Analyzer} used by this {@link DocumentProducer}.
	 * 
	 * @return the type of the {@link Analyzer}
	 */
	public Class<? extends Analyzer> getAnalyzerClass() {
		return analyzerClass;
	}

	public String getName() {
		return name;
	}

}
