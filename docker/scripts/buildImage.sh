#!/bin/bash
export VERSION=`grep -oP 'version=\K.*' impl/target/maven-archiver/pom.properties`
docker build -t ces-docker.dkrreg.mmih.biz/multiply-money-services:$VERSION .
#Remove danling images

export containercount=`docker ps -a -q | wc -l`
if [[ $containercount -gt 1 ]];
  then docker rm $(docker ps -a -q)
fi
export imagecount=`docker images -q -f dangling=true | wc -l`
if [[ $imagecount -gt 1 ]];
  then docker rmi $(docker images -q -f dangling=true)
fi
