#!/bin/bash
screen node Website/app.js
screen -dm  bash -c "export WNHOME=database;export GRHOME=/usr/bin;java -Xmx1536m -jar target/RecipeVisualisation-1.0-SNAPSHOT-jar-with-dependencies.jar"
