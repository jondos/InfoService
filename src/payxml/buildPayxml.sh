#!/bin/bash
#
# Achtung! Dieses Skript sollte nur zum Übersetzen der XML-Klassen für
# die Bezahlinstanz benutzt werden, weil hier JDK 1.4 vorausgesetzt
# wird.
#
# Der Quellcode der XML-Klassen selbst ist aber auch JDK1.1 kompatibel
# und kann unter Windows/Mac auch für Java1 kompiliert werden

# include some jars
JAVA_CLASSPATH=.:extjars/bcprov-jdk14-120.jar

# compile all .java files
echo "Compiling Java sources..." &&
javac -classpath $JAVA_CLASSPATH src/payxml/*.java src/payxml/test/*.java src/payxml/util/*.java &&
echo "Compiling done" &&

# make classes directory structure
echo "Making class directory structure..." &&
rm -rf classes &&
mkdir classes &&
mkdir classes/payxml &&
mkdir classes/payxml/util &&
mkdir classes/payxml/test &&

# move all class files there
cd src &&
find -name "*.class" -exec mv {} ../classes/{} \; &&
cd .. &&
echo "Done move classfiles" &&

# pack the .jar file
echo "Packing .jar file..." &&
cd classes &&
jar cvfm ../payxml.jar ../src/manifesto.txt * &&
echo "Done." 

# that's it