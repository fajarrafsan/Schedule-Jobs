package com.contoh.test.service;

import com.contoh.test.dto.BatchJobResponse;
import com.contoh.test.dto.BatchJobsRequest;
import com.contoh.test.dto.BatchJobsSearchCriteria;
import com.contoh.test.dto.PageResponse;
import com.contoh.test.exception.ResourceNotFoundException;
import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import com.contoh.test.repository.BatchJobSearchRepository;
import com.contoh.test.repository.BatchJobsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.module.ResolutionException;


@Service
@Transactional
public class BatchJobsService {

    private final BatchJobsRepository batchJobsRepository;
    private final BatchJobSearchRepository batchJobSearchRepository;

    public BatchJobsService(BatchJobsRepository batchJobsRepository, BatchJobSearchRepository batchJobSearchRepository) {
        this.batchJobsRepository = batchJobsRepository;
        this.batchJobSearchRepository = batchJobSearchRepository;
    }

    public BatchJobResponse create(BatchJobsRequest request) {
        Batchjob batchjob = Batchjob.builder()
                .jobName(request.getJobName().trim())
                .jobType(request.getJobType())
                .status(request.getStatus() != null ? request.getStatus() : Status.ACTIVE)
                .retryCount(0).build();

        batchJobsRepository.save(batchjob);
        return response(batchjob);
    }

    @Transactional(readOnly = true)
    public BatchJobResponse findById(long id) {
        Batchjob batchjob = batchJobsRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No BatchJob found with id " + id));
        return response(batchjob);
    }

    public BatchJobResponse update(BatchJobsRequest request, long id) {
        Batchjob existingBatchjob = batchJobsRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No BatchJob found with id " + id));

        if (request.getStatus() != null) {
            if (request.getStatus() == Status.ACTIVE && existingBatchjob.getStatus() == Status.INACTIVE) {
                existingBatchjob.setRetryCount(0);
            }
            existingBatchjob.setStatus(request.getStatus());
        }

        existingBatchjob.setJobName(request.getJobName().trim());
        existingBatchjob.setJobType(request.getJobType().trim());

        return response(batchJobsRepository.save(existingBatchjob));
    }

    public void delete(Long id) {
        Batchjob existingBatch = batchJobsRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No BatchJob found with id " + id));
        batchJobsRepository.delete(existingBatch);
    }

    @Transactional(readOnly = true)
    public PageResponse<Batchjob> search(BatchJobsSearchCriteria criteria) {
        if (criteria.page() < 0) {
            throw new IllegalArgumentException("Page minimal 0");
        }

        if (criteria.size() < 1 || criteria.size() > 100) {
            throw new IllegalArgumentException("Size harus antara 1 sampai 100");
        }

        if (criteria.startDate() != null && criteria.endDate() != null && criteria.startDate().isAfter(criteria.endDate())) {
            throw new IllegalArgumentException("startDate tidak boleh setelah endDate");
        }
        return PageResponse.from(batchJobSearchRepository.search(criteria));
    }

    private BatchJobResponse response(Batchjob request) {
        return new BatchJobResponse(
                request.getJobName(),
                request.getJobType(),
                request.getStatus(),
                request.getRetryCount(),
                request.getCreatedAt()
        );
    }

}
