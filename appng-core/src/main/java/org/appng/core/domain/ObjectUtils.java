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
package org.appng.core.domain;

import org.appng.api.model.Identifiable;
import org.appng.api.model.Named;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ObjectUtils {

	public static <T extends Named> boolean equals(T a, Object b) {
		if (a == b)
			return true;
		if (b == null)
			return false;
		if (!a.getClass().isAssignableFrom(b.getClass()))
			return false;
		T other = (T) b;
		if (a.getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!a.getId().equals(other.getId()))
			return false;
		if (a.getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!a.getName().equals(other.getName()))
			return false;
		return true;
	}

	public static <T extends Named> int hashCode(T object) {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object.getId() == null) ? 0 : object.getId().hashCode());
		result = prime * result + ((object.getName() == null) ? 0 : object.getName().hashCode());
		return result;
	}

	public static <T extends Identifiable> boolean equals(T a, Object b) {
		if (a == b)
			return true;
		if (b == null)
			return false;
		if (a.getClass() != b.getClass())
			return false;
		T other = (T) b;
		if (a.getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!a.getId().equals(other.getId()))
			return false;
		return true;
	}

	public static <T extends Identifiable> int hashCode(T object) {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object.getId() == null) ? 0 : object.getId().hashCode());
		return result;
	}
}
