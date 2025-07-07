#!/bin/bash
# Script to update all Spring Boot template files to use Gradle 9.0 compatible syntax

for file in /Users/zeji/Documents/IBM/ci.gradle/src/test/resources/sample.springboot3/*.gradle; do
  # Skip settings.gradle and already converted files
  if [[ $(basename "$file") == "settings.gradle" || $(basename "$file") == "test_spring_boot_plugins_dsl_apps_30.gradle" || $(basename "$file") == "test_spring_boot_apps_30.gradle" ]]; then
    continue
  fi
  
  # Convert from apply plugin syntax to plugins block syntax
  if grep -q "apply plugin: 'java'" "$file"; then
    # Extract Spring Boot version
    springBootVersion=$(grep -o "springBootVersion = '[^']*'" "$file" | sed "s/springBootVersion = '//;s/'//")
    
    # Replace buildscript and apply plugin sections with plugins block
    sed -i.bak '1,/apply plugin: .liberty./c\
plugins {\
    id '\''java'\''\
    id '\''org.springframework.boot'\'' version '\'''"$springBootVersion"'\''\
    id '\''io.spring.dependency-management'\'' version '\''1.1.6'\''\
    id '\''io.openliberty.tools.gradle.Liberty'\'' version "$lgpVersion"\
}\
' "$file"
  fi
  
  # Fix any duplicate repositories blocks
  sed -i.bak '/repositories {/,/}/!b;:a;N;/}/!ba;s/repositories {.*}.*\/\/ IBM public Maven repository.*maven {.*url = .https:\/\/public.dhe.ibm.com\/ibmdl\/export\/pub\/software\/openliberty\/runtime\/maven\/.*}/repositories {\
    mavenLocal()\
    mavenCentral()\
    maven {\
        name = '\''Sonatype Nexus Snapshots'\''\
        url = '\''https:\/\/oss.sonatype.org\/content\/repositories\/snapshots\/'\'' \
    }\
    \/\/ IBM public Maven repository\
    maven {\
        url = '\''https:\/\/public.dhe.ibm.com\/ibmdl\/export\/pub\/software\/openliberty\/runtime\/maven\/'\'' \
    }\
}/g' "$file"
  
  # Fix missing newline between repositories and dependencies
  sed -i.bak 's/}dependencies {/}\n\ndependencies {/g' "$file"
  
  # Fix indentation in dependencies block
  sed -i.bak '/dependencies {/,/}/{s/^[[:space:]]*\(.*\)/    \1/g}' "$file"
  
  # Fix indentation in liberty block
  sed -i.bak '/liberty {/,/}/{s/^[[:space:]]*\(.*\)/    \1/g}' "$file"
done

echo "Updated all Spring Boot template files to use Gradle 9.0 compatible syntax"
