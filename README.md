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
   ```
2. Install and configure aws cli
   ```bash
   https://aws.amazon.com/cli/
   aws configure
   ```
3. Run Docker containers (DynamoDB Local & Minio):
- Ensure Docker is running on your machine.
- Start DynamoDB Local and Minio using Docker Compose:
   ```bash
   docker-compose up
   ```
   This will start DynamoDB on http://localhost:8000 and Minio on http://localhost:9000.

- Create tables and insert initial admin user (modify admin email and name on init.sh file)
   ```bash
   ./init.sh
   ```

4. Configure Google Cloud for OAuth 2.0
   ```bash
   https://cloud.google.com/
   ```

5. Create a new file src/main/resources/application-secrets.properties
      ```bash
      # Amazon Dynamodb sensitive configuration
      AWS_ACCESS_KEY_ID=XXXXXXXXX     #(get from aws configure) 
      AWS_SECRET_ACCESS_KEY=XXXXXXXXX #(get from aws configure)
      
      # Amazon S3 sensitive configuration
      aws.s3.accessKeyId=minioadmin #(get from docker-compose.yml MINIO_ROOT_USER)
      aws.s3.secretKey=minioadmin   #(get from docker-compose.yml MINIO_ROOR_PASSWORD)

      # OAuth2 sensitive configuration
      spring.security.oauth2.client.registration.google.client-id=XXXXXXXXXXXXXX   #(get from Google Cloud)
      spring.security.oauth2.client.registration.google.client-secret=XXXXXXXXXXX  #(get from Google Cloud)
      ```

6. Build the project with Maven:
   If you have Maven installed, you can build the project with:
   ```bash
   mvn clean install
   ```

   You can run tests
   ```bash
   mvn test
   ```   

   Run the application locally:
   ```bash
   mvn spring-boot:run
   ```
   
7. Check if everything works:
   Open a browser and visit
   ```bash
   http://localhost:8080/api/users
   ```
   You will need to authenticate with Google (with the same email you put on init.sh file)
   You will see the list of users.

Usage
API Endpoints

AWS Configuration

CI/CD Integration



   
