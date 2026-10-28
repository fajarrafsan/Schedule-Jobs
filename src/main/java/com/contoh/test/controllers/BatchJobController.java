package com.contoh.test.controllers;

import com.contoh.test.dto.BatchJobResponse;
import com.contoh.test.dto.BatchJobsRequest;
import com.contoh.test.dto.BatchJobsSearchCriteria;
import com.contoh.test.dto.PageResponse;
import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import com.contoh.test.service.BatchJobsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/batch-jobs")
public class BatchJobController {

    private final BatchJobsService batchJobsService;

    public BatchJobController(BatchJobsService batchJobsService) {
        this.batchJobsService = batchJobsService;
    }

    @PostMapping
    public ResponseEntity<BatchJobResponse> create(@Valid @RequestBody BatchJobsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchJobsService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchJobResponse> getById(@PathVariable long id) {
        return ResponseEntity.status(HttpStatus.OK).body(batchJobsService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BatchJobResponse> update(@PathVariable long id, @RequestBody BatchJobsRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(batchJobsService.update(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        batchJobsService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body("Success deleting");
    }

    @GetMapping
    public PageResponse<Batchjob> search(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
            ){
        return batchJobsService.search(new BatchJobsSearchCriteria(jobName, status, jobType, startDate, endDate, page, size, sortBy, sortDirection));
    }
}
