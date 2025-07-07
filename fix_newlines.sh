#!/bin/bash
# Script to fix missing newlines between repositories and dependencies blocks

for file in /Users/zeji/Documents/IBM/ci.gradle/src/test/resources/sample.springboot3/*.gradle; do
  # Skip settings.gradle
  if [[ $(basename "$file") == "settings.gradle" ]]; then
    continue
  fi
  
  # Fix missing newline between repositories and dependencies
  sed -i.bak 's/}dependencies {/}\n\ndependencies {/g' "$file"
done

echo "Fixed missing newlines in template files"
