# Define variables for files
JAR_FILE = json-20240303.jar

# Compile command
compile:
	javac -cp "./$(JAR_FILE)" -d . *.java -Xlint

# Clean up compiled files
clean:
	rm -f *.class

# Run tests
test:
	java -cp "./:./$(JAR_FILE)" PUTTest < put_test_input.txt

# TEST PUT REQUEST
testputrequest: compile
	java -cp "./:./$(JAR_FILE)" PUTTest < put_test_input.txt
