#!/usr/bin/env bash

./gradlew build --parallel -x test -Pprofile=docker
docker-compose up --build
