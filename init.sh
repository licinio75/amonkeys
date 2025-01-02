#!/bin/bash
# init.sh
echo "Creating users table..."
aws --endpoint-url http://localhost:8000 dynamodb create-table \
    --table-name users \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

echo "Creating customers table..."
aws --endpoint-url http://localhost:8000 dynamodb create-table \
    --table-name customers \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

# Insert an item into the users table
echo "Inserting user Licinio into the users table..."
aws --endpoint-url http://localhost:8000 dynamodb put-item \
    --table-name users \
    --item '{
        "id": {"S": "1"},
        "name": {"S": "Licinio"},
        "email": {"S": "licinio@gmail.com"},
        "isAdmin": {"S": "true"},
        "isDeleted": {"S": "false"}
    }'

echo "Tables created and user inserted successfully."
