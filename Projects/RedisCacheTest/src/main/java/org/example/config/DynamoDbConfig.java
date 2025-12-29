package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.AP_SOUTH_1) // change if needed
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

    }

//    @Bean
//    public DynamoDbEnhancedClient dynamoDbEnhancedClient(
//            DynamoDbClient dynamoDbClient) {
//        return DynamoDbEnhancedClient.builder()
//                .dynamoDbClient(dynamoDbClient)
//                .build();
//    }
}

