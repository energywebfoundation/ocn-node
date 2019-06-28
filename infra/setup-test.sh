#!/usr/bin/env bash

cd ocn-client
git pull
./gradlew -Pprofile=test build
sudo service ocn-client restart