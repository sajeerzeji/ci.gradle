#!/bin/bash

# This script verifies and fixes the repositories block in all Spring Boot template files
# to ensure the IBM Maven repository is properly configured for Liberty runtime resolution

echo "Verifying repositories in Spring Boot template files..."

# Function to check and fix repositories in a file
check_and_fix_repositories() {
  local file=$1
  echo "Checking $file..."
  
  # Check if the IBM Maven repository is present
  if ! grep -q "public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/maven" "$file"; then
    echo "Adding IBM Maven repository to $file"
    # Use sed to add the IBM Maven repository after the repositories block
    sed -i '' '/repositories {/,/}/c\
repositories {\
	mavenLocal()\
	mavenCentral()\
	maven {\
		name = "Sonatype Nexus Snapshots"\
		url = "https://oss.sonatype.org/content/repositories/snapshots/"\
	}\
	// IBM public Maven repository\
	maven {\
		url = "https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/maven/"\
	}\
}' "$file"
  else
    echo "IBM Maven repository already present in $file"
  fi
}

# Process all Spring Boot template files
for file in src/test/resources/sample.springboot3/*.gradle; do
  check_and_fix_repositories "$file"
done

echo "Repository verification complete!"
