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
package org.appng.appngizer.controller;

import java.util.List;

import org.appng.appngizer.model.xml.Errors;

/**
 * {@link ConflictException} can be used to return {@code ResponseEntity<Errors>} from controller methods that are
 * defined with a different type than {@code String}.
 * <p>
 * E.g. the controller returns {@code ResponseEntity<Application>} but in case of errors a message should be returned as
 * {@code ResponseEntity<Errors>} together with {@code HttpStatus.CONFLICT}. To achieve this, a
 * {@link ConflictException} can be raised and will be caught by the
 * {@link ControllerBase#onConflictException(javax.servlet.http.HttpServletRequest, ConflictException)}.
 * 
 * @see Errors
 * @see ControllerBase#onConflictException(javax.servlet.http.HttpServletRequest, ConflictException)
 */
public class ConflictException extends Exception {

	private final List<String> conflicts;

	public ConflictException(List<String> conflicts) {
		this.conflicts = conflicts;
	}

	public List<String> getConflicts() {
		return conflicts;
	}
}
