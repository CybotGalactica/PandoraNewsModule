#!/usr/bin/env bash

./gradlew clean jar
cp build/libs/PandoraTracker-2.0-SNAPSHOT.jar ../official/modules/
cd ../official/
./start.sh
