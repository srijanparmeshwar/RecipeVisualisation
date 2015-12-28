# Recipe Visualisation
CST Part II Project - Recipe visualisation by information extraction

Repository for source code. This project is mainly written in Java.

To install dependencies, run the given script file.

To build run <code>mvn install</code>.

To run the servers two environment variables must be set. The first is the WordNet database location, if you have installed the dependencies this should point to the <code>database</code> folder and be set as <code>WNHOME=database</code>. We also need an environment variable for the Graphviz binary executables, typically this will be <code>GRHOME=/usr/bin</code> for Unix or <code>GRHOME=C:/Program Files (x86)/Graphviz[VERSION]/bin</code> for Windows.

To then run the servers, you need to run the built JAR file with dependencies and app.js in the Website module.

Navigate to [localhost](http://localhost) to view the user interface.
