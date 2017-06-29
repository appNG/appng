/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.core.security;

import org.appng.api.BusinessException;
import org.appng.api.model.AuthSubject;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.service.CoreService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides methods to hash and validate passwords usings the bcrypt algorithm.
 * 
 * @see <a href="http://static.usenix.org/events/usenix99/provos/provos_html/">Provos, Niels; Talan Jason
 *      Sutton (1999). A Future-Adaptable Password Scheme</a>
 * @see <a href="http://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#core-services-password-encoding">Spring Security Reference</a>
 * @see <a href="https://docs.spring.io/spring-security/site/docs/current/apidocs/org/springframework/security/crypto/bcrypt/BCrypt.html">jBCrypt JavaDoc</a>
 * 
 * @author Matthias Herlitzius
 * 
 */
public class BCryptPasswordHandler implements PasswordHandler {

	/**
	 * The work factor. Higher numbers result in stronger hashes, but the computation takes longer.
	 * 
	 * @see <a href="http://www.mindrot.org/files/jBCrypt/jBCrypt-0.2-doc/BCrypt.html#gensalt%28int%29">bcrypt
	 *      JavaDoc</a>
	 */
	private static final int LOG_ROUNDS = 13;
	private static final String PREFIX = "$2a$";
	private final AuthSubject authSubject;

	public BCryptPasswordHandler(AuthSubject authSubject) {
		this.authSubject = authSubject;
	}

	public void savePassword(String password) {
		String hashed = BCrypt.hashpw(password, BCrypt.gensalt(LOG_ROUNDS));
		authSubject.setDigest(hashed);
		authSubject.setSalt(null);
	}

	public boolean isValidPassword(String password) {
		boolean isValid = BCrypt.checkpw(password, authSubject.getDigest());
		return isValid;
	}

	public String getPasswordResetDigest() {
		SaltedDigest saltedDigest = new SaltedDigestSha1();
		String salt = saltedDigest.getSalt();
		authSubject.setSalt(salt);
		String digest = saltedDigest.getDigest(authSubject.getEmail(), salt);
		return digest;
	}

	public boolean isValidPasswordResetDigest(String digest) {
		SaltedDigest saltedDigest = new SaltedDigestSha1();
		String expectedDigest = saltedDigest.getDigest(authSubject.getEmail(), authSubject.getSalt());
		return (expectedDigest.equals(digest));
	}

	@Transactional
	public void updateSubject(CoreService service) throws BusinessException {
		if (null != ((SubjectImpl) authSubject).getVersion()) {
			service.updateSubject((SubjectImpl) authSubject);
		} else {
			throw new BusinessException("Unable to update subject.");
		}
	}

	public void migrate(CoreService service, String password) {
	}

	/**
	 * Returns the identifier of the bcrypt algorithm.
	 * 
	 * @return The version identifier / prefix common to all bcrypt hashes.
	 * 
	 * @see <a href="http://static.usenix.org/events/usenix99/provos/provos_html/node6.html">A Future-Adaptable Password
	 *      Scheme / Implementation</a>
	 */
	public static String getPrefix() {
		return PREFIX;
	}

}
