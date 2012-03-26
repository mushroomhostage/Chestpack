#!/bin/sh -x
CLASSPATH=../SERVER-dev1.2.4-snapshot/craftbukkit-1.2.4-R0.1-20120325.235512-21.jar javac *.java -Xlint:deprecation -Xlint:unchecked
rm -rf me
mkdir -p me/exphc/Chestpack
mv *.class me/exphc/Chestpack/
jar cf Chestpack.jar me/ *.yml *.java README.md ChangeLog LICENSE
