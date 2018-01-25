package org.appng.maven.plugin.mojo;

import org.junit.Test;

public class GenerateApplicationConstantsMojoTest extends AbstractGenerateTest {

    @Test
    public void generationTest() throws Exception {
        maven()
               .withPomLocation("src/test/resources/poms/application-constants")
               .withGoal("generateApplicationConstants")
               .withExpectedFile(
                       "target/generated-test-sources/constants/org/appng/test/constants/ApplicationConstants.java")
               .test();
    }
}