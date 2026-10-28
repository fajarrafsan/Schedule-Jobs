package com.contoh.test.service;

import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import com.contoh.test.repository.BatchJobsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BatchProccessorService {
    private static final Logger log = LoggerFactory.getLogger(BatchProccessorService.class);

    private final BatchJobsRepository batchJobsRepository;

    public BatchProccessorService(BatchJobsRepository batchJobsRepository) {
        this.batchJobsRepository = batchJobsRepository;
    }

    public void proccessActiveJobs() {
        List<Batchjob> jobs = batchJobsRepository.findByStatus(Status.ACTIVE);
        for (Batchjob job : jobs) {
            log.info("Memproses Batch job id={} ({})",job.getId(),job.getJobName());
        }
    }
}
