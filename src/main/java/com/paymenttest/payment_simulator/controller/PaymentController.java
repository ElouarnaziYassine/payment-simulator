package com.paymenttest.payment_simulator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestBody Map<String, Object> request) {

        // Simulate processing delay
        try { Thread.sleep((long)(Math.random() * 200 + 100)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Extract card details
        Map<String, Object> card = (Map<String, Object>) request.get("card");
        String cardNumber = card != null ? (String) card.get("number") : "";
        String cvv        = card != null ? (String) card.get("cvv")    : "";

        // Extract amount
        Map<String, Object> amount = (Map<String, Object>) request.get("amount");
        double value = amount != null ? ((Number) amount.get("value")).doubleValue() : 0;

        // Extract auth ref
        String authRef  = (String) request.getOrDefault("authorizationRef", "");
        String authCode = (String) request.getOrDefault("originalAuthCode", "");

        System.out.println("=== PAYMENT RECEIVED: " + cardNumber +
                " | Amount: " + value +
                " | AuthRef: " + authRef);

        String paymentId     = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String rrn           = "RRN-" + System.currentTimeMillis();

        // Declined cards
        List<String> declinedCards = List.of(
                "4000000000000002",
                "4000000000009995",
                "4000000000000069"
        );

        boolean badCvv = "000".equals(cvv);

        if (declinedCards.contains(cardNumber) || badCvv) {
            String reason = declinedCards.contains(cardNumber)
                    ? "INSUFFICIENT_FUNDS"
                    : "INVALID_CVV";

            return ResponseEntity.ok(Map.of(
                    "status",         "DECLINED",
                    "paymentId",      paymentId,
                    "transactionId",  transactionId,
                    "declineReason",  reason,
                    "balanceUpdated", false,
                    "storedInDb",     true,
                    "retrievalRefNum",rrn,
                    "timestamp",      Instant.now().toString()
            ));
        }

        // SUCCESS
        return ResponseEntity.ok(Map.of(
                "status",         "SUCCESS",
                "paymentId",      paymentId,
                "transactionId",  transactionId,
                "authorizationRef", authRef,
                "balanceUpdated", true,
                "storedInDb",     true,
                "retrievalRefNum",rrn,
                "amount",         value,
                "currency",       "MAD",
                "timestamp",      Instant.now().toString()
        ));
    }
}