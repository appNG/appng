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

import java.io.File;
import java.net.URL;

import org.appng.maven.plugins.appngizer.InstallMojo;

public class RunAppNGizerMojo {

	public static void main(String[] args) throws Exception {
		InstallMojo mojo = new InstallMojo();
		mojo.sharedSecret = "ajNxN:y/c4E6UXtY";
		mojo.endpoint = new URL("http://localhost:8080/appNGizer/");
		mojo.repository = "Local";
		mojo.targetFolder = new File("...");
		mojo.targetFile = "artefact-name";
		mojo.site = "manager";
		mojo.activate = true;
		mojo.execute();
	}

}
