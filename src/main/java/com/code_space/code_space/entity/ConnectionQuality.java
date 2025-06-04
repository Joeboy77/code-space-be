package com.code_space.code_space.entity;

public enum ConnectionQuality {
    EXCELLENT("Excellent", "green", 90),
    GOOD("Good", "green", 70),
    FAIR("Fair", "yellow", 50),
    POOR("Poor", "red", 30),
    DISCONNECTED("Disconnected", "gray", 0);

    private final String label;
    private final String color;
    private final int score;

    ConnectionQuality(String label, String color, int score) {
        this.label = label;
        this.color = color;
        this.score = score;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }
    public int getScore() { return score; }

    public static ConnectionQuality fromMetrics(double packetLoss, int latency, int bandwidth) {
        // Excellent: < 1% loss, < 50ms latency, > 1000 kbps
        if (packetLoss < 1.0 && latency < 50 && bandwidth > 1000) {
            return EXCELLENT;
        }
        // Good: < 2% loss, < 150ms latency, > 500 kbps
        else if (packetLoss < 2.0 && latency < 150 && bandwidth > 500) {
            return GOOD;
        }
        // Fair: < 5% loss, < 300ms latency, > 100 kbps
        else if (packetLoss < 5.0 && latency < 300 && bandwidth > 100) {
            return FAIR;
        }
        // Poor: anything else with connection
        else if (bandwidth > 0) {
            return POOR;
        }
        // Disconnected: no bandwidth
        else {
            return DISCONNECTED;
        }
    }
}