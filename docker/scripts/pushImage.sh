#!/bin/bash
export VERSION=`grep -oP 'version=\K.*' impl/target/maven-archiver/pom.properties`
export DOCKER_USERNAME=ces_ci
export DOCKER_PASSWORD='bZBFnsux4hcYe49Wrd'
#Push to docker cloud
docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD ces-docker.dkrreg.mmih.biz
docker push ces-docker.dkrreg.mmih.biz/multiply-money-services:$VERSION
