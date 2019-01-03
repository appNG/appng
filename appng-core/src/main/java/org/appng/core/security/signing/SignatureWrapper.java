/*
 * Copyright 2011-2019 the original author or authors.
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

/**
 * A simple wrapper class for the index, the signature and the client certificate
 * 
 * @author Matthias Müller
 *
 */
public class SignatureWrapper {

	private byte[] index;
	private byte[] signature;
	private byte[] cert;

	public SignatureWrapper() {
	}

	public byte[] getIndex() {
		return index;
	}

	public void setIndex(byte[] index) {
		this.index = index;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	public boolean isValid() {
		return null != signature && null != index;
	}

	public byte[] getCert() {
		return cert;
	}

	public void setCert(byte[] cert) {
		this.cert = cert;
	}

}
