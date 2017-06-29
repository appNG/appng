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
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides methods to hash and validate passwords using the SHA-1 algorithm.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class Sha1PasswordHandler implements PasswordHandler {

	private final AuthSubject authSubject;
	private final SaltedDigest saltedDigest = new SaltedDigestSha1();

	public Sha1PasswordHandler(AuthSubject authSubject) {
		this.authSubject = authSubject;
	}

	public void savePassword(String password) {
		String salt = saltedDigest.getSalt();
		String digest = saltedDigest.getDigest(password, salt);
		authSubject.setSalt(salt);
		authSubject.setDigest(digest);
	}

	public boolean isValidPassword(String password) {
		String digest = saltedDigest.getDigest(password, authSubject.getSalt());
		return digest.equals(authSubject.getDigest());
	}

	public String getPasswordResetDigest() {
		String digest = saltedDigest.getDigest(authSubject.getEmail(), authSubject.getSalt());
		return digest;
	}

	public boolean isValidPasswordResetDigest(String digest) {
		String expectedDigest = getPasswordResetDigest();
		return (expectedDigest.equals(digest));
	}

	public void updateSubject(CoreService service) throws BusinessException {
	}

	@Transactional
	public void migrate(CoreService service, String password) {
		// migration is only supported for AuthSubject of instance SubjectImpl because the new encrypted password needs
		// to be saved persistently in the database.
		if (authSubject instanceof SubjectImpl) {
			PasswordHandler passwordHandler = service.getDefaultPasswordHandler(authSubject);
			passwordHandler.savePassword(password);
			if (null != ((SubjectImpl) authSubject).getVersion()) {
				service.updateSubject((SubjectImpl) authSubject);
			}
		}
	}

}
