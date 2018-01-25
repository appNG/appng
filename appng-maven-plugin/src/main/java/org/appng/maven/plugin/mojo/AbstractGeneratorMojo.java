package org.appng.maven.plugin.mojo;

import java.io.File;
import java.util.Arrays;

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

    protected abstract void createConstantClass() throws Exception;

    protected abstract String getMessage();

    protected abstract String[] getArgs();

    protected boolean needsToBeExecuted() {
        return buildContext.hasDelta(filePath);
    }
}
