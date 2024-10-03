# Define variables for files and directories
JAR_FILE = json-20240303.jar
CLASSPATH = ./:./$(JAR_FILE)

# Compile all Java files
compile:
	javac -cp "$(CLASSPATH)" -d . *.java 

# Clean up compiled files
clean:
	rm -f *.class

# Compile and run the client
client: compile
	java -cp "$(CLASSPATH)" GETClient

# Compile and run the aggregation server
aggregation: compile
	java -cp "$(CLASSPATH)" AggregationServer

# Compile and run the content server
conserve: compile
	java -cp "$(CLASSPATH)" ContentServer

# Test PUT request after compiling
testputrequest: compile
	java -cp "$(CLASSPATH)" PUTTest < put_test_input.txt

# Test GET request 
testgetrequest: compile
	java -cp "$(CLASSPATH)" GETTest < get_test_input.txt

# Test aggregation expunging expired data (30s)
testdataexpunge: compile
	java -cp "$(CLASSPATH)" DataExpunge < ExpungeInput.txt

# Test Lamport clock synchronization
testlamport: compile
	java -cp "$(CLASSPATH)" LamportClockTest < LamportClockInput.txt

# Test 201 Created response
test201request: compile
	java -cp "$(CLASSPATH)" Response_201 < Response_201_input.txt

# Test 400 Bad Request response
test400request: compile
	java -cp "$(CLASSPATH)" Response_400

# Test 500 Server Error response - fails 
test500request: compile
	java -cp "$(CLASSPATH)" Response_500 InvalidJSON.json < Response_500_input.txt 

# Test 204 No Content response - fails
test204request: compile
	java -cp "$(CLASSPATH)" Response_204 < Response_204_input.txt

# Adding extra tests 
testinvalidjson: compile
	java -cp "$(CLASSPATH)" TestInvalidJSONParsing

# Edge Case
testmissingstationid: compile
	java -cp "$(CLASSPATH)" TestMissingStationID

testnouseragent: compile
	java -cp "$(CLASSPATH)" TestNoUserAgent

