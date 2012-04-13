#!/bin/sh -x
CLASSPATH=../craftbukkit-1.2.5-R0.1.jar javac *.java -Xlint:deprecation -Xlint:unchecked
rm -rf me
mkdir -p me/exphc/Chestpack
mv *.class me/exphc/Chestpack/
jar cf Chestpack.jar me/ *.yml *.java README.md ChangeLog LICENSE
