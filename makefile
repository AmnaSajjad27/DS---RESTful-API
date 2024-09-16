# Define variables for directories and files
SOURCE_DIR = .
TEST_DIR = Tests
JAR_FILE = json-20240303.jar

# Compile all Java source files
compile:
	javac -cp "./$(JAR_FILE)" -d ./ $(SOURCE_DIR)/*.java $(TEST_DIR)/*.java -Xlint

# Run tests
test: compile
	java -cp "./:./$(JAR_FILE)" Tests.PUTTest < $(TEST_DIR)/put_test_input.txt

# Clean up compiled files
clean:
	rm -f *.class $(TEST_DIR)/*.class

# Run PUT request test
testputrequest: compile
	java -cp "./:./$(JAR_FILE)" Tests.PUTTest < $(TEST_DIR)/put_test_input.txt
