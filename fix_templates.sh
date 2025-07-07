#!/bin/bash
# Script to fix quote characters in Spring Boot template files

for file in /Users/zeji/Documents/IBM/ci.gradle/src/test/resources/sample.springboot3/*.gradle; do
  # Fix Spring Boot version quotes
  sed -i.bak 's/springBootVersion = \.3\.1\.0\./springBootVersion = '\''3.1.0'\''/g' "$file"
  
  # Fix Liberty runtime quotes
  sed -i.bak 's/libertyRuntime group: \.io\.openliberty\., name: \.openliberty-runtime\., version: \.25\.0\.5\./libertyRuntime group: '\''io.openliberty'\'', name: '\''openliberty-runtime'\'', version: '\''25.0.5'\''/g' "$file"
  
  # Fix any other potential quote issues
  sed -i.bak 's/\.25\.0\.0\.2\./'\''25.0.0.2'\''/g' "$file"
  sed -i.bak 's/\.3\.4\.0\./'\''3.4.0'\''/g' "$file"
  sed -i.bak 's/\.3\.2\.0\./'\''3.2.0'\''/g' "$file"
  sed -i.bak 's/\.3\.1\.0\./'\''3.1.0'\''/g' "$file"
done

echo "Template files fixed"
