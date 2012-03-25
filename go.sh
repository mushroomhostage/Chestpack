#!/bin/sh -x
CLASSPATH=../craftbukkit-1.2.3-R0.2.jar javac *.java -Xlint:deprecation -Xlint:unchecked
rm -rf me
mkdir -p me/exphc/Chestpack
mv *.class me/exphc/Chestpack/
jar cf Chestpack.jar me/ *.yml *.java README.md ChangeLog LICENSE
