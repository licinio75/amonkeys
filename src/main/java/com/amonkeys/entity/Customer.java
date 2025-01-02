package com.amonkeys.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Customer {
    private String id;
    private String email;
    private String name;
    private String surname;
    private String photo;
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
