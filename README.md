# Component Transfer System (CTS)

CTS is a system designed to coordinate concurrent component data transfer operations between different devices to efficiently manage disk space and ensure data access performance. This system has been implemented in Java, leveraging the concurrency mechanisms offered by Java 17.

## Key Features

1. **Data Storage**: Data is grouped into components and stored on system devices. Each device and data component has a unique identifier.

2. **Management of Transfer Operations**: The system manages component transfer operations between devices. These operations include adding new components, transferring existing ones, and removing components.

3. **Synchronous Execution of Operations**: Transfer operations are executed synchronously, meaning the caller must wait for the operation to complete before another operation on the same component can be initiated.

4. **Operation Safety**: Before initiating a transfer operation, the system checks various safety conditions, such as the availability of space on the destination device and the uniqueness of operations on a given component.

5. **Operation Prioritization**: In case of contention for device access, the system locally prioritizes operations that have been waiting longer for access.

6. **Error Handling**: The system handles various types of errors, such as incorrect transfer operations or non-existent devices.

7. **Testing**: The project includes unit tests and demonstration tests to verify the correctness of the system.

## Project Requirements and Guidelines

- The project source code is written in Java following good programming practices.
- The solution does not create additional threads.
- No information is printed to standard output.
- The project passes the required tests, including running the demonstration program without errors.
- All errors and exceptions are handled according to the project requirements.

## Academic Purpose

This project has been developed as part of the coursework for studies at the University of Warsaw, Poland.

CTS project provides effective data management in storage systems by synchronizing and securely transferring components between devices, meeting all specified project requirements and guidelines.
