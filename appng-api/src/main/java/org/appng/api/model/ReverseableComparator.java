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
package org.appng.api.model;

import java.util.Comparator;

public abstract class ReverseableComparator<T> implements Comparator<T> {

	private int direction = 1;

	ReverseableComparator(Comparator<T> comparator) {
		this(false);
	}

	ReverseableComparator(boolean reverse) {
		if (reverse) {
			this.direction = -1;
		}
	}

	public final int compare(T o1, T o2) {
		int result = compareInner(o1, o2);
		return result * direction;
	}

	public abstract int compareInner(T o1, T o2);

}
