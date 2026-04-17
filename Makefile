ANTLR=./antlr-4.13.2-complete.jar

all: generate compile test

generate:
	java -jar $(ANTLR) -o output Projekt.g4

compile:
	javac -cp $(ANTLR):output:. Main.java
	
test:
	java -cp $(ANTLR):output:. Main test.p > test.ll
	lli test.ll

parsetree:
	java -cp $(ANTLR):output:. org.antlr.v4.runtime.misc.TestRig Projekt -gui test.x

clean:
	rm test.ll
	rm *.class
	rm -rf output