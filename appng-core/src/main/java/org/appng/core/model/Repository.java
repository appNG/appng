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
package org.appng.core.model;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.appng.api.BusinessException;
import org.appng.api.model.Application;
import org.appng.api.model.Identifier;
import org.appng.api.model.Named;
import org.appng.api.model.Versionable;
import org.appng.core.security.signing.SigningException;
import org.appng.core.xml.repository.Certification;
import org.appng.core.xml.repository.Package;
import org.appng.core.xml.repository.PackageVersions;
import org.appng.core.xml.repository.Packages;
import org.appng.xml.application.PackageInfo;

/**
 * A Application Repository contains and provides multiple {@link Application}s for provisioning. In the repository
 * applications are represented through a {@link InstallablePackage}.
 * 
 * @author Matthias Herlitzius
 * @author Matthias Müller
 * 
 */
public interface Repository extends Named<Integer>, Versionable<Date> {

	/**
	 * The type for this repository.
	 * 
	 * @return the {@link RepositoryType}
	 */
	RepositoryType getRepositoryType();

	/**
	 * The URI for this {@link Repository}, may refer to to local filesystem or a remote address
	 * 
	 * @return the URI
	 */
	URI getUri();

	/**
	 * Whether or not {@link PackageArchive}s can be retrieved from this repository
	 * 
	 * @return {@code true} if this repository is active, {@code false} otherwise
	 */
	boolean isActive();

	/**
	 * Whether or not this repository uses strict filename validation when scanning for {@link PackageArchive}s, i.e.
	 * the naming pattern must be {@code <name>-<version>-<timestamp>.zip}. If this method returns {@code false}, the
	 * more lax naming pattern {@code <name>-<version>.zip} is used, omitting the {@code <timestamp>} part.
	 * 
	 * @return if this repository uses strict filename validation
	 */
	boolean isStrict();

	/**
	 * Whether or not this repository is being published via the SOAP interface, so other repositories can refer to it
	 * as an remote repository
	 * 
	 * @return {@code true} if this repository is published, {@code false} otherwise
	 * @see #getRepositoryType()
	 */
	boolean isPublished();

	/**
	 * The digest for this repository, used for local published repositories to grant remote accesss
	 * 
	 * @return the digest
	 * @see #isPublished()
	 */
	String getDigest();

	/**
	 * The mode for this repository,
	 * 
	 * @return the {@link RepositoryMode}
	 */
	RepositoryMode getRepositoryMode();

	/**
	 * In case the type for this repository is {@link RepositoryType#REMOTE}, returns the name of the referred remote
	 * repository.
	 * 
	 * @return the name of the referred remote repository
	 */
	String getRemoteRepositoryName();

	/**
	 * Returns a list of {@link InstallablePackage}s for the given list of package {@link Identifier}s
	 * 
	 * @param provisionedPackages
	 *            the list of package identifiers
	 * @return a list of {@link InstallablePackage}s for the given package identifiers
	 * @throws BusinessException
	 *             if an error occurs while retrieving the packages
	 */
	List<InstallablePackage> getInstallablePackages(List<Identifier> provisionedPackages) throws BusinessException;

	/**
	 * Returns the {@link PackageArchive} with the given name, version and timestamp
	 * 
	 * @param packageName
	 *            the name of the archive
	 * @param packageVersion
	 *            the version of the archive
	 * @param packageTimestamp
	 *            the timestamp of the archive, only used if {@link #isStrict()} returns {@code true}.
	 * @return the {@link PackageArchive}
	 * @throws BusinessException
	 *             if no such {@link PackageArchive} exists or if this repository is not active
	 * 
	 * @see #isStrict()
	 * @see #isActive()
	 */
	PackageArchive getPackageArchive(String packageName, String packageVersion, String packageTimestamp)
			throws BusinessException;

	/**
	 * Returns the {@link Packages} offered by this repository, containing a list of all available {@link Package}s
	 * 
	 * @return the {@link Packages} offered by this repository
	 * @throws BusinessException
	 *             if an error occurs while reading the package informations
	 */
	Packages getPackages() throws BusinessException;

	/**
	 * Returns the {@link Certification}
	 * 
	 * @return
	 * @throws BusinessException
	 */
	Certification getCertification() throws BusinessException;

	/**
	 * Returns the {@link PackageVersions} for the package with the given name, containing a list of {@link PackageInfo}
	 * rmations
	 * 
	 * @param packageName
	 *            the name of the package to get the versions for
	 * @return the {@link PackageVersions}
	 * @throws BusinessException
	 *             if an error occurs while retrieving the {@link PackageVersions}
	 */
	PackageVersions getPackageVersions(String packageName) throws BusinessException;

	/**
	 * Returns the certificate chain, if this is a remote repository and a certificate chain to be trusted has been
	 * defined
	 * 
	 * @return the immutable certificate chain, or {@code null}
	 * @throws SigningException
	 *             if an error occurs while reading the certificate chain
	 */
	Collection<X509Certificate> getTrustedCertChain() throws SigningException;
}
