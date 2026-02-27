package io.openliberty.tools.gradle

import org.gradle.testkit.runner.BuildResult
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.assertTrue

class InstallFeatureToolchainTest extends AbstractIntegrationTest {
    
    static File resourceDir = new File("build/resources/test/kernel-install-feature-test")
    static File buildDir = new File(integTestDir, "/install-feature-toolchain-test")
    static String buildFilename = "install_features_dependencies.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
        
        // Modify build.gradle to add java plugin and toolchain configuration
        File buildFile = new File(buildDir, "build.gradle")
        String originalContent = buildFile.text
        
        // Insert java plugin and toolchain configuration after the liberty plugin line
        String modifiedContent = originalContent.replace(
            "apply plugin: 'liberty'",
            "apply plugin: 'liberty'\napply plugin: 'java'\n\njava {\n    toolchain {\n        languageVersion = JavaLanguageVersion.of(17)\n    }\n}"
        )
        buildFile.text = modifiedContent
    }
    
    @Before
    public void before() {
        runTasks(buildDir, "libertyCreate")
        copyServer("server_empty.xml")
        deleteDir(new File(buildDir, "build/wlp/lib/features"))
    }
    
    @Test
    public void testInstallFeatureWithToolchain() {
        BuildResult result = runTasksResult(buildDir, "installFeature")
        
        String output = result.getOutput()

        assertTrue("Should show toolchain configured message for installFeature task",
                output.contains(String.format(TOOLCHAIN_CONFIGURED, "installFeature")))
    }
    
    private void copyServer(String serverXmlFile) {
        File serverXml = new File(buildDir, "build/wlp/usr/servers/defaultServer/server.xml")
        File sourceServerXml = new File(resourceDir, serverXmlFile)
        copyFile(sourceServerXml, serverXml)
    }
}
