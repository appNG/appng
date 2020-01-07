/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.api.observe.impl;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.appng.api.observe.Observable;
import org.appng.api.observe.Observer;

/**
 * {@link Observable}-implementation which delegates to a given Observable.
 * 
 * @author Matthias Müller
 * 
 * @param <T>
 *            the type which is observed
 */
public class ObservableDelegate<T extends Observable<T>> implements Observable<T> {

	private Collection<Observer<? super T>> observers = new ConcurrentLinkedQueue<Observer<? super T>>();
	private T observable;

	/**
	 * Creates a new {@link ObservableDelegate} which delegates to the given {@link Observable}.
	 * 
	 * @param observable
	 *            the {@link Observable} to delegate to.
	 */
	public ObservableDelegate(T observable) {
		this.observable = observable;
	}

	protected ObservableDelegate() {

	}

	protected void setObeservable(T observable) {
		this.observable = observable;
	}

	public boolean addObserver(Observer<? super T> observer) {
		return observers.add(observer);
	}

	public boolean removeObserver(Observer<? super T> observer) {
		return observers.remove(observer);
	}

	public void notifyObservers(Event event) {
		for (Observer<? super T> observer : observers) {
			observer.onEvent(observable, event);
		}
	}

}
