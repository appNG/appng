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
package org.appng.api.observe;

import java.io.Serializable;

/**
 * 
 * An Observable can be observed by multiple {@link Observer}s and notify them of changes by firing an {@link Event} .
 * 
 * @author Matthias Müller
 * 
 * @param <T>
 *            the type which is observed
 */
public interface Observable<T> {

	/**
	 * An {@link Observable} notifies it's {@link Observer}s by firing an {@link Event}.
	 * 
	 * @author Matthias Müller
	 * 
	 */
	class Event implements Serializable {
		private final String name;

		/**
		 * Creates a new {@link Event}
		 * 
		 * @param name
		 *            the name of the {@link Event}
		 */
		public Event(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name.toUpperCase();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Event other = (Event) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}

	}

	/**
	 * Adds an {@link Observer}.
	 * 
	 * @param observer
	 *            the {@link Observer} to be added
	 * @return {@code true} if adding the {@link Observer} was successful, {@code false} otherwise
	 */
	boolean addObserver(Observer<? super T> observer);

	/**
	 * Removes an {@link Observer}.
	 * 
	 * @param observer
	 *            the {@link Observer} to be removed
	 * @return {@code true} if removing the {@link Observer} was successful, {@code false} otherwise
	 */
	boolean removeObserver(Observer<? super T> observer);

	/**
	 * Notifies the {@link Observer}s.
	 * 
	 * @param event
	 *            an {@link Event}
	 */
	void notifyObservers(Event event);

}
