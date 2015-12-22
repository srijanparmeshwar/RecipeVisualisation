#!/bin/bash
git pull
mvn -DskipTests install
cd Website
bower install
cd ..
