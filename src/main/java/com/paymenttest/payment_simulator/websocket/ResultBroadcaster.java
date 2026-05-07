package com.paymenttest.payment_simulator.websocket;


import com.paymenttest.payment_simulator.model.TestResult;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ResultBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public ResultBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(TestResult result) {
        messagingTemplate.convertAndSend("/topic/results", result);
    }
}