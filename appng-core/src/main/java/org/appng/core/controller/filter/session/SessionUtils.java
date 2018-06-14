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
package org.appng.core.controller.filter.session;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.WriteAbortedException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.session.ExpiringSession;

/**
 * Utility class for reading/writing an {@link ExpiringSession} from an {@link ObjectInputStream} / to an
 * {@link ObjectOutputStream}.
 * 
 * @author Matthias Müller
 *
 */
public class SessionUtils {

	public static void readFromStream(ObjectInputStream stream, ExpiringSession session)
			throws ClassNotFoundException, IOException {
		int numAttributes = ((Integer) stream.readObject()).intValue();
		for (int i = 0; i < numAttributes; i++) {
			String name = (String) stream.readObject();
			try {
				Object value = stream.readObject();
				session.setAttribute(name, value);
			} catch (WriteAbortedException wae) {
			}
		}
	}

	public static void writeToStream(ObjectOutputStream stream, ExpiringSession session) throws IOException {
		List<String> names = new ArrayList<>();
		List<Object> values = new ArrayList<>();
		for (String key : session.getAttributeNames()) {
			Object value = session.getAttribute(key);
			if (value != null && value instanceof Serializable) {
				names.add(key);
				values.add(value);
			}
		}

		int numAttributes = names.size();
		stream.writeObject(Integer.valueOf(numAttributes));
		for (int i = 0; i < numAttributes; i++) {
			stream.writeObject(names.get(i));
			stream.writeObject(values.get(i));
		}
	}
}
