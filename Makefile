KAFFE=$(HOME)/kaffe/bin/kjc
CLASS_PATH=$(CLASSPATH):$(HOME)/kaffe/share/kaffe/Klasses.jar:$(HOME)/src/Jap/swingall.jar:$(HOME)/src/Jap/xml.jar:$(HOME)/src/Jap/http.jar:$(HOME)/src/Jap/MRJClasses.zip:$(HOME)/src/Jap/:/usr/java1.1/lib/classes.zip
JAVAC=javac
all:
	$(KAFFE) --classpath $(CLASS_PATH) JAPDebug.java
	$(JAVAC) -classpath $(CLASS_PATH) anon/*.java
#	$(KAFFE) --classpath $(CLASS_PATH) --verbose anon/*.java
	$(KAFFE) --classpath $(CLASS_PATH) --verbose Rijndael/*.java
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
	$(HOME)/kaffe/bin/kaffe -nodeadlock -classpath $(CLASS_PATH) JAP