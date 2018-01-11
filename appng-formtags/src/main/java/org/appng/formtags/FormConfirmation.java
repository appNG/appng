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
package org.appng.formtags;

import org.apache.commons.lang3.StringUtils;

public class FormConfirmation {
	/**
	 * Defines when to display the FormConfirmation
	 * 
	 */
	public enum FormConfirmationMode {
		/** Show Form Confirmation section only if the form is submitted. (default mode) **/
		SUBMITTED,

		/**
		 * After submitting the form, the formConfirmation section may be controlled by the ProcessProvider through GET
		 * Requests
		 **/
		ALLOW_GET_REQUESTS,

		/** Show Form Confirmation section always. **/
		ALWAYS;
	}

	private FormConfirmationMode mode = FormConfirmationMode.SUBMITTED;
	private Form form;

	FormConfirmation(Form form) {
		this.form = form;
	}

	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}

	public FormConfirmationMode getMode() {
		return mode;
	}

	public void setMode(FormConfirmationMode mode) {
		this.mode = mode;
	}

	public void setMode(String mode) {
		try {
			setMode(FormConfirmationMode.valueOf(mode.toUpperCase()));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("invalid value '" + mode + "' for mode, allowed are "
					+ StringUtils.join(FormConfirmationMode.values(), ", "));
		}
	}

}
