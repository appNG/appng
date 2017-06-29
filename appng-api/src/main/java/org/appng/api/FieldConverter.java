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
package org.appng.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appng.forms.RequestContainer;
import org.appng.xml.platform.Datafield;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Linkpanel;

/**
 * 
 * A {@link FieldConverter} is responsible for converting
 * <ol>
 * <li>a property of an bindclass-instance to its {@code String}-representation inside of a {@link Datafield} (see
 * {@link #setString(FieldWrapper)})</li>
 * <li>a parameter from a {@link RequestContainer} to its object-representation (see
 * {@link #setObject(FieldWrapper, RequestContainer)})</li>
 * </ol>
 * 
 * @author Matthias Müller
 * 
 */
public interface FieldConverter {

	/**
	 * 
	 * @param field
	 */
	void reset(FieldWrapper field);

	/**
	 * Transforms the property identified by {@link FieldWrapper#getBinding()} to it's {@link String} representation,
	 * which is used as an argument for {@link Datafield#setValue(String)} in
	 * {@link #addField(DatafieldOwner, FieldWrapper)}.
	 * 
	 * @param field
	 *            the {@link FieldWrapper}
	 * 
	 * @see FieldWrapper#setStringValue(String)
	 */
	void setString(FieldWrapper field);

	/**
	 * Sets the {@code Object} for the given {@link FieldWrapper}, using the given {@link RequestContainer} as the
	 * parameter source.
	 * 
	 * @param field
	 *            the {@link FieldWrapper}
	 * @param request
	 *            the {@link RequestContainer}
	 * 
	 * @see FieldWrapper#setObject(Object)
	 */
	void setObject(FieldWrapper field, RequestContainer request);

	/**
	 * Creates a {@link Datafield} and add it to the given {@link DatafieldOwner}, using
	 * {@link FieldWrapper#getStringValue()} for the {@link Datafield}'s value.
	 * 
	 * @param dataFieldOwner
	 *            the {@link FieldWrapper}
	 * @param fieldWrapper
	 *            the {@link FieldWrapper}
	 * @return the {@link Datafield} which has been added, if any
	 */
	Datafield addField(DatafieldOwner dataFieldOwner, FieldWrapper fieldWrapper);

	/**
	 * /** A {@code DatafieldOwner} owns several {@link FieldDef}s and also {@link Linkpanel}s.
	 * 
	 * @author Matthias Müller
	 * 
	 */
	public static abstract class FieldDefOwner {

		/**
		 * Returns the {@link FieldDef}s owned by this {@code FieldDefOwner}.
		 * 
		 * @return a {@link List} of {@link FieldDef}s
		 */
		public abstract List<FieldDef> getFields();

		/**
		 * Returns the {@link Linkpanel}s owned by this {@code FieldDefOwner}.
		 * 
		 * @return a {@link List} of {@link Linkpanel}s
		 */
		public abstract List<Linkpanel> getLinkpanels();

	}

	/**
	 * A {@code DatafieldOwner} owns several {@link Datafield}s and also {@link Linkpanel}s.
	 * 
	 * @author Matthias Müller
	 * 
	 */
	public static abstract class DatafieldOwner {

		/**
		 * Returns the {@link Datafield}s owned by this {@code DatafieldOwner}.
		 * 
		 * @return a {@link List} of {@link Datafield}s
		 */
		public abstract List<Datafield> getFields();

		/**
		 * Returns the {@link Linkpanel}s owned by this {@code DatafieldOwner}.
		 * 
		 * @return a {@link List} of {@link Linkpanel}s
		 */
		public abstract List<Linkpanel> getLinkpanels();

		/**
		 * Returns the {@link Datafield} with the given name
		 * 
		 * @param name
		 *            the name of the {@link Datafield} to got
		 * @return the {@link Datafield}, if any
		 */
		Datafield getField(String name) {
			List<Datafield> fields = getFields();
			for (Datafield datafield : fields) {
				if (datafield.getName().equals(name)) {
					return datafield;
				}
			}
			return null;
		}

		/**
		 * Constructs a {@link Map} containing all the {@link Datafield}'s names as a key and their value as a value.
		 * 
		 * @return the {@link Map}
		 */
		public Map<String, String> getFieldValues() {
			Map<String, String> values = new HashMap<String, String>();
			for (Datafield datafield : getFields()) {
				values.put(datafield.getName(), datafield.getValue());
			}
			return values;
		}
	}

}
