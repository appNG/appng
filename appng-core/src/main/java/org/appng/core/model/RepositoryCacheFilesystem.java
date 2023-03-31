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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.core.domain.PackageArchiveImpl;
import org.appng.core.security.signing.BaseConfig.PrivateKeyFormat;
import org.appng.core.security.signing.BaseConfig.SigningAlgorithm;
import org.appng.core.security.signing.Signer;
import org.appng.core.security.signing.SignerConfig;
import org.appng.core.security.signing.SigningException;
import org.appng.core.xml.repository.Certification;
import org.appng.core.xml.repository.PackageType;
import org.appng.xml.application.PackageInfo;
import org.springframework.util.StopWatch;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link RepositoryCache} that retrieves the packages from the local filesystem.
 * 
 * @author Matthias Herlitzius
 */
@Slf4j
public class RepositoryCacheFilesystem extends RepositoryCacheBase {

	private static final String ZIP = ".zip";
	private static final String MINUS = "-";
	private Long lastScan;
	private Long scanPeriod = TimeUnit.SECONDS.toMillis(10);
	private Map<File, PackageInfo> activeFileMap;
	private Set<String> invalidFileMap;
	private RepositoryMode repositoryMode;
	private File directory;
	protected byte[] privateKey;

	RepositoryCacheFilesystem(Repository repository, byte[] cert, byte[] privateKey) throws BusinessException {
		super(repository, cert, false);
		if (null != privateKey) {
			this.privateKey = ArrayUtils.clone(privateKey);
		}
		reload();
	}

	void init() throws BusinessException {
		activeFileMap = new HashMap<>();
		invalidFileMap = new HashSet<>();
		lastScan = null;
		directory = new File(repository.getUri());
		repositoryMode = repository.getRepositoryMode();
		update();
		try {
			createIndex();
		} catch (SigningException e) {
			throw new BusinessException(String.format("error while signing repository %s", repository.getName()), e);
		}
	}

	private void createIndex() throws SigningException {
		if (null != cert && null != privateKey) {
			Path path = new File(repository.getUri()).toPath();
			SignerConfig config = new SignerConfig(repository.getName(), repository.getDescription(), "", privateKey,
					cert, SigningAlgorithm.SHA512withRSA, PrivateKeyFormat.PEM);
			this.signatureWrapper = Signer.signRepo(path, config);
		}
	}

	public Certification getCertification() {
		Certification certification = null;
		if (null != signatureWrapper && signatureWrapper.isValid()) {
			certification = new Certification();
			Encoder encoder = Base64.getEncoder();
			certification.setCert(encoder.encode(cert));
			certification.setIndex(encoder.encode(signatureWrapper.getIndex()));
			certification.setSignature(encoder.encode(signatureWrapper.getSignature()));
		}
		return certification;
	}

	class TypedPackage {
		private final String name;
		private final PackageType type;

		public TypedPackage(String name, PackageType type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public PackageType getType() {
			return type;
		}
	}

	synchronized void update() {
		long currentTime = System.currentTimeMillis();
		if (null == lastScan || currentTime - lastScan >= scanPeriod) {
			StopWatch stopWatch = new StopWatch("RepositoryCacheFilesystem: scan repository");
			stopWatch.start();
			Set<String> updatedPackages = new HashSet<>();
			Map<File, PackageInfo> newFileMap = new ConcurrentHashMap<>();
			Set<File> oldFiles = new HashSet<>(activeFileMap.keySet());

			getArchives(null).forEach(archive -> {
				File file = archive.getFile();
				if (!activeFileMap.containsKey(file)) {
					if (addArchive(newFileMap, archive, false)) {
						updatedPackages.add(addPackage(archive.getPackageInfo(), archive.getType()));
					}
				} else {
					LOGGER.trace("Existing file in repository: {}", file.getAbsolutePath());
					PackageInfo applicationInfo = activeFileMap.get(file);
					newFileMap.put(file, applicationInfo);
				}
			});

			oldFiles.removeAll(activeFileMap.keySet());
			for (File file : oldFiles) {
				try {
					LOGGER.debug("Removing file from repository: {}", file.getAbsolutePath());
					PackageInfo applicationInfo = activeFileMap.get(file);
					removePackage(applicationInfo.getName(), applicationInfo.getVersion(),
							applicationInfo.getTimestamp(), false);
				} catch (BusinessException e) {
					LOGGER.error("Error removing file", e);
				}
			}

			updatedPackages.stream().forEach(pkg -> initializeWrapper(pkg));
			activeFileMap = newFileMap;
			lastScan = System.currentTimeMillis();
			stopWatch.stop();
			LOGGER.debug(stopWatch.shortSummary());
		}
	}

	public boolean add(PackageArchive archive) {
		return addArchive(activeFileMap, archive, true);
	}

	private boolean addArchive(Map<File, PackageInfo> fileMap, PackageArchive archive, boolean initializeWrapper) {
		boolean added = false;
		File file = archive.getFile();
		if (archive.isValid() && null != archive.getPackageInfo()) {
			if (!fileMap.containsKey(file)) {
				LOGGER.debug("New file found in repository: {}", file.getAbsolutePath());
				PackageInfo packageInfo = archive.getPackageInfo();
				addPackage(packageInfo, archive.getType());
				fileMap.put(file, packageInfo);
				added = true;
			}
		} else {
			LOGGER.trace("Invalid file found in repository: {}", file.getAbsolutePath());
			invalidFileMap.add(file.getName());
			fileMap.remove(file);
		}
		if (initializeWrapper) {
			initializeWrapper(archive.getPackageInfo().getName());
		}
		return added;
	}

	public void initializeWrapper(String packageName) {
		if (applicationWrapperMap.containsKey(packageName)) {
			applicationWrapperMap.get(packageName).init();
		}
	}

	private String addPackage(PackageInfo packageInfo, PackageType type) {
		String name = packageInfo.getName();
		if (applicationWrapperMap.containsKey(name)) {
			applicationWrapperMap.get(name).addPackage(packageInfo);
		} else {
			applicationWrapperMap.put(name, new PackageWrapper(packageInfo, type));
		}
		return name;
	}

	public void deletePackageVersion(String packageName, String packageVersion, String packageTimestamp)
			throws BusinessException {
		String packageVersionSignature = getPackageVersionSignature(packageName, packageVersion, packageTimestamp);
		try {
			File packageFile = getPackageFile(packageName, packageVersion, packageTimestamp);
			if (packageFile.delete()) {
				removePackage(packageName, packageVersion, packageTimestamp, true);
				LOGGER.info("deleted {} from repository {}", packageVersionSignature, directory.getAbsolutePath());
			} else {
				throw new BusinessException("Unable to delete package: " + packageVersionSignature + ", file: "
						+ packageFile.getAbsolutePath());
			}
		} catch (FileNotFoundException e) {
			throw new BusinessException(e);
		}
	}

	private void removePackage(String packageName, String packageVersion, String packageTimestamp, boolean initWrapper)
			throws BusinessException {
		PackageWrapper wrapper = applicationWrapperMap.get(packageName);
		if (null != wrapper) {
			wrapper.removePackageVersion(packageVersion, packageTimestamp);
			if (wrapper.getVersions().isEmpty()) {
				applicationWrapperMap.remove(packageName);
			} else if (initWrapper) {
				wrapper.init();
			}
		}
	}

	private File getPackageFile(String packageName, String packageVersion, String packageTimestamp)
			throws FileNotFoundException, BusinessException {
		PackageInfo packageInfo;
		if (StringUtils.isBlank(packageVersion)) {
			// latest Snapshot
			packageInfo = getSnapshot(packageName);
		} else if (RepositoryUtils.isSnapshot(packageVersion)) {
			if (StringUtils.isBlank(packageTimestamp)) {
				// latest Snapshot of packageVersion
				packageInfo = getSnapshot(packageName, packageVersion);
			} else {
				// specified snapshot
				packageInfo = getSnapshot(packageName, packageVersion, packageTimestamp);
			}
		} else {
			// specified release
			packageInfo = getRelease(packageName, packageVersion);
		}
		if (null == packageInfo) {
			throw new FileNotFoundException("application not found: "
					+ getPackageVersionSignature(packageName, packageVersion, packageTimestamp));
		}
		return getFile(packageInfo);
	}

	private File getFile(PackageInfo packageInfo) throws FileNotFoundException {
		File file = new File(directory,
				packageInfo.getName() + MINUS + packageInfo.getVersion() + MINUS + packageInfo.getTimestamp() + ZIP);
		if (file.exists()) {
			return file;
		} else if (!repository.isStrict()) {
			file = new File(directory, packageInfo.getName() + MINUS + packageInfo.getVersion() + ZIP);
			if (file.exists()) {
				return file;
			}
		}
		throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
	}

	public PackageArchive getPackageArchive(String packageName, String packageVersion, String packageTimestamp)
			throws BusinessException {
		try {
			File applicationFile = getPackageFile(packageName, packageVersion, packageTimestamp);
			PackageArchive archive = new PackageArchiveImpl(applicationFile, repository.isStrict());
			if (!archive.isValid()) {
				throw new BusinessException("invalid application archive: " + applicationFile.getAbsolutePath());
			}
			return archive;
		} catch (FileNotFoundException e) {
			throw new BusinessException("application archive not found for: "
					+ getPackageVersionSignature(packageName, packageVersion, packageTimestamp), e);
		}
	}

	private Collection<PackageArchive> getArchives(String archive) {
		List<File> files = Arrays.asList(directory.listFiles((file, name) -> {
			if (name.endsWith(ZIP) && (null == archive || name.startsWith(archive))) {
				switch (repositoryMode) {
				case ALL:
					return isValidFile(name);
				case SNAPSHOT:
					return RepositoryUtils.isSnapshot(name) && isValidFile(name);
				case STABLE:
					return !RepositoryUtils.isSnapshot(name) && isValidFile(name);
				}
			}
			return false;
		}));
		return files.parallelStream().map(file -> new PackageArchiveImpl(file, repository.isStrict()))
				.collect(Collectors.toList());
	}

	private boolean isValidFile(String name) {
		return !invalidFileMap.contains(name);
	}
}
