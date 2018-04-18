#!/bin/bash
#Run from root project folder
export VERSION=`grep -oP 'version=\K.*' impl/target/maven-archiver/pom.properties`
export IMAGENAME=member-services
export POD_NAME=multiply-money-services

sudo docker stop $IMAGENAME
sudo docker rm $IMAGENAME
sudo ./docker/scripts/buildImage.sh

echo variables: $VERSION $IMAGENAME $POD_NAME
echo sudo docker run -d --name $IMAGENAME -v /etc/localtime:/etc/localtime --dns 10.1.24.50 -e MMI_ENV=dev  -p 8787:8787 -p 8081:8081 -p 9992:9992 ces-docker.dkrreg.mmih.biz/$POD_NAME:$VERSION
sudo docker run -d --name $IMAGENAME -v /etc/localtime:/etc/localtime --dns 10.1.24.50 -e MMI_ENV=dev  -p 8787:8787 -p 8081:8081 -p 9992:9992 ces-docker.dkrreg.mmih.biz/$POD_NAME:$VERSION