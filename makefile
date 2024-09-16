# extract JSON jar file
jsonjar: json-20240303.jar
	jar xf json-20240303.jar

# the compile command compiles all java files
compile: 
	javac -cp "./json-20240303.jar" -d ./ *.java -Xlint

# clean up compiled files
clean: *.class
	rm *.class 

# run tests
test: 
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ Test

# AUTOMATED TEST COMMANDS: BASIC FUNCTIONALITY
# CLIENT PROCESSES START UP AND COMMUNICATE 
client:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ Client

# AGGREGATION SERVER PROCESSES START UP AND COMMUNICATE
aggregation:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ AggregationServer

# CONTENT SERVER PROCESSES START UP AND COMMUNICATE
conserve: 
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ ContentServer

# TEXT SENDING WORKS & PUT OPERATION WORKS FOR 1 CONTENTSERVER
testputrequest:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ PUTTest < putTestInput.txt

# GET OPERATION WORKS FOR MANY CLIENTS
testgetrequest:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ GETTest < getTestInput.txt

# AGGREGATION EXPUNGING EXPIRED DATA WORKS (30s)
testdataexpunge:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ DataExpunge < dataExpungeInput.txt

# RETRY FUNCTIONALITY WORKS
# to see the retry functionality, you must run "make client" first and you will
# see the retry prompt, then run "make aggregation" and it will connect to aggregation
# server and it will connect

# AUTOMATED TEST COMMANDS: FULL FUNCTIONALITY
# Lamport clocks are implemented, you can see the requests and their lamport timestamps in the aggregation server
# terminal and the output when you run 'make lamporttesting' in another terminal, and you will see that the responses
# are sent back in lamport timestamp order
lamporttesting:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ LamportTesting < LamportTestingInput.txt
	
# Content servers are replicated and fault tolerant 
# Content servers are fault tolerant (as they can retry if the aggregation server disconnects) as the 
# retry functionality works  however, my implementaion is failing to save the contents, so it is not replicated

# Multiple, concurrent PUT and GET requests
# you must run this command to concurrently run multiple GET clients and PUT requests
multipleput:	
	make testputrequest && make testgetrequest

# All error codes are implemented:
# 200, 201 and 400 work
# SUCCESS 201: works
test201request:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ Test201req < test201reqInput.txt

# INVALID REQUEST TYPE 400: works
test400request:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ Test400req 

# ERROR 500: does not work, gets hung up
testinvalidjson:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ InvalidJSON < InvalidJSONinput.txt

# ERROR 204: does not work, gets hung up
testemptyjson:
	javac -cp "./json-20240303.jar" -d ./ *.java && java -cp ./ EmptyJSON < emptyJSONinput.txt