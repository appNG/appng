/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.List;

import org.appng.api.BusinessException;
import org.appng.core.xml.repository.Certification;
import org.appng.xml.application.PackageInfo;

/**
 * Provides access to a application repository.
 * 
 * @author Matthias Herlitzius
 */
public interface RepositoryCache {

	/**
	 * Reloads the repository contents by clearing and updating the cache.
	 */
	void reload() throws BusinessException;

	/**
	 * Adds a single {@link PackageArchive}
	 * 
	 * @param packagearchive
	 *                       the archive to add
	 * 
	 * @throws BusinessException
	 */
	boolean add(PackageArchive packagearchive) throws BusinessException;

	/**
	 * Returns all packages found in the repository.
	 * 
	 * @return The package list.
	 * 
	 * @throws BusinessException
	 */
	List<PackageWrapper> getApplications() throws BusinessException;

	/**
	 * Returns all packages that match the given name in the repository.
	 * 
	 * @param packageName
	 *                    an optional search-string for the package's name, supporting {@code *} as a placeholder
	 * 
	 * @return The package list.
	 * 
	 * @throws BusinessException
	 */
	List<PackageWrapper> getApplications(String packageName) throws BusinessException;

	/**
	 * Returns the {@link PackageWrapper} for the a package.
	 * 
	 * @param name
	 *             The name of the package.
	 * 
	 * @return the {@link PackageWrapper}
	 */
	PackageWrapper getPackageWrapper(String name);

	/**
	 * Returns all available versions of a package.
	 * 
	 * @param name
	 *             The name of the application.
	 * 
	 * @return The {@link PackageInfo}s.
	 * 
	 * @throws BusinessException
	 *                           if such a package does not exist
	 */
	List<PackageInfo> getVersions(String name) throws BusinessException;

	/**
	 * Deletes the specified application version from the repository.
	 * 
	 * @param packageName
	 *                         The package name.
	 * @param packageVersion
	 *                         The package version.
	 * @param packageTimestamp
	 *                         The package timestamp.
	 * 
	 * @throws BusinessException
	 *                           if such a package does not exist
	 */
	void deletePackageVersion(String packageName, String packageVersion, String packageTimestamp)
			throws BusinessException;

	/**
	 * Returns a {@link PackageArchive } for the specified package.
	 * 
	 * @param packageName
	 *                         The package name.
	 * @param packageVersion
	 *                         The package version.
	 * @param packageTimestamp
	 *                         The package timestamp.
	 * 
	 * @return The {@link PackageArchive }.
	 * 
	 * @throws BusinessException
	 *                           if the archive was not found or is invalid
	 */
	PackageArchive getPackageArchive(String packageName, String packageVersion, String packageTimestamp)
			throws BusinessException;

	/**
	 * Returns the {@link Certification} for the cached repository, if any
	 * 
	 * @return the {@link Certification}
	 * 
	 * @throws BusinessException
	 *                           if an error occurred while building the certification
	 */
	Certification getCertification() throws BusinessException;

}
