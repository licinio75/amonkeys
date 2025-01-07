variable "aws_region" {
  description = "AWS region for resources"
  default     = "eu-north-1"
}

provider "aws" {
  region = var.aws_region
}

# IAM Role for EC2 with CloudWatch Logs permissions
resource "aws_iam_role" "ec2_cloudwatch_role" {
  name               = "ec2-cloudwatch-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# Attach CloudWatch Logs policy to EC2 role
resource "aws_iam_role_policy_attachment" "ec2_cloudwatch_policy" {
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
  role       = aws_iam_role.ec2_cloudwatch_role.name
}

# Create CloudWatch log group for EC2 logs
resource "aws_cloudwatch_log_group" "app_log_group" {
  name              = "/aws/ec2/amonkeys-app"
  retention_in_days = 30 # Retain logs for 30 days
}

# Create EC2 instance with CloudWatch Logs agent installation
resource "aws_instance" "app_instance" {
  ami           = "ami-02df5cb5ad97983ba"
  instance_type = "t3.micro"
  key_name      = "amonkeys"

  security_groups = [aws_security_group.app_sg.name]

  user_data = <<-EOF
              #!/bin/bash
              # Install Java (Amazon Corretto)
              wget https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.rpm
              sudo rpm -ivh amazon-corretto-17-x64-linux-jdk.rpm

              # Install CloudWatch Agent (alternative to awslogs)
              sudo yum install -y amazon-cloudwatch-agent

              # Configure CloudWatch Agent (edited for custom app log)
              sudo tee /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json <<EOF2
              {
                "logs": {
                  "logs_collected": {
                    "files": {
                      "collect_list": [
                        {
                          "file_path": "/home/ec2-user/amonkeys.log",
                          "log_group_name": "/aws/ec2/amonkeys-app",
                          "log_stream_name": "{instance_id}"
                        }
                      ]
                    }
                  }
                }
              }
              EOF2

              # Start CloudWatch Agent
              sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a start -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

              # Check if the jar is already running
              if ! pgrep -f "amonkeys.jar" > /dev/null; then
                # Start the jar if it's not running
                nohup java -jar /home/ec2-user/amonkeys.jar > /home/ec2-user/amonkeys.log 2>&1 &
              fi
              EOF

  iam_instance_profile = aws_iam_instance_profile.ec2_instance_profile.name

  tags = {
    Name = "AmonkeysAppInstance"
  }
}

# IAM Instance Profile to associate with EC2 instance
resource "aws_iam_instance_profile" "ec2_instance_profile" {
  name = "ec2-instance-profile"
  role = aws_iam_role.ec2_cloudwatch_role.name
}

# Security Group for EC2 instance
resource "aws_security_group" "app_sg" {
  name        = "app-sg"
  description = "Allow traffic to app"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Attach CloudWatch Logs policy to EC2 instance role
resource "aws_iam_role_policy_attachment" "ec2_cloudwatch_logs_attachment" {
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
  role       = aws_iam_role.ec2_cloudwatch_role.name
}

# Create DynamoDB users table
resource "aws_dynamodb_table" "users_table" {
  name         = "users"
  hash_key     = "id"
  billing_mode = "PAY_PER_REQUEST"
  attribute {
    name = "id"
    type = "S"
  }
}

# Create DynamoDB customers table
resource "aws_dynamodb_table" "customers_table" {
  name         = "customers"
  hash_key     = "id"
  billing_mode = "PAY_PER_REQUEST"
  attribute {
    name = "id"
    type = "S"
  }
}


# Insert a user into the users table
resource "aws_dynamodb_table_item" "user_item" {
  table_name = aws_dynamodb_table.users_table.name

  hash_key   = "id"
  item = <<ITEM
{
  "id": {"S": "1"},
  "email": {"S": "licinio@gmail.com"},
  "name": {"S": "Licinio"},
  "isAdmin": {"BOOL": true},
  "isDeleted": {"BOOL": false}
}
ITEM
}

# Create S3 bucket
resource "aws_s3_bucket" "amonkeys" {
  bucket = "amonkeys-photos"
}

# IAM Role for GitHub Actions
resource "aws_iam_role" "github_actions_role" {
  name               = "github-actions-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Principal = {
          Service = "sts.amazonaws.com"
        }
      }
    ]
  })
}

# Policy for GitHub Actions to access EC2, S3
resource "aws_iam_policy" "github_actions_policy" {
  name        = "github-actions-policy"
  description = "Policy for GitHub Actions to access EC2, S3, etc."

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "ec2:DescribeInstances",
          "ec2:StartInstances",
          "ec2:StopInstances",
          "ec2:RebootInstances",
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:DeleteObject"
        ]
        Effect   = "Allow"
        Resource = "*"
      }
    ]
  })
}

# Attach GitHub Actions policy to IAM role
resource "aws_iam_role_policy_attachment" "attach_policy" {
  policy_arn = aws_iam_policy.github_actions_policy.arn
  role       = aws_iam_role.github_actions_role.name
}

# IAM Policy for EC2 to access S3 bucket
resource "aws_iam_policy" "ec2_s3_access_policy" {
  name        = "ec2-s3-access-policy"
  description = "Policy for EC2 to access S3 bucket"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "s3:GetObject",
          "s3:ListBucket",
          "s3:PutObject",
          "s3:DeleteObject"
        ]
        Effect   = "Allow"
        Resource = [
          "arn:aws:s3:::amonkeys-photos",
          "arn:aws:s3:::amonkeys-photos/*"
        ]
      }
    ]
  })
}

# Attach S3 access policy to EC2 role
resource "aws_iam_role_policy_attachment" "ec2_s3_access_attachment" {
  policy_arn = aws_iam_policy.ec2_s3_access_policy.arn
  role       = aws_iam_role.ec2_cloudwatch_role.name
}
