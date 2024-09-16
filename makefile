# Define variables for files and directories
JAR_FILE = json-20240303.jar

# Compile all Java files
compile:
	javac -cp "./$(JAR_FILE)" -d . *.java -Xlint

# Clean up compiled files
clean:
	rm -f *.class

# Run tests after compiling
test: compile
	java -cp "./:./$(JAR_FILE)" PUTTest < put_test_input.txt

# Compile and run the client
client: compile
	java -cp "./:./$(JAR_FILE)" Client

# Compile and run the aggregation server
aggregation: compile
	java -cp "./:./$(JAR_FILE)" AggregationServer

# Compile and run the content server
conserve: compile
	java -cp "./:./$(JAR_FILE)" ContentServer

# Test PUT request after compiling
testputrequest: clean compile
	java -cp "./:./$(JAR_FILE)" PUTTest < put_test_input.txt
