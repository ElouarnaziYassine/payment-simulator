package com.paymenttest.payment_simulator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.List;


@RestController
@RequestMapping("/api/v1")
public class AuthorizationController {

    @PostMapping("/authorization")
    public ResponseEntity<Map<String, Object>> authorize(
            @RequestBody Map<String, Object> request) {

        try { Thread.sleep((long)(Math.random() * 200 + 100)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Extract card number from request
        Map<String, Object> card = (Map<String, Object>) request.get("card");
        String cardNumber = card != null ? (String) card.get("number") : "";
        String cvv = card != null ? (String) card.get("cvv") : "";
        String expiryMonth = card != null ? (String) card.get("expiryMonth") : "";

        System.out.println("=== CARD RECEIVED: " + cardNumber + " | CVV: " + cvv + " | EXPIRY MONTH: " + expiryMonth);

        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String rrn   = "RRN-" + System.currentTimeMillis();

        // Simulate DECLINED cards
        List<String> declinedCards = List.of(
                "4000000000000002",
                "4000000000009995",
                "4000000000000069"
        );

        // Bad CVV simulation
        boolean badCvv = "000".equals(cvv);

        // Bad expiry simulation
        boolean badExpiry = "01".equals(expiryMonth);
        System.out.println("=== CARD RECEIVED: " + cardNumber + " | CVV: [" + cvv + "] | EXPIRY MONTH: [" + expiryMonth + "]");

        if (declinedCards.contains(cardNumber) || badCvv || badExpiry) {
            String reason = declinedCards.contains(cardNumber)
                    ? "INSUFFICIENT_FUNDS"
                    : badCvv ? "INVALID_CVV" : "CARD_EXPIRED";

            return ResponseEntity.ok(Map.of(
                    "status",          "DECLINED",
                    "transactionId",   txnId,
                    "declineReason",   reason,
                    "retrievalRefNum", rrn,
                    "timestamp",       Instant.now().toString()
            ));
        }

        // APPROVED
        String authCode = "AUTH-" + (int)(Math.random() * 900000 + 100000);
        return ResponseEntity.ok(Map.of(
                "status",            "APPROVED",
                "transactionId",     txnId,
                "authorizationCode", authCode,
                "retrievalRefNum",   rrn,
                "timestamp",         Instant.now().toString()
        ));
    }
}