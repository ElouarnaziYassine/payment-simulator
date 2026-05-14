package com.paymenttest.payment_simulator.controller;

import com.paymenttest.payment_simulator.model.TestResult;
import com.paymenttest.payment_simulator.websocket.ResultBroadcaster;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/test")
public class TestControlController {

    private static final String JMETER_BIN =
            "C:\\Users\\yelouarnazi\\Desktop\\apache-jmeter-5.6.3\\bin\\jmeter.bat";

    private static final String TEST_DIR =
            "C:\\Users\\yelouarnazi\\Desktop\\apache-jmeter-5.6.3\\bin\\my tests\\";

    private static final Map<String, String> TEST_FILES = Map.of(
            "card-authorization",    "Card Authorization.jmx",
            "payment-transaction",   "Payment Transaction.jmx",
            "insufficient-funds",    "Insufficient Funds.jmx",
            "duplicate-transaction", "Duplicate Transaction.jmx",
            "high-load",             "High Load.jmx",
            "stress-test",           "Stress Test.jmx",
            "refund-transaction",    "Refund Transaction.jmx",
            "invalid-card",          "Invalid Card.jmx",
            "token-expiration",      "Token Expiration.jmx",
            "atm-withdrawal",        "ATM Withdrawal.jmx"
    );

    private Process jmeterProcess = null;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ResultBroadcaster broadcaster;

    public TestControlController(ResultBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startTest(
            @RequestBody Map<String, String> body) {

        if (running.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Test is already running"
            ));
        }

        String testKey  = body.getOrDefault("testKey", "card-authorization");
        String fileName = TEST_FILES.getOrDefault(testKey, "Card Authorization.jmx");
        String testFile = TEST_DIR + fileName;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    JMETER_BIN, "-n", "-t", testFile
            );
            pb.redirectErrorStream(true);
            jmeterProcess = pb.start();
            running.set(true);

            broadcaster.broadcast(TestResult.builder()
                    .testName("SYSTEM")
                    .status("TEST_STARTED")
                    .build());

            Thread monitor = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(jmeterProcess.getInputStream())
                    );
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[JMETER] " + line);
                    }
                    jmeterProcess.waitFor();
                } catch (Exception e) {
                    System.out.println("[JMETER] Monitor error: " + e.getMessage());
                } finally {
                    running.set(false);
                    broadcaster.broadcast(TestResult.builder()
                            .testName("SYSTEM")
                            .status("TEST_STOPPED")
                            .build());
                    System.out.println("[JMETER] Test finished");
                }
            });
            monitor.setDaemon(true);
            monitor.start();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Started: " + fileName
            ));

        } catch (Exception e) {
            running.set(false);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to start: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopTest() {
        if (!running.get() || jmeterProcess == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No test is currently running"
            ));
        }
        try {
            jmeterProcess.destroyForcibly();
            running.set(false);
            broadcaster.broadcast(TestResult.builder()
                    .testName("SYSTEM")
                    .status("TEST_STOPPED")
                    .build());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test stopped"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to stop: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of("running", running.get()));
    }

    @GetMapping("/list")
    public ResponseEntity<Object> getTests() {
        return ResponseEntity.ok(TEST_FILES.entrySet().stream()
                .map(e -> Map.of(
                        "key",  e.getKey(),
                        "name", e.getValue().replace(".jmx", "")
                ))
                .toList()
        );
    }
}