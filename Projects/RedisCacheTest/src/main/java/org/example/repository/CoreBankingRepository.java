package org.example.repository;

import org.example.model.CoreBankingTransactions;
import org.example.model.Transaction;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class CoreBankingRepository {

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "corebanking_transactions";

    public CoreBankingRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public CoreBankingTransactions findByAccountNo(String accountNo) {

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(
                        "account_no", AttributeValue.builder().s(accountNo).build()
                ))
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);

        if (!response.hasItem()) {
            return null;
        }

        return mapToEntity(response.item());
    }

    public CoreBankingTransactions findByCif(String cif) {

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of(
                        "cif", AttributeValue.builder().s(cif).build()
                ))
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);


        if (!response.hasItem()) {
            return null;
        }

        return mapToEntity(response.item());
    }

    /* ------------------ MAPPING LOGIC ------------------ */

//    private CoreBankingTransactions mapToEntity(
//            Map<String, AttributeValue> item) {
//
//        CoreBankingTransactions entity = new CoreBankingTransactions();
//
//        entity.setAccountNo(item.get("account_no").s());
//        entity.setCif(item.get("cif").s());
//
//        List<Transaction> transactions = new ArrayList<>();
//
//        List<AttributeValue> txnList =
//                item.get("transactions").l();
//
//        for (AttributeValue txnAttr : txnList) {
//            transactions.add(mapTransaction(txnAttr.m()));
//        }
//
//        entity.setTransactions(transactions);
//        return entity;
//    }

    private CoreBankingTransactions mapToEntity(
            Map<String, AttributeValue> item) {

        CoreBankingTransactions entity = new CoreBankingTransactions();

        entity.setCif(item.get("cif").s());
        entity.setAccountNo(item.get("account_no").s());

        List<Transaction> transactions = new ArrayList<>();

        for (AttributeValue txn : item.get("transactions").l()) {
            transactions.add(mapTransaction(txn.m()));
        }

        entity.setTransactions(transactions);
        return entity;
    }



    private Transaction mapTransaction(
            Map<String, AttributeValue> txnMap) {

        Transaction txn = new Transaction();

        txn.setCustomerId(getString(txnMap, "customerId"));
        txn.setAccountNumber(getString(txnMap, "accountNumber"));
        txn.setTransactionLiteral(getString(txnMap, "transactionLiteral"));
        txn.setTransactionDate(getString(txnMap, "transactionDate"));
        txn.setValueDate(getString(txnMap, "valueDate"));
        txn.setTxnIndicator(getString(txnMap, "txnIndicator"));
        txn.setTransactionCode(getString(txnMap, "transactionCode"));
        txn.setDescription(getString(txnMap, "description"));
        txn.setChequeNo(getString(txnMap, "chequeNo"));
        txn.setTranRefNo(getString(txnMap, "tranRefNo"));

        txn.setAmount(getDouble(txnMap, "amount"));
        txn.setBalanceAmount(getDouble(txnMap, "balanceAmount"));

        return txn;
    }

    /* ------------------ SAFE READ HELPERS ------------------ */

    private String getString(
            Map<String, AttributeValue> map, String key) {
        return map.containsKey(key) ? map.get(key).s() : null;
    }

    private Double getDouble(
            Map<String, AttributeValue> map, String key) {
        return map.containsKey(key)
                ? Double.valueOf(map.get(key).n())
                : null;
    }
}
