package com.paymenttest.payment_simulator.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {

    private String testName;
    private String status;
    private long responseTime;
    private String maskedCard;
    private String transactionId;
    private String authorizationCode;
    private String declineReason;
    private String merchantId;
    private double amount;
    private String currency;
    private String timestamp;
    private int threadNum;
    private int iteration;
    private boolean assertionPassed;
    private boolean slaBreached;
}