package com.contoh.test.service;

import com.contoh.test.exception.RetryExhaustedException;
import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import com.contoh.test.repository.BatchJobsRepository;
import com.contoh.test.retry.RetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BatchProcessorService {
    private static final Logger log = LoggerFactory.getLogger(BatchProcessorService.class);

    private final BatchJobsRepository batchJobsRepository;
    private final ExternalProcessingClient externalProcessingCLient;
    private final RetryExecutor retryExecutor;


    public BatchProcessorService(BatchJobsRepository batchJobsRepository, ExternalProcessingClient externalProcessingCLient, RetryExecutor retryExecutor) {
        this.batchJobsRepository = batchJobsRepository;
        this.externalProcessingCLient = externalProcessingCLient;
        this.retryExecutor = retryExecutor;
    }

    public void processActiveJobs() {
        List<Batchjob> jobs = batchJobsRepository.findByStatus(Status.ACTIVE);
        log.info("Memproses {} BatchJob ACTIVE", jobs.size());
        for (Batchjob job : jobs) {
            try {
                processOne(job);
            } catch (Exception ex) {
                // Satu job error tidak boleh menghentikan job lain
                log.error("Error tak terduga saat memproses job id={}", job.getId(), ex);
            }

        }
    }

    private void processOne(Batchjob job) {
        try {
            int attempts = retryExecutor.execute("BatchJob id=" + job.getId(),
                    () -> externalProcessingCLient.process(job));
            job.setRetryCount(attempts - 1);
        } catch (RetryExhaustedException ex) {
            log.error("BatchJob id={} FAILED: {}", job.getId(), ex.getMessage());
            job.setRetryCount(ex.getAttempts() - 1);
            job.setStatus(Status.FAILED);
        } catch (RuntimeException ex) {
            // Error permanen (bukan timeout): tidak di-retry, langsung FAILED
            log.error("BatchJob id={} gagal permanen: {}", job.getId(), ex.getMessage());
            job.setStatus(Status.FAILED);
        }
        batchJobsRepository.save(job);
    }
}
