/*
 * Copyright 2011-2021 the original author or authors.
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
package org.appng.api.rest;

import org.appng.api.rest.model.ErrorModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

/**
 * A {@link ResponseEntity} that offers an {@link ErrorModel}, which contains details about potential errors on the server side.
 * 
 * @author Matthias Müller
 *
 * @param <T>
 *            the response's body type
 */
public class RestResponseEntity<T> extends ResponseEntity<T> {

	private ErrorModel error;

	public RestResponseEntity(HttpStatus status) {
		super(status);
	}

	public RestResponseEntity(MultiValueMap<String, String> headers, HttpStatus status) {
		super(headers, status);
	}

	public RestResponseEntity(T body, HttpStatus status) {
		super(body, status);
	}

	public RestResponseEntity(T body, MultiValueMap<String, String> headers, HttpStatus status) {
		super(body, headers, status);
	}

	public RestResponseEntity(ErrorModel error, HttpStatus status) {
		this(status);
		this.error = error;
	}

	public RestResponseEntity(ErrorModel error, MultiValueMap<String, String> headers, HttpStatus status) {
		this(headers, status);
		this.error = error;
	}

	/**
	 * The error, if any
	 * 
	 * @return the error
	 */
	public ErrorModel getError() {
		return error;
	}

	public static <T> RestResponseEntity<T> of(ResponseEntity<T> exchange) {
		return new RestResponseEntity<>(exchange.getBody(), exchange.getHeaders(), exchange.getStatusCode());
	}

}
