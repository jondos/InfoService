#!/bin/sh
#This script is used for development under MacOS X
action=$1;
nocommand="0";
if [ -z "${action}" ]; then
 nocommand="1"
fi
while [ 1 ]
do
if [ "${nocommand}" = "1" ]; then
 echo "Usage: m [ make | clean | start | lean ]"
 echo " (m)ake  ... make all class files"
 echo " (c)lean ... remove all class files"
 echo " (s)tart ... start JAP"
 echo " (l)ean  ... start JAPLean"
 read  action
fi
case "${action}" in
 "m")  action="make";;
 "c")  action="clean";;
 "s")  action="start";;
 "l")  action="lean";;
esac
if [ "${action}" = "make" ]; then
 javac -classpath ../japjars/http.jar:../japjars/xml.jar:../japjars/xml-1.1.jar:../japjars/kasperftp.jar:. JAPDebug.java
 javac -classpath ../japjars/http.jar:../japjars/xml.jar:../japjars/xml-1.1.jar:../japjars/kasperftp.jar:. anon/*.java
 javac -classpath ../japjars/http.jar:../japjars/xml.jar:../japjars/xml-1.1.jar:../japjars/kasperftp.jar:. rijndael/*.java
 javac -classpath ../japjars/http.jar:../japjars/xml.jar:../japjars/xml-1.1.jar:../japjars/kasperftp.jar:. *.java
fi
if [ "${action}" = "clean" ]; then
 rm -r *.class
 rm -r anon/*.class
 rm -r rijndael/*.class
fi
if [ "${action}" = "start" ]; then
 java -classpath ../japjars/http.jar:../japjars/xml.jar:../japjars/xml-1.1.jar:../japjars/kasperftp.jar:. JAP &
fi
if [ "${action}" = "lean" ]; then
 java -classpath ../japjars/http.jar:../japjars/xml.jar:../japjars/xml-1.1.jar:../japjars/kasperftp.jar:. JAPLean 4001 mix.inf.tu-dresden.de 6544
fi
if [ "${nocommand}" = "0" ]; then
 break;
fi
done










