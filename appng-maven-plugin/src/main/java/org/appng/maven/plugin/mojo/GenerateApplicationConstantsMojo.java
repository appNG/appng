/*
 * Copyright 2011-2021 the original author or authors.
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
package org.appng.maven.plugin.mojo;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.appng.xml.ApplicationPropertyConstantCreator;

@Mojo(name = "generateApplicationConstants", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class GenerateApplicationConstantsMojo extends AbstractGeneratorMojo {

	/**
	 * a prefix for the name of the generated constants (optional)
	 */
	@Parameter(property = "prefix", defaultValue = "", required = false)
	private String prefix;

	@Override
	protected void createConstantClass() throws IOException, JAXBException {
		ApplicationPropertyConstantCreator.main(getArgs());
	}

	@Override
	protected String getMessage() {
		return "generate application constants";
	}

	@Override
	protected String[] getArgs() {
		return prefix == null ? new String[] { filePath.getAbsolutePath(), targetClass, outfolder.getAbsolutePath() }
				: new String[] { filePath.getAbsolutePath(), targetClass, outfolder.getAbsolutePath(), prefix };

	}

}
