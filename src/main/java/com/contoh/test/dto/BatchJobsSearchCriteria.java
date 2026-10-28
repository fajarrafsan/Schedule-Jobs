package com.contoh.test.dto;

import com.contoh.test.model.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BatchJobsSearchCriteria(
        String jobName,
        Status status,
        String jobType,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
}
