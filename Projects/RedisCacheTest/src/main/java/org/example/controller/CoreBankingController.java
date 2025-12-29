package org.example.controller;

import org.example.model.CoreBankingTransactions;
import org.example.service.CoreBankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/corebanking")
public class CoreBankingController {

    private final CoreBankingService service;

    public CoreBankingController(CoreBankingService service) {
        this.service = service;
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
}
