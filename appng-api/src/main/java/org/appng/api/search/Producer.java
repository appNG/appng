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
package org.appng.api.search;

/**
 * A {@link Producer} creates a product via the {@link #put(Object)}, {@link #put(Object, long)} and
 * {@link #putWithTimeout(Object, long)} method and offers it to it's (unknown) {@link Consumer}s via the {@link #get()}
 * , {@link #get(long)} or {@link #getWithTimeout(long)}-method.<br/>
 * TODO MM this has nothing to do especially with searching! Design seems overloaded, use a shared single BlockingQueue
 * for communication between consumer and producer!
 * 
 * @author Matthias Müller
 * 
 * @param <E>
 *            the type produced by this {@link Producer}
 */
public class Producer<E> extends BlockingQueueAccessor<E> {

	protected Producer(int queueSize) {
		super(queueSize);
	}

	protected Producer() {
		super();
	}

}
