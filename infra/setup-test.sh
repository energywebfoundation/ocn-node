#!/usr/bin/env bash

cd ocn-node
git pull
./gradlew -Pprofile=test -x test build
sudo service ocn-node restart