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
package org.appng.maven.plugin.mojo;

import java.io.IOException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.appng.tools.file.PropertyConstantCreator;

@Mojo(name = "generateMessageConstants", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class GenerateMessageConstantsMojo extends AbstractGeneratorMojo {

    @Override
    protected void createConstantClass() throws IOException {
        PropertyConstantCreator.main(getArgs());
    }

    @Override
    protected String getMessage() {
        return "generate message constants";
    }

    @Override
    protected String[] getArgs() {
        return new String[] { filePath.getAbsolutePath(), targetClass, outfolder.getAbsolutePath() };
    }

}
