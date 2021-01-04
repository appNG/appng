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
package org.appng.core.security;

import java.util.Date;

import org.appng.api.model.AuthSubject;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.service.CoreService;

/**
 * Provides methods to hash and validate passwords using the SHA-1 algorithm.
 * 
 * @author Matthias Herlitzius
 * @deprecated wille be removed in 2.x
 */
@Deprecated
public class Sha1PasswordHandler implements PasswordHandler {

	private final AuthSubject authSubject;
	private final SaltedDigest saltedDigest = new SaltedDigestSha1();

	public Sha1PasswordHandler(AuthSubject authSubject) {
		this.authSubject = authSubject;
	}

	public void applyPassword(String password) {
		String salt = saltedDigest.getSalt();
		String digest = saltedDigest.getDigest(password, salt);
		authSubject.setSalt(salt);
		authSubject.setDigest(digest);
		authSubject.setPasswordLastChanged(new Date());
	}

	public boolean isValidPassword(String password) {
		String digest = saltedDigest.getDigest(password, authSubject.getSalt());
		return digest.equals(authSubject.getDigest());
	}

	public String calculatePasswordResetDigest() {
		return saltedDigest.getDigest(authSubject.getEmail(), authSubject.getSalt());
	}

	public boolean isValidPasswordResetDigest(String digest) {
		return calculatePasswordResetDigest().equals(digest);
	}

	public void migrate(CoreService service, String password) {
		// migration is only supported for AuthSubject of instance SubjectImpl because the new encrypted password needs
		// to be saved persistently in the database.
		if (authSubject instanceof SubjectImpl) {
			PasswordHandler passwordHandler = service.getDefaultPasswordHandler(authSubject);
			passwordHandler.applyPassword(password);
			if (null != ((SubjectImpl) authSubject).getVersion()) {
				service.updateSubject((SubjectImpl) authSubject);
			}
		}
	}

}
