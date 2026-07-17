/*
 * (C) Copyright IBM Corporation 2025.
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
package io.openliberty.tools.gradle;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileWriter;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * Verifies that dev mode hot-reload compilation succeeds when custom
 * {@code compilerArgs} are set on the {@code compileJava} task.
 *
 * <p>The test project sets:
 * <pre>
 *   tasks.withType(JavaCompile) {
 *       options.compilerArgs = ['-parameters', '-Xlint:-processing']
 *   }
 * </pre>
 * These args are non-breaking and must be passed through to the hot-reload
 * recompiler. The test verifies that compilation still succeeds after a
 * source change, confirming the args do not interfere with dev mode.
 */
class DevCompilerArgsTest extends BaseDevTest {
    static final String projectName = "dev-mode-compiler-args-test";

    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);
    static File buildDir = new File(integTestDir, "dev-test/" + projectName + System.currentTimeMillis());

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        createDir(buildDir);
        createTestProject(buildDir, resourceDir, buildFilename);
        // No --generateFeatures; this test focuses on compilerArgs compilation
        runDevMode(null, buildDir);
    }

    /**
     * Verify the server started correctly with the custom compilerArgs project.
     */
    @Test
    public void testCompilerArgsServerStartup() throws Exception {
        assertTrue("Web application did not become available — " +
                   "compilerArgs may have broken startup compilation",
                verifyLogMessage(30000, WEB_APP_AVAILABLE, errFile));
    }

    /**
     * Trigger a hot-reload by modifying {@code HelloServlet.java} and verify that
     * {@code COMPILATION_SUCCESSFUL} appears in the dev mode log.
     * This confirms custom compilerArgs read from the Gradle build are compatible
     * with the dev mode recompiler and do not cause the build to fail.
     */
    @Test
    public void testCompilerArgsHotReloadCompilation() throws Exception {
        assertTrue("Server did not start before hot-reload test",
                verifyLogMessage(60000, "Liberty is running in dev mode."));
        assertTrue("Web application did not become available before hot-reload test",
                verifyLogMessage(30000, WEB_APP_AVAILABLE, errFile));

        File helloServlet = new File(buildDir, "src/main/java/com/demo/HelloServlet.java");
        assertTrue("HelloServlet.java source file not found", helloServlet.exists());

        File helloClass = new File(targetDir, "classes/java/main/com/demo/HelloServlet.class");
        assertTrue("HelloServlet.class not found after initial compile", helloClass.exists());

        int compilationCount = countOccurrences(COMPILATION_SUCCESSFUL, logFile);

        waitLongEnough();
        long lastModified = helloClass.lastModified();

        // Append a benign comment to trigger a hot-reload recompile
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(helloServlet, true));
            writer.append("\n// trigger hot reload");
        } finally {
            if (writer != null) writer.close();
        }

        assertTrue("HelloServlet.class was not recompiled after source change",
                waitForCompilation(helloClass, lastModified, 30000));
        assertTrue("Compilation did not succeed after hot reload — " +
                   "compilerArgs may be incompatible with dev mode recompiler",
                verifyLogMessage(30000, COMPILATION_SUCCESSFUL, ++compilationCount));
        assertFalse("Unexpected compilation error with custom compilerArgs",
                verifyLogMessage(5000, COMPILATION_ERRORS));
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        String stdout = getContents(logFile, "Dev mode std output");
        System.out.println(stdout);
        String stderr = getContents(errFile, "Dev mode std error");
        System.out.println(stderr);
        cleanUpAfterClass(true);
    }
}
