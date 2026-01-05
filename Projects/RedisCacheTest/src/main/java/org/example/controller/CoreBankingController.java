package org.example.controller;

import org.example.model.CoreBankingTransactions;
import org.example.repository.DynamoWriteRepository;
import org.example.service.CoreBankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/corebanking")
public class CoreBankingController {

    private final CoreBankingService service;

    private final DynamoWriteRepository dynamoWriteRepository;

    public CoreBankingController(CoreBankingService service, DynamoWriteRepository dynamoWriteRepository) {
        this.service = service;
        this.dynamoWriteRepository = dynamoWriteRepository;
    }

    @GetMapping("/accounts/{accountNo}")
    public ResponseEntity<CoreBankingTransactions> getAccount(
            @PathVariable String accountNo) {

        CoreBankingTransactions data =
                service.getAccount(accountNo);

        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(data);
    }

    @GetMapping("/customers/{cif}")
    public ResponseEntity<?> getByCif(
            @PathVariable String cif) {

        //CoreBankingTransactions data = service.getByCif(cif);
        Map<String, Object> customerInfo = service.findByCif(cif);

        if (customerInfo == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(customerInfo);
    }

    // Controller for generic record insertion
    @PostMapping("/customers/{cif}")
    public ResponseEntity<?> insert(
            @PathVariable String cif,
            @RequestBody Map<String, Object> payload) {

        Map<String, Object> response =
                dynamoWriteRepository.putItem(cif, payload);

        return ResponseEntity.ok(response);
    }
}
