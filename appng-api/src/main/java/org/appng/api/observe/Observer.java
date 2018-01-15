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
package org.appng.api.observe;

import org.appng.api.observe.Observable.Event;

/**
 * An {@link Observer} gets notified when an {@link Observable} object fires an {@link Event}.
 * 
 * @author Matthias Müller
 * 
 * @param <T>
 *            the type to be observed
 */
public interface Observer<T extends Observable<T>> {

	/**
	 * Method to be called when an {@link Event} is fired from an {@link Observable}.
	 * 
	 * @param observable
	 *            the {@link Observable}
	 * @param event
	 *            the {@link Event}
	 */
	void onEvent(T observable, Event event);

}
