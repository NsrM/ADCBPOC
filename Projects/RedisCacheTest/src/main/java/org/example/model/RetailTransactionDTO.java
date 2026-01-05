package org.example.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RetailTransactionDTO {

    private String accountId;
    private String cif;
    private String txnId;

    private BigDecimal amount;
    private String currency;
    private String txnType;
    private String status;

    private String txnTimestamp;
}

