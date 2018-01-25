package org.appng.maven.plugin.mojo;

import org.junit.Test;

public class GenerateMessageConstantsMojoTest extends AbstractGenerateTest {

    @Test
    public void generationTest() throws Exception {
        maven()
               .withPomLocation("src/test/resources/poms/message-constants")
               .withGoal("generateMessageConstants")
               .withExpectedFile(
                       "target/generated-test-sources/constants/org/appng/test/constants/MessageConstants.java")
               .test();
    }
}