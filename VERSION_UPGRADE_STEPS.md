# Gradle 9.0 Upgrade Steps

## Overview
This document tracks the steps required to upgrade the Liberty Gradle Plugin from legacy Gradle versions to Gradle 9.0.0-rc-1 compatibility.

## STEP 1 - Update gradle version in [gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties)

### Update Gradle Wrapper Version
1. Update `gradle/wrapper/gradle-wrapper.properties` line 3:
   - From: `distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip`
   - To: `distributionUrl=https\://services.gradle.org/distributions/gradle-9.0.0-rc-1-bin.zip`

2. Run wrapper update command to ensure wrapper scripts are updated:
   ```bash
   ./gradlew wrapper --gradle-version=9.0.0-rc-1
   ```
   **STATUS:** <span style="color:green">_SUCCESS_</span>

## STEP 2 - Test Configuration Validation

### Available Test Commands
Run these tests to verify Gradle 9 compatibility:

#### Basic Test Suite
```bash
./gradlew test
```
- Runs the main test suite using JUnit 4.13.1 and Gradle TestKit
- Tests located in `src/test/groovy/`

#### Build and Test Integration
```bash
./gradlew build
```
- Assembles and tests the project
- Includes compilation, testing, and packaging

#### Comprehensive Testing
```bash
./gradlew buildDependents
```
- Assembles and tests this project and all dependent projects

```bash
./gradlew buildNeeded  
```
- Assembles and tests this project and all its dependencies

#### Test Compilation Only
```bash
./gradlew testClasses
```
- Compiles test sources without running tests
- Useful for checking compilation compatibility

#### Publishing Tests
```bash
./gradlew pluginRepo:publishAllPublicationsToTestRepository
```
- Tests plugin publishing functionality

### Test Filtering Options
- Use `-Ptest.exclude=pattern1,pattern2` to exclude specific test patterns
- Use `-Ptest.include=pattern1,pattern2` to include only specific test patterns

### All Available Test Classes (77 total)
The project contains the following test classes in `src/test/groovy/`:

#### Core Infrastructure Tests
- `AbstractIntegrationTest.groovy` - Base test infrastructure
- `LibertyTest.groovy` - Core Liberty functionality tests

#### Development Mode Tests
- `BaseDevTest.groovy` - Base development mode testing
- `DevTest.groovy` - Development mode functionality
- `DevContainerTest.groovy` - Container development mode
- `DevContainerTestWithLooseAppFalse.groovy` - Container dev mode with loose app disabled
- `DevRecompileTest.groovy` - Development mode recompilation
- `PollingDevTest.groovy` - Polling development mode

#### Feature Installation Tests
- `InstallFeature_acceptLicense.groovy` - License acceptance testing
- `InstallFeature_localEsa_Test.groovy` - Local ESA feature installation
- `InstallFeature_multiple.groovy` - Multiple feature installation
- `InstallFeature_single.groovy` - Single feature installation
- `DevSkipInstallFeatureTest.groovy` - Skip feature installation in dev mode
- `DevSkipInstallFeatureConfigTest.groovy` - Skip feature installation configuration
- `KernelInstallFeatureTest.groovy` - Kernel feature installation
- `KernelInstallVersionlessFeatureTest.groovy` - Versionless feature installation
- `WLPKernelInstallFeatureTest.groovy` - WLP kernel feature installation
- `InstallUsrFeature_toExt.groovy` - User feature to extension installation

#### Liberty Installation Tests
- `InstallLiberty_DefaultNoMavenRepo.groovy` - Default installation without Maven repo
- `InstallLiberty_installDir_Invalid_Test.groovy` - Invalid install directory
- `InstallLiberty_installDir_full_lifecycle_Test.groovy` - Full lifecycle install directory
- `InstallLiberty_installDir_missing_wlp_Test.groovy` - Missing WLP install directory
- `InstallLiberty_installDir_plus_create_server_Test.groovy` - Install directory with server creation
- `InstallLiberty_javaee7.groovy` - Java EE 7 installation
- `InstallLiberty_runtimeDep_upToDate_Test.groovy` - Runtime dependency up-to-date check
- `InstallLiberty_runtimeUrl_upToDate_Test.groovy` - Runtime URL up-to-date check
- `InstallLiberty_webProfile7.groovy` - Web Profile 7 installation
- `InstallDirSubProject.groovy` - Sub-project install directory

#### Application Configuration Tests
- `LibertyApplicationConfigurationTest.groovy` - Basic app configuration
- `LibertyApplicationConfigurationIncludeTest.groovy` - Include configuration
- `LibertyApplicationConfigurationM2Test.groovy` - Maven 2 configuration
- `LibertyApplicationConfigurationM2DefaultTest.groovy` - Maven 2 default configuration
- `TestAppConfig.groovy` - Application configuration testing
- `TestAppConfigFail.groovy` - Application configuration failure scenarios
- `TestAppListsWithObjects.groovy` - Application lists with objects
- `TestGetAppNames.groovy` - Application name retrieval

#### Server Environment Tests
- `TestAppendServerEnvWithConfigServerEnv.groovy` - Server env with config
- `TestAppendServerEnvWithNoProps.groovy` - Server env without properties
- `TestAppendServerEnvWithOnlyProps.groovy` - Server env with only properties
- `TestAppendServerEnvWithProps.groovy` - Server env with properties

#### Packaging Tests
- `LibertyPackage_DefaultOutputDir_Test.groovy` - Default output directory
- `LibertyPackage_archiveJarDirExist_Test.groovy` - Archive JAR directory existence
- `LibertyPackage_archiveJar_Test.groovy` - JAR archive packaging
- `LibertyPackage_archiveTarGz_Test.groovy` - TAR.GZ archive packaging
- `LibertyPackage_archiveTar_Test.groovy` - TAR archive packaging
- `LibertyPackage_archiveZipPath_Test.groovy` - ZIP archive path
- `LibertyPackage_archiveZip_Test.groovy` - ZIP archive packaging
- `LibertyPackage_looseApplication_Test.groovy` - Loose application packaging
- `LibertyPackage_noArchive_Test.groovy` - No archive packaging
- `LibertyPackage_noAttrib_Test.groovy` - No attributes packaging

#### Loose Application Tests
- `TestLooseApplication.groovy` - Basic loose application
- `TestLooseApplicationWithWarTask.groovy` - Loose application with WAR task
- `TestLooseEarApplication.groovy` - Loose EAR application
- `TestLooseEarApplicationEarlibs.groovy` - Loose EAR with earlibs
- `TestLooseWarWithLooseJar.groovy` - Loose WAR with loose JAR
- `TestMultiModuleLooseEar.groovy` - Multi-module loose EAR
- `TestMultiModuleLooseEarAppDevMode.groovy` - Multi-module loose EAR in dev mode
- `TestMultiModuleLooseEarWithPages.groovy` - Multi-module loose EAR with pages

#### Server Creation Tests
- `TestCreateConfigDirUpToDate.groovy` - Config directory up-to-date check
- `TestCreateWithConfigDir.groovy` - Create with config directory
- `TestCreateWithDefaultConfigDir.groovy` - Create with default config directory
- `TestCreateWithFiles.groovy` - Create with files
- `TestCreateWithInlineProperties.groovy` - Create with inline properties
- `NoServerNameTest.groovy` - No server name scenario
- `NoAppsTemplateTest.groovy` - No apps template

#### JSP Compilation Tests
- `TestCompileJSP.groovy` - JSP compilation
- `TestCompileJSPSource17.groovy` - JSP compilation with Java 17

#### Verification Tests
- `VerifyFeatureTest.groovy` - Feature verification
- `VerifyLooseAppTestTimeoutSuccess.groovy` - Loose app timeout verification
- `VerifyTimeoutSuccessAppsTest.groovy` - App timeout verification
- `VerifyTimeoutSuccessDropinsTest.groovy` - Dropins timeout verification
- `VerifyTimeoutSuccessListsOfAppsTest.groovy` - App lists timeout verification

#### Feature Generation Tests
- `BaseGenerateFeaturesTest.groovy` - Base feature generation
- `GenerateFeaturesTest.groovy` - Feature generation
- `GenerateFeaturesRestTest.groovy` - REST feature generation
- `PrepareFeatureTest.groovy` - Feature preparation

#### Spring Boot Tests
- `TestSpringBootApplication20.groovy` - Spring Boot 2.0 application
- `TestSpringBootApplication30.groovy` - Spring Boot 3.0 application

#### Eclipse Integration Tests
- `TestEclipseFacetsEar.groovy` - Eclipse facets for EAR
- `TestEclipseFacetsWar.groovy` - Eclipse facets for WAR

#### Miscellaneous Tests
- `ConfigureArquillianTest.groovy` - Arquillian configuration
- `PluginRepoTest.groovy` - Plugin repository
- `TestConfigDropinsApp.groovy` - Config dropins application
- `TestEtcOutputDir.groovy` - Etc output directory
- `TestOutputDirs.groovy` - Output directories
- `TestPluginConfigFile.groovy` - Plugin configuration file
- `TestStripVersion.groovy` - Version stripping
- `TestWarTasksWithDifferentDependencies.groovy` - WAR tasks with dependencies

### Commands to Run Individual Test Classes
```bash
# Run specific test class
./gradlew test --tests "io.openliberty.tools.gradle.TestClassName"

# Run all tests in a package
./gradlew test --tests "io.openliberty.tools.gradle.*"

# Run tests matching a pattern
./gradlew test --tests "*Dev*"

# Sample test command
./gradlew clean install check -P"test.include"="**/AbstractIntegrationTest*" -Druntime=ol -DruntimeVersion="25.0.0.5" --stacktrace --info --no-daemon
```