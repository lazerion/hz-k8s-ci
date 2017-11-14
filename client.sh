#!/usr/bin/env bash

git clone https://github.com/lazerion/hz-k8s-ci.git
ls -al
cd hz-k8s-ci
mvn test
rc=$?
if [[ ${rc} -ne 0 ]] ; then
  echo 'could not perform tests'; exit $rc
fi
