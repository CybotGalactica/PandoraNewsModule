#!/usr/bin/env bash

echo Uploading...
scp build/libs/PandoraTracker-2.0-SNAPSHOT.jar titan:pandorabot/official/modules/
echo Done!
