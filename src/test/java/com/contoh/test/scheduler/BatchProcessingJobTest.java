package com.contoh.test.scheduler;

import com.contoh.test.service.BatchProcessorService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

@ExtendWith(MockitoExtension.class)
public class BatchProcessingJobTest {
    @Mock
    private BatchProcessorService batchProcessorService;

    @Mock
    private JobExecutionContext context;

    @InjectMocks
    private BatchProcessingJob job;

    @Test
    @DisplayName("saat dijalankan Quartz -> memanggil processActiveJobs()")
    void executeInternal_memanggilService() {
        job.executeInternal(context);

        Mockito.verify(batchProcessorService).processActiveJobs();
    }

    @Test
    @DisplayName("punya @DisallowConcurrentExecution -> putaran berikutnya menunggu yang sebelumnya selesai")
    void tidakBolehJalanBersamaan() {
        Assertions.assertThat(BatchProcessingJob.class.isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
    }
}
