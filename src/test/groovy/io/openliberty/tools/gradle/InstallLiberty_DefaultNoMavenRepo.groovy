/*
 * (C) Copyright IBM Corporation 2019,2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.gradle



import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import org.junit.BeforeClass
import org.junit.Test

public class InstallLiberty_DefaultNoMavenRepo extends AbstractIntegrationTest {
    static File resourceDir = new File("build/resources/test/liberty-test")
    static File buildDir = new File(integTestDir, "/InstallLiberty_DefaultNoMavenRepo")
    static String buildFilename = "install_liberty_default_no_maven_repo.gradle"

    @BeforeClass
    public static void setup() {
        try {
            createDir(buildDir)
            createTestProject(buildDir, resourceDir, buildFilename)
        } catch (Exception e) {
            System.out.println("Error in setup: " + e.getMessage())
        }
    }

    @Test
    public void test_installLiberty_no_maven_repo_fail() {
        try {
            BuildResult result = GradleRunner.create()
                .withProjectDir(buildDir)
                .forwardOutput()
                .withArguments('installLiberty', '-i', '-s')
                .buildAndFail()

            String output = result.getOutput()
            assert (output.contains("org.gradle.internal.resolve.ModuleVersionNotFoundException") || 
                   output.contains("Cannot resolve external dependency") || 
                   output.contains("no repositories are defined")) : 
                  "Expected installLiberty to fail with repository-related exception"
        } catch (Exception e) {
            System.out.println("Error in test_installLiberty_no_maven_repo_fail: " + e.getMessage())
            throw e;
        }
    }
}
