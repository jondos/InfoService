KAFFE=$(HOME)/kaffenew/bin/kjc
CLASS_PATH=.:$(CLASSPATH):$(HOME)/src/Jap:$(HOME)/kaffenew/share/kaffe/Klasses.jar:$(HOME)/src/Jap/swingall.jar:$(HOME)/src/Jap/xml.jar:$(HOME)/src/Jap/http.jar:$(HOME)/src/Jap/MRJClasses.zip:$(HOME)/src/Jap/:/usr/java1.1/lib/classes.zip:/usr/lib/jdk1.1.8/lib/classes.zip:../swingall.jar:../xml.jar:../http.jar:../xml-1.1.jar:../kasperftp.jar:../MRJClasses.zip:../xmlrpc.jar
JAVAC=javac
all:
	rm -f JAPCertificate.java
	rm -f JAPTest.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPConstants.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPModel.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPAWTMsgBox.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPMessages.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPUtil.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPDebug.java
	$(KAFFE) --classpath $(CLASS_PATH) Rijndael/*.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) anon/AnonChannel.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) anon/AnonServer.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) anon/AnonServiceEventListener.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) anon/AnonService.java -d .
	$(JAVAC) -classpath $(CLASS_PATH) anon/server/impl/*.java -d . 
	$(KAFFE) --classpath $(CLASS_PATH) anon/server/*.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) anon/*.java
#	$(KAFFE) --classpath $(CLASS_PATH) --verbose anon/*.java
	$(KAFFE) --classpath $(CLASS_PATH) anon/AnonServiceFactory.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) anon/JAPAnonServiceListener.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) anon/xmlrpc/Server.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) AnonServerDBEntry.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPInfoService.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPObserver.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPView.java
	$(KAFFE) --classpath $(CLASS_PATH) JAPController.java
	$(KAFFE) --classpath $(CLASS_PATH) gui/*/*.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) JAPController.java
	$(KAFFE) --classpath $(CLASS_PATH) update/*.java -d .
	$(KAFFE) --classpath $(CLASS_PATH) *.java
	
javac:
	$(JAVAC) -classpath $(CLASS_PATH) JAPConstants.java
	$(JAVAC) -classpath $(CLASS_PATH) JAPModel.java
	$(JAVAC) -classpath $(CLASS_PATH) JAPDebug.java
	$(JAVAC) -classpath $(CLASS_PATH) anon/*.java
	$(JAVAC) -classpath $(CLASS_PATH) Rijndael/*.java
	$(JAVAC) -classpath $(CLASS_PATH) *.java
	
clean:
	rm -f *.class
	rm -f anon/*.class
	rm -f anon/*/*.class
	rm -f anon/*/*/*.class
	rm -f gui/*.class
	rm -f gui/*/*.class
	rm -f update/*.class
	rm -f Rijndael/*.class

start:
	$(HOME)/kaffenew/bin/kaffe -nodeadlock -classpath $(CLASS_PATH) JAP