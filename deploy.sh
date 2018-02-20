#!/bin/bash

# Build jar and Dockerfile
sbt clean update docker

rm -f target/build.zip

# ZIP it up
zip -j target/build.zip ./target/build.jar ./target/docker/Dockerfile
zip -r target/build.zip ./.ebextensions

# Deploy to EB
eb deploy