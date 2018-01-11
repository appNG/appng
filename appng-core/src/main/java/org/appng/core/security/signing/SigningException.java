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
package org.appng.core.security.signing;

import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * An exception thrown when something goes wrong during signing/verifying
 * 
 * @author Dirk Heuvels
 */
public class SigningException extends IOException {

	private final ErrorType type;
	private X509Certificate cert;

	public enum ErrorType {
		SIGN, VERIFY
	}

	SigningException(ErrorType type, Exception cause) {
		super(cause);
		this.type = type;
	}

	SigningException(ErrorType type, String message) {
		super(message);
		this.type = type;
	}

	SigningException(ErrorType type, String message, X509Certificate cert) {
		super(message);
		this.type = type;
		this.cert = cert;
	}

	public SigningException(ErrorType type, String message, Exception cause) {
		super(message, cause);
		this.type = type;
	}
	
	SigningException(ErrorType type, String message, Exception cause, X509Certificate cert) {
		super(message, cause);
		this.type = type;
		this.cert = cert;
	}

	public ErrorType getType() {
		return type;
	}

	public X509Certificate getCert() {
		return cert;
	}

}
