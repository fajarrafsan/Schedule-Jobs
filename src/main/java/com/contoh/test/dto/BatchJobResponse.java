package com.contoh.test.dto;

import com.contoh.test.model.Status;

import java.time.LocalDateTime;

public record BatchJobResponse(
        String jobName,
        String jobType,
        Status status,
        Integer retryCount,
        LocalDateTime createdAt

) {
}
