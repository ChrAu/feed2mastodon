package com.hexix.traffic;

public record ServerMetrics(double netInMBs, double netOutMBs, double cpuUsage, double memUsage, long uptime) {
}