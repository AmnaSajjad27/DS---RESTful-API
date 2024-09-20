# DS---RESTful-API
**Assignment 2**

**Building an aggregation server with consistency management and a RESTful API** 

## Overview
This project implements a RESTful API with functionality for handling weather data. The API includes a content server, an aggregation server, and a client for testing various requests.

## Makefile Commands

The provided Makefile automates the compilation and execution of the Java files in the project. Below are the available commands:

### 1. Compilation
- **Command**: `make compile`
- **Description**: Compiles all Java files in the current directory, creating corresponding `.class` files.

### 2. Clean
- **Command**: `make clean`
- **Description**: Deletes all compiled `.class` files to clean up the project directory.

### 3. Run Client
- **Command**: `make client`
- **Description**: Compiles the Java files (if not already compiled) and runs the `GETClient`, which is used to send GET requests to the server.

### 4. Run Aggregation Server
- **Command**: `make aggregation`
- **Description**: Compiles the Java files (if not already compiled) and runs the `AggregationServer`, which aggregates data from various content servers.

### 5. Run Content Server
- **Command**: `make conserve`
- **Description**: Compiles the Java files (if not already compiled) and runs the `ContentServer`, which serves weather data to clients.

### 6. Test PUT Request
- **Command**: `make testputrequest`
- **Description**: Compiles the Java files (if not already compiled) and runs the `PUTTest` class, which tests the PUT request functionality using input from `put_test_input.txt`.

### 7. Test GET Request
- **Command**: `make testgetrequest`
- **Description**: Runs the `GETTest` class, which tests the GET request functionality using input from `get_test_input.txt`.

### 8. Test Data Expunge
- **Command**: `make testdataexpunge`
- **Description**: Compiles the Java files (if not already compiled) and runs the `DataExpunge` class, which tests the functionality for clearing expired data after 30 seconds.

### 9. Test Lamport Clock
- **Command**: `make testlamport`
- **Description**: Compiles the Java files (if not already compiled) and runs the `LamportClockTest` class, which tests the Lamport clock functionality.

### 10. Test 201 Response
- **Command**: `make test201request`
- **Description**: Compiles the Java files (if not already compiled) and runs the `Response_201` class to test a 201 Created response.

### 11. Test 400 Response
- **Command**: `make test400request`
- **Description**: Compiles the Java files (if not already compiled) and runs the `Response_400` class to test a 400 Bad Request response.

### 12. Test 500 Response
- **Command**: `make test500request`
- **Description**: Compiles the Java files (if not already compiled) and runs the `Response_500` class, testing an invalid JSON input using `InvalidJSON.json` and the input from `Response_500_input.txt`.

### 13. Test 204 Response
- **Command**: `make test204request`
- **Description**: Compiles the Java files (if not already compiled) and runs the `Response_204` class to test a 204 No Content response.

## How to Run Tests

1. Open a terminal and navigate to the directory containing the project files.
2. Use the `make` command followed by the desired target (e.g., `make client`, `make test500request`, etc.) to run the respective command.
3. Ensure that the necessary input files are present in the directory as specified in the Makefile.
