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
package org.appng.formtags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * A {@link FormData} contains the actual {@link FormElement}s/{@link FormGroup}s of a {@link Form}.
 * 
 * @author Matthias Müller
 * 
 * @see Form
 * @see FormElement
 */
public class FormData extends FormElementOwner {

	/**
	 * Defines when to display the {@link FormData}.
	 */
	public enum FormDataMode {
		/**
		 * Display {@link FormData} only if the form is not submitted or if the submitted data is not valid. (default
		 * mode)
		 **/
		NOT_SUBMITTED,

		/** Always display the {@link FormData} **/
		ALWAYS;
	}

	private FormDataMode mode = FormDataMode.NOT_SUBMITTED;
	private List<FormElementOwner> elementOwner;

	/**
	 * Creates a new {@link FormData} for the given {@link Form}
	 * 
	 * @param form
	 *             the {@link Form}
	 */
	FormData(Form form) {
		super(form);
		this.elementOwner = new ArrayList<>();
	}

	/**
	 * Returns the {@link FormDataMode} for this {@link FormData}.
	 * 
	 * @return the {@link FormDataMode}
	 */
	public FormDataMode getMode() {
		return mode;
	}

	/**
	 * Sets the {@link FormDataMode} for this {@link FormData}.
	 * 
	 * @param mode
	 *             the {@link FormDataMode}
	 */
	public void setMode(FormDataMode mode) {
		this.mode = mode;
	}

	/**
	 * Sets the {@link FormDataMode} for this {@link FormData} from a given {@link String}.
	 * 
	 * @param mode
	 *             a {@link String}
	 * 
	 * @throws IllegalArgumentException
	 *                                  if the {@link String} does not represent a valid {@link FormDataMode}
	 */
	public void setMode(String mode) {
		try {
			setMode(FormDataMode.valueOf(mode.toUpperCase()));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid value '" + mode + "' for mode, allowed are "
					+ StringUtils.join(FormDataMode.values(), ", "));
		}
	}

	/**
	 * Adds a new {@link FormElement} to this {@link FormData} and returns it.
	 * 
	 * @return the added {@link FormElement}
	 * 
	 * @see FormElement
	 */
	public FormElement addFormElement() {
		FormElement formElement = new FormElement(this);
		elementOwner.add(formElement);
		return formElement;
	}

	/**
	 * Adds a new {@link FormGroup} to this {@link FormData} and returns it.
	 * 
	 * @return the added {@link FormGroup}
	 * 
	 * @see FormGroup
	 */
	public FormGroup addFormGroup() {
		FormGroup formGroup = new FormGroup(this);
		elementOwner.add(formGroup);
		return formGroup;
	}

	/**
	 * Returns an immutable {@link List} containing all {@link FormElement}s of this {@link FormData}. This not only
	 * includes the {@link FormElement}s added via {@link #addFormElement()}, but also those {@link FormElement}s of the
	 * {@link FormGroup}s added via {@link #addFormGroup()}.
	 * 
	 * @return a {@link List} of {@link FormElement}s
	 */
	@Override
	public List<FormElement> getElements() {
		List<FormElement> allElements = new ArrayList<>();
		for (FormElementOwner formElementOwner : elementOwner) {
			allElements.addAll(formElementOwner.getElements());
		}
		return Collections.unmodifiableList(allElements);
	}

}
