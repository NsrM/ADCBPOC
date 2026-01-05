package org.example.service;

import org.example.model.RetailTransactionDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
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

    public void upsertTransaction(RetailTransactionDTO request) {

        String pk =  request.getCif();
        String sk = request.getTxnTimestamp();

        Map<String, AttributeValue> key = Map.of(
                "cif", AttributeValue.fromS(pk), // primary key
                "txnTimestamp", AttributeValue.fromS(sk) // sort key
        );

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":amount", AttributeValue.fromN(request.getAmount().toString()));
        values.put(":currency", AttributeValue.fromS(request.getCurrency()));
        values.put(":type", AttributeValue.fromS(request.getTxnType()));
        values.put(":status", AttributeValue.fromS(request.getStatus()));
        //values.put(":updatedAt", AttributeValue.fromN(String.valueOf(System.currentTimeMillis())));

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .updateExpression("""
                    SET #txn.amount = :amount,
                        #txn.currency = :currency,
                        #txn.#t = :type,
                        #txn.#s = :status
                """)
                .expressionAttributeNames(Map.of(
                        "#t", "txnType",
                        "#s", "status",
                        "#txn","transaction"
                ))
                .expressionAttributeValues(values)
                .build();

        dynamoDbClient.updateItem(updateRequest);
    }




}


