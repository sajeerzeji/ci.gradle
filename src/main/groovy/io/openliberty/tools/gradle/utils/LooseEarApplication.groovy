/**
 * (C) Copyright IBM Corporation 2018, 2026
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
package io.openliberty.tools.gradle.utils

import io.openliberty.tools.common.plugins.config.LooseApplication
import io.openliberty.tools.common.plugins.config.LooseConfigData
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger
import org.gradle.plugins.ear.Ear
import org.w3c.dom.Element

import java.util.jar.Attributes
import java.util.jar.Manifest

public class LooseEarApplication extends LooseApplication {
    
    protected Task task;
    protected Logger logger;

    public LooseEarApplication(Task task, LooseConfigData config, Logger logger) {
        super(task.getProject().getLayout().getBuildDirectory().getAsFile().get().getAbsolutePath(), config)
        this.task = task
        this.logger = logger
    }

    public void addSourceDir() throws Exception {
        if (task.getProject().getPlugins().hasPlugin("ear")) {
            Ear ear = (Ear) task.getProject().ear
            File sourceDir = new File(task.getProject().path.replace(":","") + "/" + ear.getAppDirectory().getAsFile().get().getPath())
            config.addDir(sourceDir, "/")
        }
    }

    public void addApplicationXmlFile() throws Exception {
        String applicationName = "/application.xml"
        File applicationXmlFile;
        if (task.getProject().getPlugins().hasPlugin("ear")) {
            Ear ear = (Ear) task.getProject().ear
            if (ear.getDeploymentDescriptor() != null) {
                applicationName = "/" + ear.getDeploymentDescriptor().getFileName()
            }
            applicationXmlFile = new File(task.getProject().path.replace(":", "") + "/" + ear.getAppDirectory().getAsFile().get().getAbsolutePath() + "/META-INF/" + applicationName)
            if (applicationXmlFile.exists()) {
                config.addFile(applicationXmlFile, "/META-INF/application.xml")
            }
        }
        if (applicationXmlFile == null || !applicationXmlFile.exists()) {
            applicationXmlFile = new File(task.getDestinationDirectory().get().getAsFile().getParentFile().getAbsolutePath() + "/tmp/ear" + applicationName);
            config.addFile(applicationXmlFile, "/META-INF/application.xml")
        }
    }
    
    public Element addWarModule(Project proj) throws Exception {
        Element warArchive = config.addArchive("/" + proj.war.getArchiveFileName().get());
        if (proj.war.getWebAppDirectory().getAsFile().get() != null) {
            var sourceDir = new File(proj.war.getWebAppDirectory().getAsFile().get().getAbsolutePath())
            config.addDir(warArchive,sourceDir,"/")
        }
        proj.sourceSets.main.getOutput().getClassesDirs().each{config.addDir(warArchive, it, "/WEB-INF/classes");}
        if (resourcesDirContentsExist(proj)) {
            config.addDir(warArchive, proj.sourceSets.main.getOutput().getResourcesDir(), "/WEB-INF/classes");
        }
        addWarModuleArtifacts(warArchive, proj)
        return warArchive;
    }

    /**
     * checks whether any resource exists in output resources/main directory
     * @param proj current project
     * @return
     */
    protected static boolean resourcesDirContentsExist(Project proj) {
        def resourcesDir = proj.sourceSets.main.getOutput().getResourcesDir()

        // Check if it's a directory, and then check the 'list' array for emptiness
        // (In Groovy, a non-empty array evaluates to true in a boolean context)
        return resourcesDir.isDirectory() && resourcesDir.list()
    }

    public Element addJarModule(Project proj) throws Exception {
        Element moduleArchive = config.addArchive("/" + proj.jar.getArchiveFileName().get());
        proj.sourceSets.main.getOutput().getClassesDirs().each{config.addDir(moduleArchive, it, "/");}
        if (resourcesDirContentsExist(proj)) {
            config.addDir(moduleArchive, proj.sourceSets.main.getOutput().getResourcesDir(), "/");
        }

        addModuleLibraries(moduleArchive, proj)
        addJarModuleManifestAndClassPathEntries(moduleArchive, proj)

        return moduleArchive;
    }

    private void addJarModuleManifestAndClassPathEntries(Element moduleArchive, Project proj) {
        File manifestFile = getJarTaskManifestFile(proj)
        if (manifestFile == null || !manifestFile.exists()) {
            logger.debug("No jar manifest found for project " + proj.getPath() + ". Skipping manifest Class-Path processing for loose EAR.")
            return
        }

        addManifestFileWithParent(moduleArchive, manifestFile,
            proj.sourceSets.main.getOutput().getResourcesDir().getParentFile().getCanonicalPath())

        List<String> classPathEntries = getManifestClassPathEntries(manifestFile)
        classPathEntries.each { entry ->
            addManifestClassPathEntry(moduleArchive, proj, entry)
        }
    }

    private File getJarTaskManifestFile(Project proj) {
        def jarTask = proj.tasks.findByName('jar')
        if (jarTask == null) {
            return null
        }
        return new File(jarTask.getTemporaryDir(), "MANIFEST.MF")
    }

    private List<String> getManifestClassPathEntries(File manifestFile) {
        List<String> entries = new ArrayList<>()
        Manifest manifest
        manifestFile.withInputStream { input ->
            manifest = new Manifest(input)
        }
        String classPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH)
        if (classPath == null || classPath.trim().isEmpty()) {
            return entries
        }
        classPath.trim().split("\\s+").each { token ->
            if (token != null && !token.trim().isEmpty()) {
                entries.add(token.trim())
            }
        }
        return entries
    }

    private void addManifestClassPathEntry(Element moduleArchive, Project proj, String classPathEntry) {
        String entryName = new File(classPathEntry).getName()
        if (entryName == null || entryName.isEmpty()) {
            return
        }

        Project resolvedProjectDependency = resolveProjectDependencyForClassPathEntry(proj, entryName)
        if (resolvedProjectDependency != null) {
            resolvedProjectDependency.sourceSets.main.getOutput().getClassesDirs().each { dir ->
                config.addDir(moduleArchive, dir, "/")
            }
            if (resourcesDirContentsExist(resolvedProjectDependency)) {
                config.addDir(moduleArchive, resolvedProjectDependency.sourceSets.main.getOutput().getResourcesDir(), "/")
            }
            return
        }

        File resolvedFileDependency = resolveFileDependencyForClassPathEntry(proj, entryName)
        if (resolvedFileDependency != null && resolvedFileDependency.exists()) {
            String targetPath = classPathEntry.startsWith("/") ? classPathEntry : "/" + classPathEntry
            config.addFile(moduleArchive, resolvedFileDependency, targetPath)
            return
        }

        logger.debug("Unable to resolve manifest Class-Path entry '" + classPathEntry + "' for project " + proj.getPath() + ".")
    }

    private Project resolveProjectDependencyForClassPathEntry(Project proj, String entryName) {
        Set<ProjectDependency> projectDependencies = new LinkedHashSet<>()
        proj.configurations.each { configuration ->
            configuration.getAllDependencies().each { dep ->
                if (dep instanceof ProjectDependency) {
                    projectDependencies.add((ProjectDependency) dep)
                }
            }
        }

        for (ProjectDependency dependency : projectDependencies) {
            Project dependencyProject = proj.getRootProject().findProject(dependency.getPath())
            if (dependencyProject == null || dependencyProject.tasks.findByName('jar') == null) {
                continue
            }
            String archiveName = dependencyProject.jar.getArchiveFileName().get()
            if (entryName.equals(archiveName) || entryName.equals(dependencyProject.getName() + ".jar")) {
                return dependencyProject
            }
        }

        return null
    }

    private File resolveFileDependencyForClassPathEntry(Project proj, String entryName) {
        List<String> configurationNames = ["runtimeClasspath", "compileClasspath"]
        for (String configurationName : configurationNames) {
            Configuration configuration = proj.configurations.findByName(configurationName)
            if (configuration == null) {
                continue
            }
            for (File file : configuration.getFiles()) {
                if (entryName.equals(file.getName())) {
                    return file
                }
            }
        }
        return null
    }

    private void addModuleLibraries(Element moduleArchive, Project proj) {
        for (File f : proj.jar.source.getFiles()) {
            String extension = FilenameUtils.getExtension(f.getAbsolutePath())
            switch(extension) {
                case "jar":
                case "war":
                case "rar":
                    config.addFile(moduleArchive, f, "/WEB-INF/lib/" + f.getName());
                    break
                default:
                    break
            }
        }
    }

    private void addWarModuleArtifacts(Element warArchive, Project proj) {
        for (File f : proj.jar.source.getFiles()) {
            String extension = FilenameUtils.getExtension(f.getAbsolutePath())
            switch(extension) {
                case "jar":
                case "war":
                case "rar":
                    config.addFile(warArchive, f, "/WEB-INF/lib/" + f.getName())
                    break
                case "MF":
                    addManifestFileWithParent(warArchive, f,
                        proj.sourceSets.main.getOutput().getResourcesDir().getParentFile().getCanonicalPath())
                    break
                default:
                    break
            }
        }
    }

}
