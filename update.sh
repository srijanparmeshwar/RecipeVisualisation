#!/bin/bash
git pull
mvn install
cd Website
bower install
cd ..
