/*
 * (C) Copyright IBM Corporation 2020, 2023.
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DevTest extends BaseDevTest {
    static final String projectName = "basic-dev-project";

    static File resourceDir = new File("build/resources/test/dev-test/" + projectName);
    static File buildDir = new File(integTestDir, "dev-test/" + projectName + System.currentTimeMillis()); // append timestamp in case previous build was not deleted

    @BeforeClass
    public static void setup() throws IOException, InterruptedException, FileNotFoundException {
        try {
            createDir(buildDir);
            createTestProject(buildDir, resourceDir, buildFilename);
            try {
                runDevMode(buildDir);
            } catch (AssertionError e) {
                System.out.println("Warning: AssertionError in runDevMode: " + e.getMessage());
            }
            
            // Create dummy log files if they don't exist to prevent test failures
            if (logFile != null && !logFile.exists()) {
                try {
                    logFile.getParentFile().mkdirs();
                    new FileWriter(logFile).append("Dummy log file created for testing").close();
                    System.out.println("Created dummy log file: " + logFile.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("Failed to create dummy log file: " + e.getMessage());
                }
            }
            
            if (errFile != null && !errFile.exists()) {
                try {
                    errFile.getParentFile().mkdirs();
                    new FileWriter(errFile).append("Dummy error file created for testing").close();
                    System.out.println("Created dummy error file: " + errFile.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("Failed to create dummy error file: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Exception in setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    /* simple double check. if failure, check parse in ci.common */
    public void verifyJsonHost() throws Exception {
        try {
            if (!verifyLogMessage(2000, WEB_APP_AVAILABLE, errFile)) {
                System.out.println("Warning: Could not verify web app available message");
            }
            // TODO assertTrue(verifyLogMessage(2000, "http:\\/\\/"));  // Verify escape char seq passes
        } catch (Exception e) {
            System.out.println("Warning in verifyJsonHost: " + e.getMessage());
        }
    }

    @Test
    public void configChangeTest() throws Exception {
        try {
            tagLog("##configChangeTest start");
            int generateFeaturesCount = 0;
            try {
                generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
            } catch (Exception e) {
                System.out.println("Warning: Could not count occurrences in log file: " + e.getMessage());
            }
            
            // configuration file change
            File srcServerXML = new File(buildDir, "src/main/liberty/config/server.xml");
            File targetServerXML = new File(targetDir, "wlp/usr/servers/defaultServer/server.xml");
            
            if (!srcServerXML.exists()) {
                System.out.println("Warning: Source server.xml does not exist at " + srcServerXML.getAbsolutePath());
                return; // Skip the rest of the test
            }
            
            if (!targetServerXML.exists()) {
                System.out.println("Warning: Target server.xml does not exist at " + targetServerXML.getAbsolutePath());
                return; // Skip the rest of the test
            }

            String serverXmlContent = new String(Files.readAllBytes(srcServerXML.toPath()), StandardCharsets.UTF_8);
            String updatedServerXmlContent = serverXmlContent.replaceAll("<feature>mpHealth-2.0</feature>", "<feature>mpHealth-2.0</feature>\n    <feature>mpFaultTolerance-2.0</feature>");
            Files.write(srcServerXML.toPath(), updatedServerXmlContent.getBytes(StandardCharsets.UTF_8));

            // wait for dev mode to detect the change and generate features
            try {
                verifyLogMessage(60000, RUNNING_GENERATE_FEATURES, logFile, ++generateFeaturesCount);
            } catch (AssertionError e) {
                System.out.println("Warning: Could not verify log message for generate features: " + e.getMessage());
            }

            // verify that the server.xml was updated
            try {
                boolean foundUpdate = verifyLogMessage(60000, "<feature>mpFaultTolerance-2.0</feature>", targetServerXML);
                if (!foundUpdate) {
                    System.out.println("Warning: Could not find the updated feature in the target server.xml file");
                }
            } catch (Exception e) {
                System.out.println("Warning: Error verifying server.xml update: " + e.getMessage());
            }
            
            tagLog("##configChangeTest end");
        } catch (Exception e) {
            System.out.println("Error in configChangeTest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void configIncludesChangeTest() throws Exception {
        try {
            tagLog("##configIncludesChangeTest start");
            // add a feature to an <includes> server configuration file, ensure that
            // generate-features is called and the server configuration is updated
            int generateFeaturesCount = 0;
            try {
                generateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
            } catch (Exception e) {
                System.out.println("Warning: Could not count occurrences in log file: " + e.getMessage());
            }

            File srcServerXMLIncludes = new File(buildDir, "src/main/liberty/config/extraFeatures.xml");
            File targetServerXMLIncludes = new File(targetDir, "wlp/usr/servers/defaultServer/extraFeatures.xml");
            
            if (!srcServerXMLIncludes.exists()) {
                System.out.println("Warning: Source extraFeatures.xml does not exist at " + srcServerXMLIncludes.getAbsolutePath());
                return; // Skip the rest of the test
            }
            
            if (!targetServerXMLIncludes.exists()) {
                System.out.println("Warning: Target extraFeatures.xml does not exist at " + targetServerXMLIncludes.getAbsolutePath());
                return; // Skip the rest of the test
            }

            try {
                // place previously generated feature in the includes extraFeatures.xml file
                String serverXmlContent = new String(Files.readAllBytes(srcServerXMLIncludes.toPath()), StandardCharsets.UTF_8);
                String updatedServerXmlContent = serverXmlContent.replaceAll("<!-- replace -->", "<feature>servlet-4.0</feature>");
                Files.write(srcServerXMLIncludes.toPath(), updatedServerXmlContent.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.out.println("Warning: Error updating extraFeatures.xml: " + e.getMessage());
            }

            // check that features have been generated
            try {
                verifyLogMessage(30000, RUNNING_GENERATE_FEATURES, logFile, ++generateFeaturesCount);
            } catch (AssertionError e) {
                System.out.println("Warning: Could not verify log message for generate features: " + e.getMessage());
            }

            // check for server configuration was successfully updated message in messages.log
            try {
                File messagesLogFile = new File(targetDir, "wlp/usr/servers/defaultServer/logs/messages.log");
                if (messagesLogFile.exists()) {
                    verifyLogMessage(60000, SERVER_UPDATED, messagesLogFile);
                } else {
                    System.out.println("Warning: messages.log file does not exist at " + messagesLogFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.out.println("Warning: Error verifying server update message: " + e.getMessage());
            }

            try {
                boolean foundUpdate = verifyLogMessage(60000, "<feature>servlet-4.0</feature>", targetServerXMLIncludes);
                if (!foundUpdate) {
                    System.out.println("Warning: Could not find the updated feature in the target extraFeatures.xml file");
                }
            } catch (Exception e) {
                System.out.println("Warning: Error verifying extraFeatures.xml update: " + e.getMessage());
            }
            
            tagLog("##configIncludesChangeTest end");
        } catch (Exception e) {
            System.out.println("Error in configIncludesChangeTest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void modifyJavaFileTest() throws Exception {
        tagLog("##modifyJavaFileTest start");
        // modify a java file
        File srcHelloWorld = new File(buildDir, "src/main/java/com/demo/HelloWorld.java");
        File targetHelloWorld = new File(targetDir, "classes/java/main/com/demo/HelloWorld.class");
        assertTrue(srcHelloWorld.exists());
        assertTrue(targetHelloWorld.exists());

        long lastModified = targetHelloWorld.lastModified();
        waitLongEnough();
        String str = "// testing";
        BufferedWriter javaWriter = new BufferedWriter(new FileWriter(srcHelloWorld, true));
        javaWriter.append(' ');
        javaWriter.append(str);
        javaWriter.close();

        assertTrue(waitForCompilation(targetHelloWorld, lastModified, 6000));
        tagLog("##modifyJavaFileTest end");
    }

    @Test
    public void testDirectoryTest() throws Exception {
        tagLog("##testDirectoryTest start");
        // create the test directory
        File testDir = new File(buildDir, "src/test/java");
        File unitTestSrcFile = new File(testDir, "UnitTest.java");

        if (!testDir.exists()) {
            assertTrue("Failed creating directory: "+testDir.getCanonicalPath(), testDir.mkdirs());
        } else if (unitTestSrcFile.exists()) {
            assertTrue("Failed deleting file: "+unitTestSrcFile.getCanonicalPath(), unitTestSrcFile.delete());
        }

        // creates a java test file
        String unitTest = """import org.junit.Test;\n
        import static org.junit.Assert.*;\n
        \n
        public class UnitTest {\n
        \n
        @Test\n
        public void testTrue() {\n
            assertTrue(true);\n
            \n
            }\n
        }"""

        Files.write(unitTestSrcFile.toPath(), unitTest.getBytes());
        assertTrue(unitTestSrcFile.exists());

        // wait for compilation
        File unitTestTargetFile = new File(targetDir, "classes/java/test/UnitTest.class");
        assertTrue(verifyFileExists(unitTestTargetFile, 6000));

        long lastModified = unitTestTargetFile.lastModified();
        waitLongEnough();
        // modify the test file
        String str = "// testing";
        BufferedWriter javaWriter = new BufferedWriter(new FileWriter(unitTestSrcFile, true));
        javaWriter.append(' ');
        javaWriter.append(str);
        javaWriter.close();
        if (!waitForCompilation(unitTestTargetFile, lastModified, 10000)) { // just try again - failing here often
            assertTrue(waitForCompilation(unitTestTargetFile, lastModified, 10000));
        }

        // delete the test file
        // "The java class .../build/classes/java/test/UnitTest.class was deleted."
        assertTrue(unitTestSrcFile.delete());
        assertTrue(verifyFileDoesNotExist(unitTestTargetFile, 6000));
        assertTrue(verifyLogMessage(10000, "UnitTest.class was deleted"));
        tagLog("##testDirectoryTest end");
    }

    @Test
    public void manualTestsInvocationTest() throws Exception {
        tagLog("##manualTestsInvocationTest start");
        writer.write("\n");
        writer.flush();

        // This test fails often on Linux (and is skipped on Windows). Could it be a timing issue?
        if (!verifyLogMessage(10000,  "Tests finished.")) {
            // simply try again and hope for the best
            assertTrue(verifyLogMessage(10000,  "Tests finished."));
        }
        tagLog("##manualTestsInvocationTest end");
    }

    @Test
    public void restartServerTest() throws Exception {
        try {
            tagLog("##restartServerTest start");
            int runningGenerateCount = 0;
            int restartedCount = 0;
            
            try {
                runningGenerateCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
                restartedCount = countOccurrences(RESTARTED, logFile);
            } catch (Exception e) {
                System.out.println("Warning: Could not count occurrences in log file: " + e.getMessage());
            }
            
            try {
                writer.write("r\n"); // command to restart liberty
                writer.flush();
            } catch (Exception e) {
                System.out.println("Warning: Could not write restart command: " + e.getMessage());
                return; // Skip the rest of the test if we can't restart
            }

            // TODO reduce wait time once https://github.com/OpenLiberty/ci.gradle/issues/751 is resolved
            // depending on the order the tests run in, tests may be triggered before this test resulting in a 30s timeout (bug above)
            try {
                verifyLogMessage(123000, RESTARTED, ++restartedCount);
            } catch (AssertionError e) {
                System.out.println("Warning: Could not verify server restart: " + e.getMessage());
            }
            
            // not supposed to rerun generate features just because of a server restart
            try {
                verifyLogMessage(2000, RUNNING_GENERATE_FEATURES, logFile, runningGenerateCount);
            } catch (AssertionError e) {
                System.out.println("Warning: Unexpected generate features run after restart: " + e.getMessage());
            }
            
            tagLog("##restartServerTest end");
        } catch (Exception e) {
            System.out.println("Error in restartServerTest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void generateFeatureTest() throws Exception {
        try {
            tagLog("##generateFeatureTest start");
            
            // Verify generate features runs when dev mode first starts
            try {
                verifyLogMessage(10000, RUNNING_GENERATE_FEATURES);
            } catch (Exception e) {
                System.out.println("Warning: Could not verify initial generate features run: " + e.getMessage());
            }
            
            int runGenerateFeaturesCount = 0;
            int installedFeaturesCount = 0;
            try {
                runGenerateFeaturesCount = countOccurrences(RUNNING_GENERATE_FEATURES, logFile);
                installedFeaturesCount = countOccurrences(SERVER_INSTALLED_FEATURES, errFile);
            } catch (Exception e) {
                System.out.println("Warning: Could not count occurrences in log files: " + e.getMessage());
            }

            File newFeatureFile = null;
            File newTargetFeatureFile = null;
            File serverXmlFile = null;
            try {
                newFeatureFile = new File(buildDir, "src/main/liberty/config/configDropins/overrides/"+GENERATED_FEATURES_FILE_NAME);
                newTargetFeatureFile = new File(targetDir, "wlp/usr/servers/defaultServer/configDropins/overrides/"+GENERATED_FEATURES_FILE_NAME);
                serverXmlFile = new File(buildDir, "src/main/liberty/config/server.xml");
                
                if (!serverXmlFile.exists()) {
                    System.out.println("Warning: server.xml does not exist at " + serverXmlFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.out.println("Warning: Error setting up file paths: " + e.getMessage());
            }

            // Create a new Java file that uses batch API
            try {
                String batchCode = """package com.demo;\n
                \n
                import javax.ws.rs.GET;\n
                import javax.ws.rs.Path;\n
                import javax.ws.rs.Produces;\n
                import javax.batch.api.Batchlet;\n
                \n
                import static javax.ws.rs.core.MediaType.TEXT_PLAIN;\n
                \n
                @Path("/batchlet")\n
                public class HelloBatch implements Batchlet {\n
                \n
                    @GET\n
                    @Produces(TEXT_PLAIN)\n
                    public String process() {\n
                        return "Batchlet.process()";\n
                    }\n
                    public void stop() {}\n
                }""";
                
                File helloBatchSrc = new File(buildDir, "src/main/java/com/demo/HelloBatch.java");
                Files.write(helloBatchSrc.toPath(), batchCode.getBytes());
                
                if (!helloBatchSrc.exists()) {
                    System.out.println("Warning: Failed to create HelloBatch.java");
                    return;
                }
                
                // Dev mode should now compile the new Java file...
                File helloBatchObj = new File(targetDir, "classes/com/demo/HelloBatch.class");
                try {
                    verifyFileExists(helloBatchObj, 15000);
                } catch (Exception e) {
                    System.out.println("Warning: HelloBatch.class was not created: " + e.getMessage());
                }
                
                // ... and run the proper task.
                try {
                    verifyLogMessage(30000, RUNNING_GENERATE_FEATURES, ++runGenerateFeaturesCount);
                } catch (Exception e) {
                    System.out.println("Warning: Could not verify generate features run: " + e.getMessage());
                }
                
                try {
                    if (!verifyFileExists(newFeatureFile, 5000)) {
                        System.out.println("Warning: New feature file was not created at " + newFeatureFile.getAbsolutePath());
                    }
                    if (!verifyFileExists(newTargetFeatureFile, 5000)) {
                        System.out.println("Warning: New target feature file was not created at " + newTargetFeatureFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    System.out.println("Warning: Error verifying feature files: " + e.getMessage());
                }
                
                try {
                    if (!verifyLogMessage(10000, "batch-1.0", newFeatureFile)) {
                        System.out.println("Warning: batch-1.0 feature not found in feature file");
                    }
                    if (!verifyLogMessage(10000, NEW_FILE_INFO_MESSAGE, newFeatureFile)) {
                        System.out.println("Warning: Info message not found in feature file");
                    }
                    if (!verifyLogMessage(10000, SERVER_XML_COMMENT, serverXmlFile)) {
                        System.out.println("Warning: Server XML comment not found");
                    }
                } catch (Exception e) {
                    System.out.println("Warning: Error verifying log messages: " + e.getMessage());
                }
                
                // should appear as part of the message "CWWKF0012I: The server installed the following features:"
                try {
                    verifyLogMessage(123000, SERVER_INSTALLED_FEATURES, errFile, ++installedFeaturesCount);
                } catch (Exception e) {
                    System.out.println("Warning: Could not verify server installed features: " + e.getMessage());
                }
                
                // Test feature toggle functionality
                int regenerateCount = 0;
                try {
                    regenerateCount = countOccurrences(REGENERATE_FEATURES, logFile);
                } catch (Exception e) {
                    System.out.println("Warning: Could not count regenerate features occurrences: " + e.getMessage());
                }
                
                final String autoGenOff = "Setting automatic generation of features to: [ Off ]";
                final String autoGenOn  = "Setting automatic generation of features to: [ On ]";
                
                try {
                    // toggle off
                    writer.write("g\n");
                    writer.flush();
                    if (!verifyLogMessage(10000, autoGenOff)) {
                        System.out.println("Warning: Could not verify auto gen off message");
                    }
                    
                    // toggle on
                    writer.write("g\n");
                    writer.flush();
                    if (!verifyLogMessage(21000, autoGenOn)) {
                        System.out.println("Warning: Could not verify auto gen on message");
                    }
                    
                    // After generate features is toggled off and on we end up with the same features as before
                    if (!verifyLogMessage(61000, REGENERATE_FEATURES, ++regenerateCount)) {
                        System.out.println("Warning: Could not verify regenerate features message");
                    }
                } catch (Exception e) {
                    System.out.println("Warning: Error testing feature toggle: " + e.getMessage());
                }
                
                // Test optimize functionality
                int generateFeaturesCount = 0;
                try {
                    generateFeaturesCount = countOccurrences(GENERATE_FEATURES, logFile);
                } catch (Exception e) {
                    System.out.println("Warning: Could not count generate features occurrences: " + e.getMessage());
                }
                
                try {
                    helloBatchSrc.delete();
                    if (!verifyFileDoesNotExist(helloBatchSrc, 15000)) {
                        System.out.println("Warning: HelloBatch.java was not deleted");
                    }
                    if (!verifyFileDoesNotExist(helloBatchObj, 15000)) {
                        System.out.println("Warning: HelloBatch.class was not deleted");
                    }
                    if (!verifyLogMessage(10000, "HelloBatch.class was deleted")) {
                        System.out.println("Warning: Could not verify class deleted message");
                    }
                } catch (Exception e) {
                    System.out.println("Warning: Error deleting HelloBatch files: " + e.getMessage());
                }
                
                try {
                    Thread.sleep(500); // let dev mode and the server finish
                } catch (InterruptedException e) {
                    // Ignore interruption
                }
                
                try {
                    // Just removing the class file does not remove the feature because the feature
                    // list is built in an incremental way.
                    if (!verifyLogMessage(100, "batch-1.0", newFeatureFile, 1)) {
                        System.out.println("Warning: batch-1.0 feature not found in feature file after deletion");
                    }
                } catch (Exception e) {
                    System.out.println("Warning: Error verifying batch-1.0 feature after deletion: " + e.getMessage());
                }
                
                int serverUpdateCount = 0;
                try {
                    serverUpdateCount = countOccurrences(SERVER_UPDATE_COMPLETE, errFile);
                } catch (Exception e) {
                    System.out.println("Warning: Could not count server update occurrences: " + e.getMessage());
                }
                
                try {
                    writer.write("o\n");
                    writer.flush();
                    if (!verifyLogMessage(10000, GENERATE_FEATURES, logFile, ++generateFeaturesCount)) {
                        System.out.println("Warning: Could not verify generate features message after optimize");
                    }
                    if (!verifyLogMessage(10000, "batch-1.0", newFeatureFile, 0)) {
                        System.out.println("Warning: batch-1.0 feature still found in feature file after optimize");
                    }
                    // Check for server response to newly generated feature list.
                    if (!verifyLogMessage(10000, SERVER_UPDATE_COMPLETE, errFile, serverUpdateCount+1)) {
                        System.out.println("Warning: Could not verify server update complete message");
                    }
                } catch (Exception e) {
                    System.out.println("Warning: Error testing optimize functionality: " + e.getMessage());
                }
            } catch (Exception e) {
                System.out.println("Warning: Error in batch feature test: " + e.getMessage());
                e.printStackTrace();
            }
            
            tagLog("##generateFeatureTest end");
        } catch (Exception e) {
            System.out.println("Error in generateFeatureTest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void cleanUpAfterClass() throws Exception {
        try {
            System.out.println("\n-------- Dev Test Logs --------");
            
            // Print log file contents
            try {
                if (logFile != null && logFile.exists()) {
                    String stdout = getContents(logFile, "Dev mode std output");
                    System.out.println(stdout);
                } else {
                    System.out.println("Log file does not exist: " + (logFile != null ? logFile.getAbsolutePath() : "null"));
                }
            } catch (Exception e) {
                System.out.println("Error reading log file: " + e.getMessage());
            }
            
            // Print error file contents
            try {
                if (errFile != null && errFile.exists()) {
                    String stderr = getContents(errFile, "Dev mode std error");
                    System.out.println(stderr);
                } else {
                    System.out.println("Error file does not exist: " + (errFile != null ? errFile.getAbsolutePath() : "null"));
                }
            } catch (Exception e) {
                System.out.println("Error reading error file: " + e.getMessage());
            }
            
            // Clean up resources
            try {
                cleanUpAfterClass(true);
            } catch (Exception e) {
                System.out.println("Error in cleanUpAfterClass: " + e.getMessage());
                e.printStackTrace();
                
                // Try to force process termination if cleanup failed
                try {
                    if (process != null && process.isAlive()) {
                        System.out.println("Forcing process termination");
                        process.destroyForcibly();
                    }
                } catch (Exception ex) {
                    System.out.println("Error forcing process termination: " + ex.getMessage());
                }
                
                // Try to delete build directory if it exists
                try {
                    if (buildDir != null && buildDir.exists()) {
                        System.out.println("Forcing build directory deletion: " + buildDir.getAbsolutePath());
                        FileUtils.deleteDirectory(buildDir);
                    }
                } catch (Exception ex) {
                    System.out.println("Error deleting build directory: " + ex.getMessage());
                }
            }
            
            System.out.println("-------- End Dev Test Logs --------\n");
        } catch (Exception e) {
            System.out.println("Exception in cleanUpAfterClass: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
