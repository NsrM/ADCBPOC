package org.example.controller;

import org.example.service.RetailTransactionService;
import org.example.util.DynamoAttributeParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/retail/transactions")
public class RetailTransactionController {

    private final RetailTransactionService service;
    private final DynamoAttributeParser parser;

    public RetailTransactionController(
            RetailTransactionService service,
            DynamoAttributeParser parser) {
        this.service = service;
        this.parser = parser;
    }

    @GetMapping("/{cif}")
    public Map<String, Object> getAllByCif(@PathVariable String cif) {

        List<Map<String, AttributeValue>> items = service.getByCif(cif);

        List<Map<String, Object>> transactions = items.stream()
                .map(DynamoAttributeParser::parseItem)
                .toList();

        return Map.of(
                "cif", cif,
                "transactions", transactions
        );
    }

    @GetMapping("/{cif}/range")
    public Map<String, Object> getByDateRange(
            @PathVariable String cif,
            @RequestParam String from,
            @RequestParam String to) {

        List<Map<String, AttributeValue>> items =
                service.getByCifAndDateRange(cif, from, to);

        List<Map<String, Object>> transactions = items.stream()
                .map(DynamoAttributeParser::parseItem)
                .toList();

        return Map.of(
                "cif", cif,
                "from", from,
                "to", to,
                "transactions", transactions
        );
    }

    @GetMapping("/{cif}/latest")
    public Map<String, Object> getLatest(@PathVariable String cif) {

        return service.getLatestByCif(cif)
                .map(DynamoAttributeParser::parseItem)
                .map(txn -> Map.of(
                        "cif", cif,
                        "transaction", txn
                ))
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("cif", cif);
                    response.put("transaction", null);
                    return response;
                });
        // Can try below else also instead of transaction null for cleaner rest semantics
        // .orElse(ResponseEntity.noContent().build());
    }
}

