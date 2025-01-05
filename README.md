# amonkeys
API Test - The CRM service

Amonkeys is a Java-based web application designed to interact with DynamoDB and S3 for storing user and customer data. The application is built using Spring Boot, leveraging AWS SDK for services like DynamoDB and S3. This project provides basic user and customer management, authentication, and file storage.

## Prerequisites

Before running this project locally, make sure you have the following tools installed:

- **Java 17** or higher.
- **Maven** for building and managing dependencies.
- **Docker** for containerized environments.
- **AWS CLI** configured for local DynamoDB and S3 access (for development).
- **Git** for version control.

## Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/licinio75/amonkeys.git
   cd amonkeys
   ```
2. Install and configure aws cli
   Follow the instructions at [AWS CLI Installation Guide](https://aws.amazon.com/cli/)
   ```bash
   aws configure
   ```
3. Run Docker containers (DynamoDB Local & Minio):
Ensure Docker is running on your machine.
Start DynamoDB Local and Minio using Docker Compose:
   ```bash
   docker-compose up
   ```
This will start DynamoDB on http://localhost:8000 and Minio on http://localhost:9000.

4. Create tables and insert the initial admin user
Modify admin email and name on init.sh file
   ```bash
   ./init.sh
   ```

5. Configure Google Cloud for OAuth 2.0
Visit [Google Cloud Console](https://cloud.google.com/)
   

6. Create a new file src/main/resources/application-secrets.properties with the sensitive information
      ```bash
      # Amazon Dynamodb sensitive configuration
      AWS_ACCESS_KEY_ID=XXXXXXXXX     #(obtained  from aws configure) 
      AWS_SECRET_ACCESS_KEY=XXXXXXXXX #(obtained  from aws configure)
      
      # Amazon S3 sensitive configuration
      AWS_S3_ACCESS_KEY_ID=minioadmin #(from docker-compose.yml MINIO_ROOT_USER)
      AWS_S3_SECRET_ACCESS_KEY=minioadmin   #(from docker-compose.yml MINIO_ROOR_PASSWORD)

      # OAuth2 sensitive configuration
      spring.security.oauth2.client.registration.google.client-id=XXXXXXXXXXXXXX   #(obtained  from Google Cloud)
      spring.security.oauth2.client.registration.google.client-secret=XXXXXXXXXXX  #(obtained  from Google Cloud)
      ```
7. 
   In the files
      src/main/resources/application-local.properties
      src/main/resources/application-prod.properties

   There are non-sensitive variables for their respective environments.

## Build and Run the Project
1. Build the project with Maven:
   You can build the project with:
   ```bash
   mvn clean install
   ```

2. Run tests
   ```bash
   mvn test
   ```   

3. Run the application locally:
   ```bash
   mvn spring-boot:run
   ```
   
4. Verify Everything is Working:
   - Open a browser and visit
   ```bash
   http://localhost:8080/api/users
   ```
   - Authenticate with Google using the email specified in the init.sh file.
   - If successful, you will see the list of users.


## Usage

For detailed usage instructions and available API endpoints, refer to the Postman documentation:

[Postman API Documentation](https://documenter.getpostman.com/view/10832843/2sAYJ9AJ9y)

You can explore the endpoints and even import the collection directly into your Postman environment.












TODO LIST


AWS Configuration

CI/CD Integration



   
