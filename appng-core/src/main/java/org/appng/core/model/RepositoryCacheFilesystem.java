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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

/**
 * Implementation of {@link RepositoryCache} that retrieves the packages from the local filesystem.
 * 
 * @author Matthias Herlitzius
 * 
 */
public class RepositoryCacheFilesystem extends RepositoryCacheBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCacheFilesystem.class);
	private static final String ZIP = ".zip";
	private static final String MINUS = "-";
	private Long lastScan;
	private Long scanPeriod = TimeUnit.SECONDS.toMillis(10);
	private Map<File, PackageInfo> activeFileMap = new HashMap<File, PackageInfo>();
	private Set<String> invalidFileMap = new HashSet<String>();
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
		activeFileMap = new HashMap<File, PackageInfo>();
		invalidFileMap = new HashSet<String>();
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

	void update() {
		long currentTime = System.currentTimeMillis();
		if (null == lastScan || currentTime - lastScan >= scanPeriod) {
			StopWatch stopWatch = new StopWatch("RepositoryCacheFilesystem: scan repository");
			stopWatch.start();
			Map<File, PackageInfo> newfileMap = new HashMap<File, PackageInfo>();
			Set<TypedPackage> changedPackages = new HashSet<TypedPackage>();
			File[] currentFiles = getFiles();
			for (File file : currentFiles) {
				if (!activeFileMap.containsKey(file)) {
					// new file
					PackageArchive archive = new PackageArchiveImpl(file, repository.isStrict());
					PackageType type = archive.getType();
					if (archive.isValid()) {
						LOGGER.debug("New file found in repository: " + file.getAbsolutePath());
						PackageInfo packageInfo = archive.getPackageInfo();
						newfileMap.put(file, packageInfo);
						String packageName = addApplication(packageInfo);
						changedPackages.add(new TypedPackage(packageName, type));
					} else {
						// invalid file
						LOGGER.trace("Invalid file found in repository: " + file.getAbsolutePath());
						invalidFileMap.add(file.getName());
					}
				} else {
					// existing file --> take archive from activeFileMap.
					LOGGER.trace("Existing file in repository: " + file.getAbsolutePath());
					PackageInfo applicationInfo = activeFileMap.get(file);
					newfileMap.put(file, applicationInfo);
				}
			}

			Set<File> keySet = activeFileMap.keySet();
			keySet.removeAll(newfileMap.keySet());
			for (File file : keySet) {
				// deleted file
				LOGGER.debug("Deleted file from repository: " + file.getAbsolutePath());
				PackageInfo applicationInfo = activeFileMap.get(file);
				removePackage(applicationInfo);
			}
			for (TypedPackage typedPackage : changedPackages) {
				if (applicationWrapperMap.containsKey(typedPackage.getName())) {
					applicationWrapperMap.get(typedPackage.getName()).init(typedPackage.getType());
				}
			}
			activeFileMap = newfileMap;
			lastScan = System.currentTimeMillis();
			stopWatch.stop();
			LOGGER.debug(stopWatch.shortSummary());
		}
	}

	void update(String packageName) {
		update();
	}

	private String addApplication(PackageInfo packageInfo) {
		String name = packageInfo.getName();
		if (applicationWrapperMap.containsKey(name)) {
			applicationWrapperMap.get(name).put(packageInfo);
		} else {
			PackageWrapper wrapper = new PackageWrapper();
			wrapper.put(packageInfo);
			applicationWrapperMap.put(name, wrapper);
		}
		return name;
	}

	private String removePackage(PackageInfo packageInfo) {
		String name = packageInfo.getName();
		if (applicationWrapperMap.containsKey(name)) {
			try {
				String version = packageInfo.getVersion();
				String timestamp = packageInfo.getTimestamp();
				deletePackage(name, version, timestamp, false);
			} catch (Exception e) {
			}
		}
		return name;
	}

	public void deleteApplicationVersion(String packageName, String packageVersion, String packageTimestamp)
			throws BusinessException {
		String packageVersionSignature = getApplicationVersionSignature(packageName, packageVersion, packageTimestamp);
		try {
			File applicationFile = getPackageFile(packageName, packageVersion, packageTimestamp);
			if (applicationFile.delete()) {
				deletePackage(packageName, packageVersion, packageTimestamp, true);
				LOGGER.info("deleted " + packageVersionSignature + " from repository " + directory.getAbsolutePath());
			} else {
				throw new BusinessException("Unable to delete application: " + packageVersionSignature + ", file: "
						+ applicationFile.getAbsolutePath());
			}
		} catch (FileNotFoundException e) {
			throw new BusinessException(e);
		}
	}

	private void deletePackage(String packageName, String packageVersion, String packageTimestamp, boolean initWrapper)
			throws BusinessException {
		PackageWrapper wrapper = applicationWrapperMap.get(packageName);
		wrapper.deletePackageVersion(packageVersion, packageTimestamp);
		if (wrapper.getVersions().isEmpty()) {
			applicationWrapperMap.remove(packageName);
		} else if (initWrapper) {
			wrapper.init();
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
					+ getApplicationVersionSignature(packageName, packageVersion, packageTimestamp));
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

	public PackageArchive getApplicationArchive(String packageName, String packageVersion, String packageTimestamp)
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
					+ getApplicationVersionSignature(packageName, packageVersion, packageTimestamp), e);
		}
	}

	private File[] getFiles() {
		return directory.listFiles(new FilenameFilter() {
			public boolean accept(File file, String name) {
				if (name.endsWith(ZIP)) {
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
			}
		});
	}

	private boolean isValidFile(String name) {
		return !invalidFileMap.contains(name);
	}
}
