/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.maven.plugins.appngizer;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.appng.appngizer.model.xml.Package;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Mojo(name = "install", defaultPhase = LifecyclePhase.PACKAGE)
public class InstallMojo extends AppNGizerMojo {

	@Parameter(property = "site")
	protected String site;

	@Parameter(property = "activate")
	protected boolean activate = false;

	@Parameter(property = "privileged")
	protected boolean privileged = false;

	@Parameter(property = "hidden")
	protected boolean hidden = false;

	@Parameter(property = "installDuringUpload")
	protected boolean installDuringUpload = false;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			determineFile();
			login();
			getRepository();

			ResponseEntity<Package> uploaded = upload(installDuringUpload, privileged, hidden);

			boolean doReload = true;
			HttpStatus status = uploaded.getStatusCode();
			boolean packageOk = status.equals(HttpStatus.OK);
			boolean isSiteSet = StringUtils.isNotBlank(site);

			if (packageOk) {
				Package uploadPackage = uploaded.getBody();
				String packageName = uploadPackage.getName();
				if (installDuringUpload) {
					packageOk = true;
				} else {
					if (uploadPackage.isInstalled()) {
						getLog().info(String.format("%s %s %s is already installed", packageName,
								uploadPackage.getVersion(), uploadPackage.getTimestamp()));
						if (isSiteSet) {
							doReload = false;
						}
					} else {
						HttpHeaders installHeader = getHeader();
						installHeader.setContentType(MediaType.APPLICATION_XML);
						uploadPackage.setPrivileged(privileged);
						uploadPackage.setHidden(hidden);
						ResponseEntity<Package> installPackage = send(uploadPackage, installHeader, HttpMethod.PUT,
								String.format("repository/%s/install", repository), Package.class);
						packageOk = HttpStatus.OK.equals(installPackage.getStatusCode());
					}
				}
				if (packageOk) {
					getLog().info(String.format("Installed %s %s %s (privileged: %s, hidden: %s)", packageName,
							uploaded.getBody().getVersion(), uploaded.getBody().getTimestamp(), privileged, hidden));
					if (isSiteSet && activate) {

						ResponseEntity<Void> activatePackage = send(null, getHeader(), HttpMethod.POST,
								String.format("site/%s/application/%s", site, packageName), Void.class, false);

						status = activatePackage.getStatusCode();
						if (status.is3xxRedirection()) {
							getLog().info(String.format("Activated application %s for site %s", packageName, site));
						} else if (HttpStatus.METHOD_NOT_ALLOWED.equals(status)) {
							getLog().info(
									String.format("Application %s already active for site %s", packageName, site));
						} else {
							doReload = false;
							getLog().error(String.format("error installing package %s, return code was %s", packageName,
									status));
						}

					} else {
						getLog().debug("no site set, skipping activation/reloading");
					}
				} else {
					getLog().error(
							String.format("error installing package %s, return code was %s", packageName, status));
				}

				if (doReload) {
					getLog().info(String.format("Reloading site %s", site));
					send(null, getHeader(), HttpMethod.PUT, String.format("site/%s/reload", site), Void.class);
				} else {
					getLog().info(String.format("NOT reloading site %s", site));
				}
			} else {
				getLog().error(String.format(
						"error " + (installDuringUpload ? "installing" : "uploading") + " package , return code was %s",
						uploaded.getStatusCode()));
			}

		} catch (URISyntaxException | ExecutionException e) {
			throw new MojoExecutionException("error during upload", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MojoExecutionException("upload was interrupted", e);
		}

	}

}
