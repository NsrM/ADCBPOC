package org.example.repository;

import org.example.util.DynamoAttributeParser;
import org.example.util.DynamoAttributeWriter;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class DynamoWriteRepository {

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "corebanking_transactions";

    public DynamoWriteRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public Map<String, Object> putItem(
            String cif,
            Map<String, Object> payload) {

        // Ensure partition key
        payload.put("cif", cif);

        Map<String, AttributeValue> item =
                DynamoAttributeWriter.toItem(payload);

        PutItemRequest request =
                PutItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .item(item)
                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                        .returnValues(ReturnValue.ALL_OLD) // detect overwrite
                        .build();

        PutItemResponse response = dynamoDbClient.putItem(request);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("cif", cif);
        //result.put("consumedCapacity", response.consumedCapacity());

        // Optional: fetch item back to confirm
        result.put("storedItem", fetchByCif(cif));

        if (response.hasAttributes()) {
            result.put("message", "Item overwritten");
        } else {
            result.put("message", "Item inserted");
        }

        return result;
    }

    private Map<String, Object> fetchByCif(String cif) {
        GetItemResponse response =
                dynamoDbClient.getItem(
                        GetItemRequest.builder()
                                .tableName(TABLE_NAME)
                                .key(Map.of(
                                        "cif", AttributeValue.builder().s(cif).build()
                                ))
                                .build()
                );

        return response.hasItem()
                ? DynamoAttributeParser.parseItem(response.item())
                : null;
    }


}
