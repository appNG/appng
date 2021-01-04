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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;

public abstract class AbstractGenerateTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    Maven maven() {
        return new Maven(mojoRule);
    }

    public static class Maven {

        private final MojoRule mojoRule;

        private Maven(MojoRule mojoRule) {
            this.mojoRule = mojoRule;
        }

        private File expectedFile;
        private File pom;
        private String goal;

        public Maven withExpectedFile(String file) {
            expectedFile = new File(file);
            return this;
        }

        public Maven withPomLocation(String location) {
            pom = new File(location);
            return this;
        }

        public Maven withGoal(String goal) {
            this.goal = goal;
            return this;
        }

        public void test() throws Exception {
            testPreconditions();

            if (expectedFile.exists()) {
                expectedFile.delete();
            }
            AbstractMojo mojo = (AbstractMojo) mojoRule.lookupConfiguredMojo(
                    pom, goal);
            assertThat(mojo).isNotNull();
            mojo.execute();
            assertThat(expectedFile.exists()).isTrue();
        }

        private void testPreconditions() {
            notNull("pom must be given", pom);
            isTrue("pom must exist", pom.exists());
            notNull("expected file must be given", expectedFile);
            notNull("goal must be given", goal);
            isTrue("goal must not be blank or empty", goal.trim().length() > 0);
        }

        private void notNull(String message, Object object) {
            isTrue(message, object != null);
        }

        private void isTrue(String message, boolean condition) {
            if (!condition) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
