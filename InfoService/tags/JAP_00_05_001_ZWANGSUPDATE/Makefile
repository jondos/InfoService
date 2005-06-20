JARS=/opt/java/log4j/log4j-core.jar:/opt/java/BouncyCastle/BouncyCastleLightForJAP.jar:/opt/java/http/http.jar:/opt/java/jai/jai_core.jar:/opt/java/jaf/activation.jar:/opt/java/mail/mail.jar

JAVAC=/opt/jdk1.5/bin/javac
JAVACOPTS=-classpath $(JARS) -O -target 1.5 -g:none -Xlint:all
JAVACOPTS_DEBUG=-classpath $(JARS) -target 1.5 
#JAVACOPTS=-classpath $(JARS) -target 1.4 -g
JAR=/opt/jdk1.5/bin/jar
JAROPTS=i

InfoService.jar: ./src/*.java ./src/*/*.java
	rm -f MixISTest.java
	rm -r -f ./src/test/
	$(JAVAC) $(JAVACOPTS) ./src/*.java ./src/*/*/*.java ./src/*/*.java ./src/*/*/*/*.java
	$(JAR) -cf InfoService.jar -C src . certificates/*.cer
	$(JAR) -i InfoService.jar

clean:
	rm -f ./src/*.class
	rm -f ./src/*/*/*.class
	rm -f ./src/*/*/*/*.class
	rm -f ./src/*/*.class
	rm -f *.jar

debug: *.java
	rm -f MixISTest.java
	$(JAVAC) $(JAVACOPTS_DEBUG) *.java
	$(JAR) -cf InfoService.jar Database.class *.class
	$(JAR) -i InfoService.jar
