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
        // *****This below style of code for upsert has a problem: It results in exception
        // The document path provided in the update expression is invalid for update (Service: DynamoDb, Status Code: 400, Request ID: 5T5C904K679JCQ5JHKMHS7ECERVV4KQNSO5AEMVJF66Q9ASUAAJG)
        // Reason: DynamoDB cannot update a child attribute if the parent map doesn’t exist.
        // For a new item transaction is not already existent under which amount , currency other fields are present*****
//
//        String pk =  request.getCif();
//        String sk = request.getTxnTimestamp();
//
//        Map<String, AttributeValue> key = Map.of(
//                "cif", AttributeValue.fromS(pk), // primary key
//                "txnTimestamp", AttributeValue.fromS(sk) // sort key
//        );
//
//        Map<String, AttributeValue> values = new HashMap<>();
//        values.put(":amount", AttributeValue.fromN(request.getAmount().toString()));
//        values.put(":currency", AttributeValue.fromS(request.getCurrency()));
//        values.put(":type", AttributeValue.fromS(request.getTxnType()));
//        values.put(":status", AttributeValue.fromS(request.getStatus()));
//        //values.put(":updatedAt", AttributeValue.fromN(String.valueOf(System.currentTimeMillis())));
//
//        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
//                .tableName(TABLE_NAME)
//                .key(key)
//                .updateExpression("""
//                    SET #txn.amount = :amount,
//                        #txn.currency = :currency,
//                        #txn.#t = :type,
//                        #txn.#s = :status
//                """)
//                .expressionAttributeNames(Map.of(
//                        "#t", "txnType",
//                        "#s", "status",
//                        "#txn","transaction"
//                ))
//                .expressionAttributeValues(values)
//                .build();
//
//        dynamoDbClient.updateItem(updateRequest);

        UpdateItemRequest upsertRequest = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(
                        "cif", AttributeValue.fromS(request.getCif()),
                        "txnTimestamp", AttributeValue.fromS(request.getTxnTimestamp())
                ))
                .updateExpression("SET #txn = :txn")
                .expressionAttributeNames(Map.of(
                        "#txn", "transaction"
                ))
                .expressionAttributeValues(Map.of(
                        ":txn", AttributeValue.fromM(Map.of(
                                "amount", AttributeValue.fromN(request.getAmount().toString()),
                                "currency", AttributeValue.fromS(request.getCurrency()),
                                "status", AttributeValue.fromS(request.getStatus()),
                                "txnId", AttributeValue.fromS(request.getTxnId()),
                                "txnType", AttributeValue.fromS(request.getTxnType())
                        ))
                ))
                .build();
        dynamoDbClient.updateItem(upsertRequest);

    }




}


