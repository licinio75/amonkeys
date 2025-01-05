# amonkeys
API Test - The CRM service

Amonkeys is a Java-based web application designed to interact with DynamoDB and S3 for storing user and customer data. The application is built using Spring Boot, leveraging AWS SDK for services like DynamoDB and S3. This project provides basic user and customer management, authentication, and file storage.

## Prerequisites

Before running this project locally, make sure you have the following tools installed:

- **Java 17** or higher.
- **Docker** for containerized environments.
- **AWS CLI** configured for local DynamoDB and S3 access (for development).
- **Git** for version control.

## Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/licinio75/amonkeys.git
   cd amonkeys
   
2. Run Docker containers (DynamoDB Local & Minio):
- Ensure Docker is running on your machine.
- Start DynamoDB Local and Minio using Docker Compose:

   ```bash
   docker-compose up
This will start DynamoDB on http://localhost:8000 and Minio on http://localhost:9000.

3. Build the project with Maven:
If you have Maven installed, you can build the project with:

   ```bash
   mvn clean install

4. Run the application locally:
You can run the application with:

   ```bash
   mvn spring-boot:run


Usage
API Endpoints

AWS Configuration

CI/CD Integration

prueba

   
