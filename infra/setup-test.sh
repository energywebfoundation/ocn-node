#!/usr/bin/env bash

cd ocn-client
git pull
./gradlew -Pprofile=test -x test build
sudo service ocn-client restart