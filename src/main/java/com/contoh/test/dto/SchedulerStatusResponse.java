package com.contoh.test.dto;

import java.time.LocalDateTime;

public record SchedulerStatusResponse (
        String status,
        int intervalSeconds,
        LocalDateTime lastRun,
        LocalDateTime nextRun
) {
}
