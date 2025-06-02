#!/bin/bash
cd /home/kavia/workspace/code-generation/2048-webmaster-38404-ba8aeb76/main_container_for_2048_webmaster
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

