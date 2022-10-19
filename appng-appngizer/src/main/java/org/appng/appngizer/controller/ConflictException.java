package org.appng.appngizer.controller;

/**
 * {@link ConflictException} can be used to return <code>ResponseEntity<String></code>
 * from controller methods that are defined with a different type than <code>String</code>.
 *
 * E.g. the controller returns <code>ResponseEntity<Application></code> but in case of errors a message
 * should be returned as <code>ResponseEntity<String></code> together with <code>HttpStatus.CONFLICT</code>.
 * To achieve this, a {@link ConflictException} can be raised and will be caught by the
 * {@link ControllerBase#onConflictException}.
 *
 */
public class ConflictException extends Exception {

	public ConflictException(String exceptionMessage) {
		super(exceptionMessage);
	}

}
