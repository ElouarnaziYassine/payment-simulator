package com.paymenttest.payment_simulator.controller;

import com.paymenttest.payment_simulator.model.TestResult;
import com.paymenttest.payment_simulator.websocket.ResultBroadcaster;
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

    private static final String TEST_FILE =
            "C:\\Users\\yelouarnazi\\Desktop\\apache-jmeter-5.6.3\\bin\\my tests\\Card Authorization.jmx";

    private Process jmeterProcess = null;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ResultBroadcaster broadcaster;

    public TestControlController(ResultBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startTest() {
        if (running.get()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Test is already running"
            ));
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    JMETER_BIN,
                    "-n",
                    "-t", TEST_FILE
            );
            pb.redirectErrorStream(true);
            jmeterProcess = pb.start();
            running.set(true);

            // Notify Angular test started
            broadcaster.broadcast(TestResult.builder()
                    .testName("SYSTEM")
                    .status("TEST_STARTED")
                    .build());

            // Monitor process in background
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
                    // Notify Angular test stopped
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
                    "message", "Test started successfully"
            ));

        } catch (Exception e) {
            running.set(false);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to start test: " + e.getMessage()
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
                    "message", "Test stopped successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to stop test: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "running", running.get()
        ));
    }
}