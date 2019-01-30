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
package org.appng.appngizer.controller;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.model.Identifier;
import org.appng.api.model.Properties;
import org.appng.appngizer.model.Package;
import org.appng.appngizer.model.Packages;
import org.appng.appngizer.model.Repositories;
import org.appng.appngizer.model.Repository;
import org.appng.appngizer.model.xml.PackageType;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.model.InstallablePackage;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;
import org.appng.core.model.RepositoryUtils;
import org.appng.core.xml.repository.PackageVersions;
import org.appng.xml.application.PackageInfo;
import org.slf4j.Logger;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class RepositoryController extends ControllerBase {

	@RequestMapping(value = "/repository", method = RequestMethod.GET)
	public ResponseEntity<Repositories> listRepositories() {
		List<Repository> repoList = new ArrayList<>();
		for (RepositoryImpl r : getCoreService().getApplicationRepositories()) {
			repoList.add(org.appng.appngizer.model.Repository.fromDomain(r));
		}
		Repositories entity = new Repositories(repoList);
		entity.applyUriComponents(getUriBuilder());
		return ok(entity);
	}

	@RequestMapping(value = "/repository/{name}", method = RequestMethod.GET)
	public ResponseEntity<Repository> getRepository(@PathVariable("name") String name) throws BusinessException {
		org.appng.core.model.Repository r = getCoreService().getApplicationRepositoryByName(name);
		if (null == r) {
			return notFound();
		}
		Repository fromDomain = Repository.fromDomain(r);
		fromDomain.setPackages(new Packages());
		List<Identifier> provisionedPackages = new ArrayList<>();
		provisionedPackages.addAll(getCoreService().getApplications());
		provisionedPackages.addAll(getTemplateService().getInstalledTemplates());
		List<InstallablePackage> installablePackages = r.getInstallablePackages(provisionedPackages);
		for (InstallablePackage pkg : installablePackages) {
			org.appng.appngizer.model.Package p = new org.appng.appngizer.model.Package();
			p.setName(pkg.getName());
			p.setDisplayName(pkg.getDisplayName());
			p.setSnapshot(pkg.getLatestSnapshot());
			p.setRelease(pkg.getLatestRelease());
			p.setInstalled(StringUtils.isNotBlank(pkg.getInstalledVersion()));
			p.setType(PackageType.valueOf(pkg.getType().name()));
			fromDomain.getPackages().getPackage().add(p);
		}
		fromDomain.applyUriComponents(getUriBuilder());
		return ok(fromDomain);
	}

	@RequestMapping(value = "/repository/{name}/{package}", method = RequestMethod.GET)
	public ResponseEntity<Packages> getRepositoryPackages(@PathVariable("name") String name,
			@PathVariable("package") String packageName) {
		org.appng.core.model.Repository r = getCoreService().getApplicationRepositoryByName(name);
		if (null == r) {
			return notFound();
		}
		try {
			PackageVersions packageVersions = r.getPackageVersions(packageName);
			Identifier installedApp = getApplicationByName(packageName);
			Identifier installedTemplate = templateService.getTemplateByName(packageName);
			Packages packages = new Packages();
			for (PackageInfo pkg : packageVersions.getPackage()) {
				Package p = getPackage(installedApp, installedTemplate, pkg);
				packages.getPackage().add(p);
			}
			Comparator<org.appng.appngizer.model.xml.Package> propertyComparator = new PropertyComparator<org.appng.appngizer.model.xml.Package>(
					"timestamp", false, false);
			Collections.sort(packages.getPackage(), propertyComparator);
			packages.setSelf("/repository/" + packages.encode(name) + "/" + packageName);
			packages.applyUriComponents(getUriBuilder());
			return ok(packages);
		} catch (BusinessException e) {
			return notFound();
		}
	}

	protected Package getPackage(Identifier installedApp, Identifier installedTemplate, PackageInfo pkg) {
		Package p = Package.fromDomain(pkg, false);
		if (PackageType.APPLICATION.equals(p.getType())) {
			p.setInstalled(isInstalled(installedApp, pkg));
		} else {
			p.setInstalled(isInstalled(installedTemplate, pkg));
		}
		return p;
	}

	@RequestMapping(value = "/repository/{name}/{package}/{version}/{timestamp}", method = RequestMethod.GET)
	public ResponseEntity<Package> getRepositoryPackage(@PathVariable("name") String name,
			@PathVariable("package") String packageName, @PathVariable("version") String packageVersion,
			@PathVariable("timestamp") String packageTimestamp) {
		org.appng.core.model.Repository r = getCoreService().getApplicationRepositoryByName(name);
		if (null == r) {
			return notFound();
		}
		try {
			PackageArchive packageArchive = r.getPackageArchive(packageName, packageVersion, packageTimestamp);
			if (null == packageArchive) {
				return notFound();
			}
			Identifier installedApp = getApplicationByName(packageName);
			Identifier installedTemplate = templateService.getTemplateByName(packageName);
			Package pkg = getPackage(installedApp, installedTemplate, packageArchive.getPackageInfo());
			URI uri = getUriBuilder().path("/repository/{name}/{package}/{version}/{timestamp}")
					.buildAndExpand(name, pkg.getName(), pkg.getVersion(), pkg.getTimestamp()).toUri();
			pkg.setSelf(uri.toString());
			return ok(pkg);
		} catch (BusinessException e) {
			return notFound();
		}
	}

	@RequestMapping(value = "/repository/{name}/{package}/{version}/{timestamp}", method = RequestMethod.DELETE)
	public ResponseEntity<Packages> deleteRepositoryPackage(@PathVariable("name") String name,
			@PathVariable("package") String packageName, @PathVariable("version") String packageVersion,
			@PathVariable("timestamp") String packageTimestamp) throws BusinessException {
		RepositoryImpl r = (RepositoryImpl) getCoreService().getApplicationRepositoryByName(name);
		if (null == r) {
			return notFound();
		}
		if (r.isActive() && RepositoryType.LOCAL.equals(r.getRepositoryType())) {
			try {
				r.deletePackageVersion(packageName, packageVersion, packageTimestamp);
			} catch (BusinessException be) {
				return notFound();
			} catch (Exception e) {
				throw new BusinessException(e);
			}
			return getRepositoryPackages(name, packageName);
		} else {
			return reply(HttpStatus.METHOD_NOT_ALLOWED);
		}
	}

	protected boolean isInstalled(Identifier installed, PackageInfo pkg) {
		return null != installed && installed.getPackageVersion().equals(pkg.getVersion())
				&& installed.getTimestamp().equals(pkg.getTimestamp());
	}

	@RequestMapping(value = "/repository", method = RequestMethod.POST)
	public ResponseEntity<Repository> createRepository(@RequestBody org.appng.appngizer.model.xml.Repository repository)
			throws BusinessException, URISyntaxException {
		org.appng.core.model.Repository r = getCoreService().getApplicationRepositoryByName(repository.getName());
		if (null != r) {
			return conflict();
		}
		RepositoryImpl repo = Repository.toDomain(repository);
		RepositoryCacheFactory.validateRepositoryURI(repo);
		getCoreService().createRepository(repo);
		return created(getRepository(repository.getName()).getBody());
	}

	@RequestMapping(value = "/repository/{name}", method = RequestMethod.PUT)
	public ResponseEntity<Repository> updateRepository(@PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Repository repository) throws URISyntaxException {
		boolean nameChanged = nameChanged(repository, name);
		if (nameChanged) {
			RepositoryImpl existingRepo = (RepositoryImpl) getCoreService()
					.getApplicationRepositoryByName(repository.getName());
			if (null != existingRepo) {
				return conflict();
			}
		}
		RepositoryImpl r = (RepositoryImpl) getCoreService().getApplicationRepositoryByName(name);
		if (null == r) {
			return notFound();
		}
		r.setDescription(repository.getDescription());
		r.setUri(new URI(repository.getUri()));
		r.setName(repository.getName());
		r.setRepositoryMode(RepositoryMode.valueOf(repository.getMode().name()));
		r.setActive(repository.isEnabled());
		r.setStrict(repository.isStrict());
		r.setPublished(repository.isPublished());
		getCoreService().saveRepository(r);
		Repository fromDomain = Repository.fromDomain(r);
		fromDomain.applyUriComponents(getUriBuilder());
		if (nameChanged) {
			URI uri = getUriBuilder().path(fromDomain.getSelf()).build().toUri();
			return seeOther(uri);
		}
		return ok(fromDomain);
	}

	@RequestMapping(value = "/repository/{name}/install", method = RequestMethod.PUT)
	public ResponseEntity<Package> installPackage(@PathVariable("name") String name,
			@RequestBody org.appng.appngizer.model.xml.Package pkg) throws BusinessException {
		RepositoryImpl r = (RepositoryImpl) getCoreService().getApplicationRepositoryByName(name);
		if (null == r) {
			return notFound();
		}
		Properties platformCfg = getCoreService().getPlatformProperties();
		boolean isFileBased = platformCfg.getBoolean(Platform.Property.FILEBASED_DEPLOYMENT);
		PackageInfo installedPackage = getCoreService().installPackage(r.getId(), pkg.getName(), pkg.getVersion(),
				pkg.getTimestamp(), false, false, isFileBased);
		if (null == installedPackage) {
			return notFound();
		}

		return getRepositoryPackage(name, pkg.getName(), pkg.getVersion(), pkg.getTimestamp());
	}

	@RequestMapping(value = "/repository/{name}/upload", method = RequestMethod.POST)
	public ResponseEntity<Package> uploadPackage(@PathVariable("name") String name,
			@RequestParam("file") MultipartFile file) throws BusinessException {
		org.appng.core.model.Repository r = getCoreService().getApplicationRepositoryByName(name);
		if (null == r) {
			return notFound();
		}
		if (!RepositoryType.LOCAL.equals(r.getRepositoryType())) {
			return reply(HttpStatus.METHOD_NOT_ALLOWED);
		}
		try {
			File outFile = new File(new File(r.getUri()), file.getOriginalFilename());
			FileUtils.writeByteArrayToFile(outFile, file.getBytes());
			PackageArchive packageArchive = RepositoryUtils.getPackage(r, outFile, file.getOriginalFilename());
			if (null != packageArchive) {
				RepositoryCacheFactory.instance().getCache(r).reload();
				PackageInfo packageInfo = packageArchive.getPackageInfo();
				return getRepositoryPackage(name, packageInfo.getName(), packageInfo.getVersion(),
						packageInfo.getTimestamp());
			} else {
				FileUtils.deleteQuietly(outFile);
				return reply(HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			throw new BusinessException(e);
		}
	}

	@RequestMapping(value = "/repository/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteRepository(@PathVariable("name") String name) {
		RepositoryImpl repository = (RepositoryImpl) getCoreService().getApplicationRepositoryByName(name);
		if (null == repository) {
			return notFound();
		}
		getCoreService().deleteApplicationRepository(repository);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(getUriBuilder().path("/repository").build().toUri());
		return noContent(headers);
	}

	Logger logger() {
		return LOGGER;
	}
}
