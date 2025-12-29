package org.example.service;

import org.example.model.CoreBankingTransactions;
import org.example.repository.CoreBankingRepository;
import org.example.util.DynamoAttributeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CoreBankingService {

    private final CoreBankingRepository repository;

    private final DynamoAttributeParser dynamoAttributeParser;

    @Autowired
    DynamoDbClient dynamoDbClient;

    public CoreBankingService(CoreBankingRepository repository, DynamoAttributeParser dynamoAttributeParser) {
        this.repository = repository;
        this.dynamoAttributeParser = dynamoAttributeParser;
    }

    public CoreBankingTransactions getAccount(String accountNo) {
        return repository.findByAccountNo(accountNo);
    }

    public Map<String, Object> findByCif(String cif) {

        GetItemResponse response =
                dynamoDbClient.getItem(
                        GetItemRequest.builder()
                                .tableName("corebanking_transactions")
                                .key(Map.of(
                                        "cif", AttributeValue.builder().s(cif).build()
                                ))
                                .build()
                );

        if (!response.hasItem()) {
            return null;
        }

        return parseItem(response.item());
    }

    public static Map<String, Object> parseItem(Map<String, AttributeValue> item) {
        Map<String, Object> result = new LinkedHashMap<>();
        item.forEach((k, v) -> result.put(k, DynamoAttributeParser.parse(v)));
        return result;
    }


    public CoreBankingTransactions getByCif(String cif) {
        try {
            ListTablesResponse listTablesResponse = dynamoDbClient.listTables();
            System.out.println("Tables in this account/region:");
            listTablesResponse.tableNames().forEach(System.out::println);


            DescribeTableResponse tableResponse =
                    dynamoDbClient.describeTable(
                            DescribeTableRequest.builder()
                                    .tableName("corebanking_transactions")
                                    .build()
                    );

            System.out.println("Key Schema:");
            tableResponse.table().keySchema()
                    .forEach(k ->
                            System.out.println(k.attributeName() + " -> " + k.keyType())
                    );


//            System.out.println("Access Key: " + creds.accessKeyId());
//            System.out.println("Secret Key: " + creds.secretAccessKey());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return repository.findByCif(cif);
    }


}

