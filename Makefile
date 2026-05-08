ANTLR=./antlr-4.13.2-complete.jar

all: generate compile test

generate:
	java -jar $(ANTLR) -o output Projekt.g4

compile:
	javac -cp $(ANTLR):output:. *.java
	
test:
	java -cp $(ANTLR):output:. Main test.p > test.ll
	lli test.ll

test-arrays: compile
	java -cp $(ANTLR):output:. Main test_arrays.p > test_arrays.ll
	lli test_arrays.ll

parsetree:
	java -cp $(ANTLR):output:. org.antlr.v4.runtime.misc.TestRig Projekt -gui test.x

clean:
	rm -f test.ll test_arrays.ll
	rm *.class
	rm -rf output
