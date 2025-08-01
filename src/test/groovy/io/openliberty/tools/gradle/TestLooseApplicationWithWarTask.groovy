package io.openliberty.tools.gradle;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import io.openliberty.tools.common.plugins.util.OSUtil

public class TestLooseApplicationWithWarTask extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/test/sample.servlet")
    static File buildDir = new File(integTestDir, "/test-loose-application-with-war-tasks")
    static String buildFilename = "testLooseApplicationWithWarTask.gradle"

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
    public void test_loose_config_file_exists() {
        try {
            runTasks(buildDir, 'deploy')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task deploy.", e)
        }
        assert new File('build/testBuilds/test-loose-application-with-war-tasks/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet1.war.xml').exists() : 'looseApplication config file was not copied over to the liberty runtime'
    }
/*
  Expected output to the XML
  <?xml version="1.0" encoding="UTF-8"?>
  <archive>
      <dir sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/test-loose-application-with-war-tasks/src/main/webapp" targetInArchive="/"/>
      <dir sourceOnDisk="/Users/../../ci.gradle/build/testBuilds/test-loose-application-with-war-tasks/build/classes" targetInArchive="/WEB-INF/classes"/>
      <file sourceOnDisk="/Users/../.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-text/1.13.1/.../commons-text-1.13.1.jar" targetInArchive="/WEB-INF/lib/commons-text-1.13.1.jar"/>
      <file sourceOnDisk="/Users/../.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-lang3/3.18.0/.../commons-lang3-3.18.0.jar" targetInArchive="/WEB-INF/lib/commons-lang3-3.18.0.jar"/>
  </archive>
*/
    @Test
    public void test_loose_config_file_contents_are_correct(){
      File on = new File("build/testBuilds/test-loose-application-with-war-tasks/build/wlp/usr/servers/LibertyProjectServer/apps/sample.servlet1.war.xml");
      FileInputStream input = new FileInputStream(on);

      // get input XML Document
      DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
      inputBuilderFactory.setIgnoringComments(true);
      inputBuilderFactory.setCoalescing(true);
      inputBuilderFactory.setIgnoringElementContentWhitespace(true);
      inputBuilderFactory.setValidating(false);
      inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
      inputBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)  
      DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
      Document inputDoc=inputBuilder.parse(input);

      // parse input XML Document
      XPath xPath = XPathFactory.newInstance().newXPath();
      String expression = "/archive/dir";
      NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <dir/> element ==>", 2, nodes.getLength());

      expression = "/archive/archive";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <archive/> element ==>", 0, nodes.getLength());

      expression = "/archive/file";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <file/> element ==>", 3, nodes.getLength());

      Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/commons-text-1.13.1.jar",
              nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue());

      // Check that dependencies are not located in the test build dir since copyLibsDirectory not set. They will be located in the gradle cache somewhere.
      String nodeValue = nodes.item(0).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();

      if (OSUtil.isWindows()) {
          Assert.assertTrue('archive sourceOnDisk attribute value not correct', nodeValue.endsWith("\\commons-text-1.13.1.jar") && !nodeValue.contains("\\test-loose-application-with-war-tasks\\"))
      } else {
          Assert.assertTrue('archive sourceOnDisk attribute value not correct', nodeValue.endsWith("/commons-text-1.13.1.jar") && !nodeValue.contains("/test-loose-application-with-war-tasks/"))
      }
      Assert.assertEquals("archive targetInArchive attribute value", "/WEB-INF/lib/commons-lang3-3.18.0.jar",
              nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue());

      // Check that dependencies are not located in the test build dir since copyLibsDirectory not set. They will be located in the gradle cache somewhere.
      nodeValue = nodes.item(1).getAttributes().getNamedItem("sourceOnDisk").getNodeValue();

      if (OSUtil.isWindows()) {
          Assert.assertTrue('archive sourceOnDisk attribute value not correct', nodeValue.endsWith("\\commons-lang3-3.18.0.jar") && !nodeValue.contains("\\test-loose-application-with-war-tasks\\"))
      } else {
          Assert.assertTrue('archive sourceOnDisk attribute value not correct', nodeValue.endsWith("/commons-lang3-3.18.0.jar") && !nodeValue.contains("/test-loose-application-with-war-tasks/"))
      }
    }

    @Test
    public void test_start_with_timeout_success() throws Exception{
      try {
          runTasks(buildDir, 'libertyStart')
      } catch (Exception e) {
          throw new AssertionError ("Fail on task libertyStart.", e)
      }
    }
}
