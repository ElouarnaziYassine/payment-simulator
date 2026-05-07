package com.paymenttest.payment_simulator.controller;

import com.paymenttest.payment_simulator.model.TestResult;
import com.paymenttest.payment_simulator.websocket.ResultBroadcaster;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
public class ResultsController {

    private final ResultBroadcaster broadcaster;

    public ResultsController(ResultBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @PostMapping("/live")
    public ResponseEntity<Void> receiveResult(@RequestBody TestResult result) {
        broadcaster.broadcast(result);
        System.out.println("=== RESULT RECEIVED: " + result.getTestName() +
                " | " + result.getStatus() +
                " | " + result.getResponseTime() + "ms" +
                " | Card: " + result.getMaskedCard());
        return ResponseEntity.ok().build();
    }
}