package org.appng.maven.plugin.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.appng.tools.file.PropertyConstantCreator;

@Mojo(name = "generateMessageConstants", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class GenerateMessageConstantsMojo extends AbstractGeneratorMojo {

    @Override
    protected void createConstantClass() throws Exception {
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
