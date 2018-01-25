package org.appng.maven.plugin.mojo;

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
    protected void createConstantClass() throws Exception {
        ApplicationPropertyConstantCreator.main(getArgs());
    }

    @Override
    protected String getMessage() {
        return "generate application constants";
    }

    @Override
    protected String[] getArgs() {
        return prefix == null
                ? new String[] { filePath.getAbsolutePath(), targetClass, outfolder.getAbsolutePath() }
                : new String[] { filePath.getAbsolutePath(), targetClass, outfolder.getAbsolutePath(), prefix };

    }

}
