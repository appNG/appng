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
package org.appng.core.model;

/**
 * Possible types of a {@link Repository}.
 * 
 * @author Matthias Herlitzius
 * 
 * @see RepositoryScheme
 */
public enum RepositoryType {

	/** local, meaning {@link PackageArchive}s are retrieved from the local filesystem */
	LOCAL,
	/** remote, meaning {@link PackageArchive}s are retrieved from a remote machine */
	REMOTE;

	public static RepositoryType getDefault() {
		return LOCAL;
	}
}
