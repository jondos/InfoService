#!/bin/sh
#Note: If this script does not run on cygwin check if the line ending is UNIX
InfoServiceCommands_java=../../src/main/java/infoservice/InfoServiceCommands.java
sed -e "N;N;N;N;s/^.*Full Command: *\(\(GET\)\|\(POST\)\) *\/\([^ ^\n^\/]*\)\(\/\[\([^\n]*\)\]\)*.*Source: *\([^\n]*\).*Category: *\([^\n]*\).*Description_de: *\([^\n]*\)/\\\\isCommand{\8}{\1}{\4}{\6}{\7}{\9}/i;P;D;D;D;D;D" ${InfoServiceCommands_java} | grep "isCommand"
