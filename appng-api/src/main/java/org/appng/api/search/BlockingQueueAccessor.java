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
package org.appng.api.search;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link BlockingQueue}-based implementation for consumer-producer-scenarios.<br/>
 * <b>Note that all available operations are blocking!</b>
 * 
 * @author Matthias Müller
 * 
 * @see Producer
 * @see Consumer
 */
public abstract class BlockingQueueAccessor<E> {

	private static final int DEFAULT_QUEUE_SIZE = 100;

	private final BlockingQueue<E> queue;

	/**
	 * Creates a new {@code BlockingQueueAccessor} with the given queue size.
	 * 
	 * @param queueSize
	 *            the size of the internal {@link BlockingQueue}
	 */
	protected BlockingQueueAccessor(int queueSize) {
		queue = new ArrayBlockingQueue<E>(queueSize);
	}

	/**
	 * Creates a new {@code BlockingQueueAccessor} with a queue size of 100}.
	 */
	protected BlockingQueueAccessor() {
		this(DEFAULT_QUEUE_SIZE);
	}

	/**
	 * Puts the given {@code element} into the queue.<br/>
	 * <b>This is a blocking operation!</b>
	 * 
	 * @param element
	 *            the element of type {@code <E>} to put to the queue
	 * @throws InterruptedException
	 *             if interrupted while waiting
	 */
	public void put(E element) throws InterruptedException {
		queue.put(element);
	}

	/**
	 * Puts the given {@code element} into the queue, waiting up to {@code timeoutMillis} milliseconds if necessary.<br/>
	 * <b>This is a blocking operation!</b>
	 * 
	 * @param element
	 *            the element of type {@code <E>} to put into the queue
	 * @param timeoutMillis
	 *            the time to wait in milliseconds
	 * @return {@code true} if the element could be added before the timeout, {@code false} otherwise
	 * @throws InterruptedException
	 *             if interrupted while waiting
	 */
	public boolean put(E element, long timeoutMillis) throws InterruptedException {
		return queue.offer(element, timeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * Puts the given {@code element} into the queue, waiting up to {@code timeoutMillis} milliseconds if necessary. If
	 * no element could be added, a {@link TimeoutException} is thrown.<br/>
	 * <b>This is a blocking operation!</b>
	 * 
	 * @param element
	 *            the element of type {@code <E>} to put to the queue
	 * @param timeoutMillis
	 *            the time to wait in milliseconds
	 * @throws InterruptedException
	 *             if interrupted while waiting
	 * @throws TimeoutException
	 *             if a timeout occurred while waiting
	 */
	public void putWithTimeout(E element, long timeoutMillis) throws InterruptedException, TimeoutException {
		boolean offered = put(element, timeoutMillis);
		if (!offered) {
			throw new TimeoutException(element + " could not be added after " + timeoutMillis + " "
					+ TimeUnit.MILLISECONDS.name().toLowerCase() + ", queue limit exceeded!");
		}
	}

	/**
	 * Retrieves and removes the head of the queue.<br/>
	 * <b>This is a blocking operation!</b>
	 * 
	 * @return an element of type {@code <E>}
	 * @throws InterruptedException
	 *             if interrupted while waiting
	 */
	public E get() throws InterruptedException {
		return queue.take();
	}

	/**
	 * Retrieves and removes the head of the queue, waiting up to {@code timeoutMillis} milliseconds if necessary. If no
	 * element could be retrieved, {@code null} is returned.<br/>
	 * <b>This is a blocking operation!</b>
	 * 
	 * @param timeoutMillis
	 *            the time to wait in milliseconds
	 * @return an element of type {@code <E>}, or {@code null} if no element could be retrieved in {@code timeoutMillis}
	 *         milliseconds
	 * @throws InterruptedException
	 *             if interrupted while waiting
	 */
	public E get(long timeoutMillis) throws InterruptedException {
		return queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * Retrieves and removes the head of the queue, waiting up to {@code timeoutMillis} milliseconds if necessary. If no
	 * element could be retrieved, a {@link TimeoutException} is thrown.<br/>
	 * <b>This is a blocking operation!</b>
	 * 
	 * @param timeoutMillis
	 *            the time to wait in milliseconds
	 * @return an element of type {@code <E>}
	 * @throws InterruptedException
	 *             if interrupted while waiting
	 * @throws TimeoutException
	 *             if a timeout occurred while waiting
	 */
	public E getWithTimeout(long timeoutMillis) throws InterruptedException, TimeoutException {
		E element = get(timeoutMillis);
		if (null == element) {
			ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
			Class<?> itemType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
			throw new TimeoutException("no " + itemType.getName() + " returned after " + timeoutMillis + " "
					+ TimeUnit.MILLISECONDS.name().toLowerCase());
		}
		return element;
	}

	protected BlockingQueue<E> getBlockingQueue() {
		return queue;
	}
}
