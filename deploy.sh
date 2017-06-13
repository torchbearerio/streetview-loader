#!/bin/bash

# Build jar and Dockerfile
sbt clean update docker

rm target/build.zip

# ZIP it up
zip -j target/build.zip ./target/build.jar ./target/docker/Dockerfile

# Deploy to EB
eb deploy