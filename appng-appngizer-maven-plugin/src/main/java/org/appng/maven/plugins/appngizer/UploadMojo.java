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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "upload", defaultPhase = LifecyclePhase.PACKAGE)
public class UploadMojo extends AppNGizerMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			determineFile();
			login();
			getRepository();
			upload();
		} catch (URISyntaxException | InterruptedException | ExecutionException e) {
			throw new MojoExecutionException("error during upload", e);
		}

	}

}
