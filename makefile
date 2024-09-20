# Define variables for files and directories
JAR_FILE = json-20240303.jar

# Compile all Java files
compile:
	javac -cp "./$(JAR_FILE)" -d . *.java 

# Clean up compiled files
clean:
	rm -f *.class

# Compile and run the client
client: compile
	java -cp "./:./$(JAR_FILE)" GETClient

# Compile and run the aggregation server
aggregation: compile
	java -cp "./:./$(JAR_FILE)" AggregationServer

# Compile and run the content server
conserve: compile
	java -cp "./:./$(JAR_FILE)" ContentServer

# Test PUT request after compiling
testputrequest: compile
	java -cp "./:./$(JAR_FILE)" PUTTest < put_test_input.txt

# Test GET request 
testgetrequest: 
	java -cp "./:./$(JAR_FILE)" GETTest < get_test_input.txt

# Test AGGREGATION EXPUNGING EXPIRED DATA WORKS (30s)
testdataexpunge: compile
	java -cp "./:./$(JAR_FILE)" DataExpunge < ExpungeInput.txt

testlamport: compile
	java -cp "./:./$(JAR_FILE)" LamportClockTest < LamportClockInput.txt

test201request: compile
	java -cp "./:./$(JAR_FILE)" Response_201 < Response_201_input.txt

test400request: compile
	java -cp "./:./$(JAR_FILE)" Response_400

test500request: compile
	java -cp "./:./$(JAR_FILE)" Response_500 InvalidJSON.json < Response_500_input.txt 

test204request: compile
	java -cp "./:./$(JAR_FILE)" Response_204 < Response_204_input.txt
