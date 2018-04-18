#!/bin/bash
export VERSION=`grep -oP 'version=\K.*' impl/target/maven-archiver/pom.properties`
export IMAGENAME=multiply-money-services
export INSTANCE1=multiply-money-1
export INSTANCE2=multiply-money-2

export DOCKER_USERNAME=ces_ci
export DOCKER_PASSWORD='bZBFnsux4hcYe49Wrd'
#Deploy to DEV environment

ssh root@10.1.135.10 "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD" ces-docker.dkrreg.mmih.biz
ssh root@10.1.135.10 "docker pull ces-docker.dkrreg.mmih.biz/$IMAGENAME:$VERSION"

ssh root@10.1.135.10 "docker stop $INSTANCE1"
ssh root@10.1.135.10 "docker rm $INSTANCE1"
ssh root@10.1.135.10 "docker run -d --log-driver gelf --log-opt gelf-address=udp://10.1.135.10:12201 --log-opt tag=\"$INSTANCE1\" --name $INSTANCE1 -e MMI_ENV=dev -p 8187:8080 -p 8887:9990 ces-docker.dkrreg.mmih.biz/$IMAGENAME:$VERSION"

ssh root@10.1.135.10 "docker stop $INSTANCE2"
ssh root@10.1.135.10 "docker rm $INSTANCE2"
ssh root@10.1.135.10 "docker run -d --log-driver gelf --log-opt gelf-address=udp://10.1.135.10:12201 --dns --dns 10.1.24.50 - --log-opt tag=\"$INSTANCE2\" --name $INSTANCE2 -e MMI_ENV=dev -p 8188:8080 -p 7778:9990 ces-docker.dkrreg.mmih.biz/$IMAGENAME:$VERSION"

#ssh root@10.1.135.10 "docker rmi $(docker images -f dangling=true -q)"
