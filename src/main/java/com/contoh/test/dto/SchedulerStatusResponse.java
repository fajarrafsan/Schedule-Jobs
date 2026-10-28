package com.contoh.test.dto;

import java.time.LocalDateTime;

public record SchedulerStatusResponse (
        String status,
        int intervalSecond,
        LocalDateTime lastRun,
        LocalDateTime nextRun
) {
}
