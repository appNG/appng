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
package org.appng.api.model;

/**
 * An {@link AuthSubject} provides some user-credentials.
 * 
 * @author Matthias Müller
 * 
 */
public interface AuthSubject extends Account {

	/**
	 * Returns the salted digest for this {@link AuthSubject}.
	 * 
	 * @return the salted digest
	 */
	String getDigest();

	/**
	 * Returns the <a href="http://en.wikipedia.org/wiki/Salt_(cryptography)">salt</a> for this {@link AuthSubject}.
	 * 
	 * @return the salt
	 */
	String getSalt();

	/**
	 * Sets the salt for this {@link AuthSubject}.
	 * 
	 * @param salt
	 *            the salt
	 */
	void setSalt(String salt);

	/**
	 * Sets the salted digest for this {@link AuthSubject}.
	 * 
	 * @param digest
	 *            the digest
	 */
	void setDigest(String digest);

}
