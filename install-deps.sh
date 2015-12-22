#!/bin/bash
apt-get install python-software-properties
add-apt-repository ppa:webupd8team/java
apt-get update
apt-get install oracle-java8-installer
apt-get install wordnet maven node npm graphviz screen
npm install -g bower
mvn install
wget http://wordnetcode.princeton.edu/wn3.1.dict.tar.gz
mkdir database
tar -xzvf wn3.1.dict.tar.gz -C database
rm wn3.1.dict.tar.gz
cd Website
npm install express
bower install
cd ..
