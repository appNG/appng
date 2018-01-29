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
package org.appng.maven.plugin.mojo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

abstract class AbstractGeneratorMojo extends AbstractMojo {

    /**
     * the path to the source file
     */
    @Parameter(property = "filePath", required = true)
    protected File filePath;

    /**
     * the fully qualified name of the target class to generate
     */
    @Parameter(property = "targetClass", required = true)
    protected String targetClass;

    /**
     * the output-folder for the generated class
     */
    @Parameter(property = "outFolder", defaultValue = "target/generated-sources/constants", required = false)
    protected File outfolder;

    /**
     * skips the execution
     */
    @Parameter(property = "skip", defaultValue = "false", required = false)
    protected boolean skip;

    @Component
    protected BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("skipping " + getMessage());
            return;
        }
        if (needsToBeExecuted()) {
            try {
                getLog().info(getMessage() + " for " + Arrays.toString(getArgs()));
                createConstantClass();
                buildContext.refresh(outfolder.getAbsoluteFile());
            } catch (Exception e) {
                buildContext.addMessage(filePath, 0, 0, "unable to " + getMessage(),
                        BuildContext.SEVERITY_ERROR, e);
                throw new MojoExecutionException("unable to " + getMessage(), e);
            }
        } else {
            getLog().debug("no creation needed: " + getMessage());
        }
        getLog().debug("delta: " + buildContext.hasDelta(filePath));
        getLog().debug("incremental: " + buildContext.isIncremental());
    }

    protected abstract void createConstantClass() throws IOException, JAXBException;

    protected abstract String getMessage();

    protected abstract String[] getArgs();

    protected boolean needsToBeExecuted() {
        return buildContext.hasDelta(filePath);
    }
}
