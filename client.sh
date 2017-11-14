#!/usr/bin/env bash

git clone https://github.com/lazerion/hz-k8s-ci.git
ls -al
cd hz-k8s-ci
mvn test
