package io.openliberty.tools.gradle;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import java.io.File
import java.io.FileInputStream

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

import org.junit.Assert
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList

public class TestLooseEarManifestClassPath extends AbstractIntegrationTest {

    static File resourceDir = new File('build/resources/test/loose-ear-manifest-classpath-test')
    static File buildDir = new File(integTestDir, '/loose-ear-manifest-classpath-test')
    static String buildFilename = 'build.gradle'

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    @Test
    public void test_loose_config_contains_manifest_classpath_project_dependency() {
        try {
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError('Fail on task deploy.', e)
        }

        File xml = new File('build/testBuilds/loose-ear-manifest-classpath-test/sample-ear/build/wlp/usr/servers/earServer/apps/sample-ear.ear.xml')
        Assert.assertTrue('Loose EAR config was not generated', xml.exists())

        FileInputStream input = new FileInputStream(xml)

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        factory.setIgnoringComments(true)
        factory.setCoalescing(true)
        factory.setIgnoringElementContentWhitespace(true)
        factory.setValidating(false)
        factory.setFeature('http://apache.org/xml/features/nonvalidating/load-dtd-grammar', false)
        factory.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

        DocumentBuilder builder = factory.newDocumentBuilder()
        Document doc = builder.parse(input)

        XPath xPath = XPathFactory.newInstance().newXPath()

        String expression = "/archive/archive[@targetInArchive='/sample-ejb.jar']/file[@targetInArchive='/META-INF/MANIFEST.MF']"
        NodeList manifestNodes = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET)
        Assert.assertEquals('Expected one manifest entry in sample-ejb module', 1, manifestNodes.getLength())

        expression = "/archive/archive[@targetInArchive='/sample-ejb.jar']/dir"
        NodeList dirNodes = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET)

        boolean foundSampleLibClasses = false
        for (Node node : dirNodes) {
            String sourceOnDisk = node.getAttributes().getNamedItem('sourceOnDisk').getNodeValue().replace('\\\\', '/')
            if (sourceOnDisk.contains('/sample-lib/build/classes/java/main')) {
                foundSampleLibClasses = true
                break
            }
        }

        Assert.assertTrue('Expected sample-lib classes to be added to sample-ejb module from manifest Class-Path', foundSampleLibClasses)
    }
}
