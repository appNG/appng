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
package org.appng.core.model;

import java.util.HashSet;
import java.util.Set;

/**
 * The URI-scheme for an {@link Repository},
 * 
 * @author Matthias Herlitzius
 * 
 */
public enum RepositoryScheme {

	/** file scheme, using {@link RepositoryType#LOCAL} */
	FILE("file", RepositoryType.LOCAL),
	/** https scheme, using {@link RepositoryType#REMOTE} */
	HTTP("http", RepositoryType.REMOTE),
	/** https scheme, using {@link RepositoryType#REMOTE} */
	HTTPS("https", RepositoryType.REMOTE);

	private String uriForm;
	private RepositoryType repositoryType;

	private RepositoryScheme(String uriForm, RepositoryType repositoryType) {
		this.uriForm = uriForm;
		this.repositoryType = repositoryType;
	}

	/**
	 * Return the {@link RepositoryType} supported by this scheme.
	 * 
	 * @return the {@link RepositoryType}
	 */
	public RepositoryType getSupportedRepositoryType() {
		return repositoryType;
	}

	public static Set<RepositoryScheme> getSchemes(RepositoryType repositoryType) {
		Set<RepositoryScheme> supportedSchemes = new HashSet<RepositoryScheme>();
		for (RepositoryScheme scheme : RepositoryScheme.values()) {
			if (repositoryType.equals(scheme.getSupportedRepositoryType())) {
				supportedSchemes.add(scheme);
			}
		}
		return supportedSchemes;
	}

	public String toString() {
		return uriForm;
	}
}
