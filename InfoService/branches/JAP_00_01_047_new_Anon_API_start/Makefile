KAFFE=$(HOME)/kaffenew/bin/kjc
CLASS_PATH=$(CLASSPATH):$(HOME)/src/Jap:$(HOME)/kaffenew/share/kaffe/Klasses.jar:$(HOME)/src/Jap/swingall.jar:$(HOME)/src/Jap/xml.jar:$(HOME)/src/Jap/http.jar:$(HOME)/src/Jap/MRJClasses.zip:$(HOME)/src/Jap/:/usr/java1.1/lib/classes.zip
JAVAC=javac
all:
	rm -f JAPCertificate.java
	rm -f JAPTest.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPAWTMsgBox.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPMessages.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPUtil.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPDebug.java
	$(KAFFE) --classpath $(CLASS_PATH) Rijndael/*.java
	$(JAVAC) -classpath $(CLASS_PATH) anon/*.java
#	$(KAFFE) --classpath $(CLASS_PATH) --verbose anon/*.java
	$(KAFFE) --classpath $(CLASS_PATH) *.java
	
javac:
	$(JAVAC) -classpath $(CLASS_PATH) JAPDebug.java
	$(JAVAC) -classpath $(CLASS_PATH) anon/*.java
	$(JAVAC) -classpath $(CLASS_PATH) Rijndael/*.java
	$(JAVAC) -classpath $(CLASS_PATH) *.java
	
clean:
	rm -f *.class
	rm -f anon/*.class
	rm -f Rijndael/*.class

start:
	$(HOME)/kaffenew/bin/kaffe -nodeadlock -classpath $(CLASS_PATH) JAP