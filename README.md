# amonkeys
API Test - The CRM service

Amonkeys is a Java-based web application designed to interact with DynamoDB and S3 for storing user and customer data. The application is built using Spring Boot, leveraging AWS SDK for services like DynamoDB and S3. This project provides basic user and customer management, Google OAuth 2.0 authentication, and file storage.

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
This administrator's email will be the one you need to log in to Google.
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
   There are two profiles in Spring Boot, one for local (local) and another for production (prod). Each profile has its own properties file, respectively.  
      src/main/resources/application-local.properties  
      src/main/resources/application-prod.properties  

   There are non-sensitive variables in those files.

## Build and Run the Project
1. Build the project with Maven:  
   You can build the project with:
   ```bash
   mvn clean install -D spring.profiles.active=local
   ```

2. Run tests
   ```bash
   mvn test -D spring.profiles.active=local
   ```   

3. Run the application locally:
   ```bash
   mvn spring-boot:run -D spring-boot.run.arguments="--spring.profiles.active=local"
   ```
   
4. Verify Everything is Working:
   - Open a browser and visit
   ```bash
   http://localhost:8080/api/users
   ```
   - Authenticate with Google using the email specified in the init.sh file.
   - If successful, you will see the list of users.


# Usage

For detailed usage instructions and available API endpoints, refer to the Postman documentation:

[Postman API Documentation](https://documenter.getpostman.com/view/10832843/2sAYJ9AJ9y)

You can explore the endpoints and even import the collection directly into your Postman environment.



# Automating Infrastructure and CI/CD Pipeline for Amonkeys

# 1. AWS Infrastructure Setup
To deploy and manage the required AWS resources for the application, we use Terraform. Terraform allows you to define and provision AWS resources in a consistent and automated way.

Steps for Running Terraform  
Install Terraform  
If you don't have Terraform installed, you can download it from the official website:  
[Terraform Installation Guide](https://www.terraform.io/)

Modify the Terraform Configuration  
Open the main.tf file located in the root of the repository. In this file, you'll find configuration values for the AWS resources. Ensure to modify the administrator's user details (e.g., email and name) to match your desired values.

Run Terraform to Create the Infrastructure  
Once you've modified the configuration, you can run Terraform to provision the resources by executing the following commands in your terminal:

```bash
   terraform init
   terraform apply
```

# 2. CI/CD Pipeline with GitHub Actions
The goal of this CI/CD pipeline is to automate the build, test, and deployment process every time code is pushed to the main branch. This includes compiling the project, running tests, uploading the JAR to an S3 bucket, copying it to an EC2 instance, and running the application.

1. CI/CD Workflow Steps:  
Checkout the Code: This step checks out your repository and ensures that the code is pulled from the correct branch (usually main).

2. Set Up Java: Use Amazon Corretto (Java 17) to compile your Spring Boot project.

3. Run Tests: Execute unit and integration tests using Maven to ensure the project is working as expected. You can use environment variables for sensitive data like AWS credentials.

4. Build the JAR: If the tests pass, compile the application and package it into a JAR file.

5. Upload the JAR to S3: Upload the JAR file to an S3 bucket where it can be accessed by the EC2 instance.

6. Deploy to EC2: Once the JAR is uploaded, SSH into the EC2 instance and transfer the JAR file from S3 to the instance. Finally, run the application on EC2.

3. GitHub Secrets Configuration
Ensure that you store your sensitive information in GitHub Secrets to avoid exposing it in the workflow configuration.

[Using Secrets in GitHub Actions](https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions)

   You must create these secrets variables:  
   
  - ##AWS_ACCESS_KEY_ID  
  - ##AWS_SECRET_ACCESS_KEY  
   Description: These keys are used to access AWS services such as DynamoDB or SQS.
   How to obtain them:
   Log in to the AWS Management Console.
   Go to IAM (Identity and Access Management).
   Create a new user with the necessary permissions (e.g., AmazonDynamoDBFullAccess or AmazonSQSFullAccess).
   Generate access keys (Access Key and Secret Key) for the user.  
   We won´t use them on production environment
   
     
  - ##AWS_S3_ACCESS_KEY_ID  
  - ##AWS_S3_SECRET_ACCESS_KEY  
   Description: These keys are used to access Minio (local)
   You can obtain them from docker-compose.yml  
   We won´t use them on production environment
   
   
  - ##GOOGLE_CLIENT_ID  
  - ##GOOGLE_CLIENT_SECRET  
   Description: These credentials are required for integrating Google authentication.
   How to obtain them:  
   Log in to the Google Cloud Platform (GCP) Console.  
   Create a new project or use an existing one.  
   Navigate to APIs & Services > Credentials.  
   Create credentials of type OAuth 2.0 Client ID.  
   Copy the client_id and client_secret values.
   
  - ##EC2_SSH_PRIVATE_KEY  
   Description: Private key to access your EC2 instance via SSH.  
   
      Create a Key Pair in AWS:  
      
      Go to the EC2 console in AWS.  
      In the left-hand menu, under Network & Security, select Key Pairs.  
      Click on Create Key Pair.  
      Enter a name for your key pair (e.g., ec2-key).  
      Choose the desired key format (.pem for OpenSSH or .ppk for PuTTY).  
      Click Create Key Pair.  
      Download the Private Key:  
      
      AWS will automatically generate the key pair and provide the option to download the private key (.pem or .ppk) at this point.  
      Important Note: This is the only time you can download the private key. Make sure to save it in a secure location.  
      Assign the Key to Your EC2 Instance:  
      
      During the process of launching your EC2 instance, select the newly created key pair in the Key pair (login) section.  
      Use the Private Key:  
      
      Use the downloaded private key file as the value for the EC2_SSH_PRIVATE_KEY variable.

