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
 * A {@link Consumer} is aware of several {@link Producer}s which can be added via the {@link #put(Object)},
 * {@link #put(Object, long)} and {@link #putWithTimeout(Object, long)} method. It can access it's queued
 * {@link Producer}s with the corresponding {@link #get()}, {@link #get(long)} and {@link #getBlockingQueue()}
 * method.<br/>
 * TODO MM this has nothing to do especially with searching! Design seems overloaded, use a shared single BlockingQueue
 * for communication between consumer and producer!
 * 
 * @author Matthias Müller
 * 
 * @param <E>
 *            the type consumed by this {@link Consumer} (and produced by a {@link Producer} via {@link #put(Object)},
 *            {@link #put(Object, long)} or {@link #putWithTimeout(Object, long)})
 * @param <P>
 *            the type of the {@link Producer} which must produce object of type {@code <E>}
 */
public abstract class Consumer<E, P extends Producer<E>> extends BlockingQueueAccessor<P> {

	protected Consumer(int queueSize) {
		super(queueSize);
	}

	protected Consumer() {
		super();
	}

}
