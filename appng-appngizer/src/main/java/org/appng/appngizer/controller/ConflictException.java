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
