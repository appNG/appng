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
package org.appng.api;

import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.xml.platform.PageReference;

/**
 * 
 * A checked exception that is thrown by {@link CallableAction} and {@link CallableDataSource}, to indicate an error
 * occurred while assembling the {@link PageReference}.
 * 
 * @author Matthias Müller
 * 
 */
public class ProcessingException extends Exception {

	private final FieldProcessor fieldProcessor;

	/**
	 * Creates a new {@code ProcessingException}.
	 * 
	 * @param message
	 *            the error message.
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} in charge when the error occurred, used to append some further details
	 *            about the error via {@link FieldProcessor#addErrorMessage(String)}
	 */
	public ProcessingException(String message, FieldProcessor fieldProcessor) {
		super(message);
		this.fieldProcessor = fieldProcessor;
	}

	/**
	 * Creates a new {@code ProcessingException}.
	 * 
	 * @param message
	 *            the error message
	 * @param cause
	 *            the cause of the error
	 * @param fieldProcessor
	 *            the {@link FieldProcessor} in charge when the error occurred, used to append some further details
	 *            about the error via {@link FieldProcessor#addErrorMessage(String)}
	 */
	public ProcessingException(String message, Throwable cause, FieldProcessor fieldProcessor) {
		super(message, cause);
		this.fieldProcessor = fieldProcessor;
	}

	/**
	 * Returns the {@link FieldProcessor} in charge when the error occurred
	 * 
	 * @return the {@link FieldProcessor}
	 */
	public FieldProcessor getFieldProcessor() {
		return fieldProcessor;
	}

}
