#!/bin/bash
# Script to update repositories in all Spring Boot template files

for file in /Users/zeji/Documents/IBM/ci.gradle/src/test/resources/sample.springboot3/*.gradle; do
  # Skip settings.gradle
  if [[ $(basename "$file") == "settings.gradle" ]]; then
    continue
  fi
  
  # Add repositories to each file
  sed -i.bak '/repositories {/,/}/c\
repositories {\
	mavenLocal()\
	mavenCentral()\
	maven {\
		name = '\''Sonatype Nexus Snapshots'\''\
		url = '\''https://oss.sonatype.org/content/repositories/snapshots/'\''\
	}\
	// IBM public Maven repository\
	maven {\
		url = '\''https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/maven/'\''\
	}\
}' "$file"
done

echo "Repositories updated in all template files"
