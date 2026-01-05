package org.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RetailTransactionService {

    private final DynamoDbClient dynamoDbClient;

    private static final String TABLE_NAME = "retailbanking_transactions";

    public RetailTransactionService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Fetch all transactions for a CIF
     */
    public List<Map<String, AttributeValue>> getByCif(String cif) {

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("cif = :cif")
                .expressionAttributeValues(Map.of(
                        ":cif", AttributeValue.builder().s(cif).build()
                ))
                .build();

        return dynamoDbClient.query(request).items();
    }

    /**
     * Fetch transactions for a CIF within a date range
     */
    public List<Map<String, AttributeValue>> getByCifAndDateRange(
            String cif,
            String fromTs,
            String toTs) {

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression(
                        "cif = :cif AND txnTimestamp BETWEEN :from AND :to"
                )
                .expressionAttributeValues(Map.of(
                        ":cif", AttributeValue.builder().s(cif).build(),
                        ":from", AttributeValue.builder().s(fromTs).build(),
                        ":to", AttributeValue.builder().s(toTs).build()
                ))
                .build();

        return dynamoDbClient.query(request).items();
    }

    /**
     * Fetch latest transaction
     */
    public Optional<Map<String, AttributeValue>> getLatestByCif(String cif) {

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("cif = :cif")
                .expressionAttributeValues(Map.of(
                        ":cif", AttributeValue.builder().s(cif).build()
                ))
                .scanIndexForward(false) // DESC
                .limit(1)
                .build();

        List<Map<String, AttributeValue>> items =
                dynamoDbClient.query(request).items();

        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }


}


