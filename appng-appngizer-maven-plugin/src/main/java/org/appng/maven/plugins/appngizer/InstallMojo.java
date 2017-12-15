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

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			determineFile();
			login();
			getRepository();
			ResponseEntity<Package> uploaded = upload();

			if (uploaded.getStatusCode().equals(HttpStatus.OK)) {
				getLog().info("Success!");

				Package uploadPackage = uploaded.getBody();
				boolean isSiteSet = StringUtils.isNotBlank(site);
				if (uploadPackage.isInstalled()) {
					getLog().info(String.format("%s %s %s is already installed", uploadPackage.getName(),
							uploadPackage.getVersion(), uploadPackage.getTimestamp()));
					if (isSiteSet) {
						getLog().info(String.format("Skipping site reload for site %s", site));
					}
				} else {
					getLog().info(String.format("Installing %s %s %s", uploadPackage.getName(),
							uploadPackage.getVersion(), uploadPackage.getTimestamp()));
					HttpHeaders installHeader = getHeader();
					installHeader.setContentType(MediaType.APPLICATION_XML);
					ResponseEntity<Package> installPackage = send(uploadPackage, installHeader, HttpMethod.PUT,
							"repository/" + repository + "/install", Package.class);
					Package installedPackage = installPackage.getBody();

					if (HttpStatus.OK.equals(installPackage.getStatusCode())) {
						if (isSiteSet) {
							boolean doReload = true;
							if (activate) {

								getLog().info(
										"Activating application " + installedPackage.getName() + " for site " + site);
								ResponseEntity<Void> response = send(null, getHeader(), HttpMethod.POST,
										"site/" + site + "/application/" + installedPackage.getName(), Void.class,
										false);

								HttpStatus statusCode = response.getStatusCode();

								if (statusCode.is3xxRedirection()) {
									getLog().info("Activated application " + installedPackage.getName() + " for site "
											+ site);
								} else if (HttpStatus.METHOD_NOT_ALLOWED.equals(statusCode)) {
									getLog().info("Application " + installedPackage.getName()
											+ " already active for site " + site);
								} else {
									doReload = false;
									getLog().error("error installing package " + installedPackage.getName()
											+ ", return code was " + installPackage.getStatusCode());
								}

							}
							if (doReload) {
								getLog().info("Reloading site " + site);
								send(null, getHeader(), HttpMethod.PUT, "site/" + site + "/reload", Void.class);
							} else {
								getLog().info("NOT reloading site " + site);
							}
						} else {
							getLog().debug("no site set, skipping activation/reloading");
						}
					} else {
						getLog().error("error installing package " + uploadPackage.getName() + ", return code was "
								+ installPackage.getStatusCode());
					}
				}
			} else {
				getLog().error("error uploading package , return code was " + uploaded.getStatusCode());
			}

		} catch (URISyntaxException | InterruptedException | ExecutionException e) {
			throw new MojoExecutionException("error during upload", e);
		}

	}

}
