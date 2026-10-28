package com.contoh.test.service;

import com.contoh.test.dto.BatchJobResponse;
import com.contoh.test.dto.BatchJobsRequest;
import com.contoh.test.exception.ResourceNotFoundException;
import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import com.contoh.test.repository.BatchJobSearchRepository;
import com.contoh.test.repository.BatchJobsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatchJobServiceTest {

    @Mock
    private BatchJobsRepository batchJobsRepository;

    @Mock
    private BatchJobSearchRepository batchJobSearchRepository;

    @InjectMocks
    private BatchJobsService batchJobsService;

    private Batchjob existingJob;

    @BeforeEach
    void setUp() {
        existingJob = new Batchjob();
        existingJob.setId(1L);
        existingJob.setJobName("Sync Data");
        existingJob.setJobType("SYNC");
        existingJob.setStatus(Status.ACTIVE);
        existingJob.setRetryCount(0);
        existingJob.setCreatedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    @Test
    @DisplayName("create: tanpa status -> default ACTIVE, retryCount 0, jobName di-trim")
    void create_tanpaStatus() {
        BatchJobsRequest request = new BatchJobsRequest("  Sync Data  ", "SYNC", null);
        when(batchJobsRepository.save(any(Batchjob.class))).thenAnswer(inv -> inv.getArgument(0));

        batchJobsService.create(request);

        ArgumentCaptor<Batchjob> captor = ArgumentCaptor.forClass(Batchjob.class);
        verify(batchJobsRepository).save(captor.capture());
        Batchjob saved = captor.getValue();

        assertThat(saved.getJobName()).isEqualTo("Sync Data");
        assertThat(saved.getJobType()).isEqualTo("SYNC");
        assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("create: status terkirim -> status di pakai")
    void create_denganStatus() {
        BatchJobsRequest request = new BatchJobsRequest("  Sync Data  ", "SYNC", Status.INACTIVE);
        when(batchJobsRepository.save(any(Batchjob.class))).thenAnswer(inv -> inv.getArgument(0));

        batchJobsService.create(request);

        ArgumentCaptor<Batchjob> captor = ArgumentCaptor.forClass(Batchjob.class);
        verify(batchJobsRepository).save(captor.capture());
        Batchjob saved = captor.getValue();

        assertThat(saved.getJobName()).isEqualTo("Sync Data");
        assertThat(saved.getJobType()).isEqualTo("SYNC");
        assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("getById: id ada -> return BatchJob")
    void getById_ada() {
        when(batchJobsRepository.findById(1L)).thenReturn(Optional.of(existingJob));

        BatchJobResponse result = batchJobsService.findById(1L);

        assertThat(result.jobName()).isEqualTo("Sync Data");
    }

    @Test
    @DisplayName("getById: id tidak ada -> ResourceNotFoundException")
    void getById_tidakAda() {
        when(batchJobsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchJobsService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update: id ada -> jobName, JobType, status berubah")
    void update_ada() {
        when(batchJobsRepository.findById(1L)).thenReturn(Optional.of(existingJob));
        when(batchJobsRepository.save(any(Batchjob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BatchJobsRequest request = new BatchJobsRequest("Sync Data v2", "REPORT", Status.INACTIVE);

        BatchJobResponse result = batchJobsService.update(request, 1L);

        assertThat(result.jobName()).isEqualTo("Sync Data v2");
        assertThat(result.jobType()).isEqualTo("REPORT");
        assertThat(result.status()).isEqualTo(Status.INACTIVE);
        assertThat(result.createdAt()).isEqualTo(existingJob.getCreatedAt());
    }

    @Test
    @DisplayName("update: FAILED di ubah ke ACTIVE -> retryCount di reset ke 0")
    void update_failedJadiActive_resetRetryCount() {
        existingJob.setStatus(Status.INACTIVE);
        existingJob.setRetryCount(2);
        when(batchJobsRepository.findById(1L)).thenReturn(Optional.of(existingJob));
        when(batchJobsRepository.save(any(Batchjob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BatchJobsRequest request = new BatchJobsRequest("Sync Data", "SYNC", Status.ACTIVE);

        BatchJobResponse result = batchJobsService.update(request, 1L);

        assertThat(result.status()).isEqualTo(Status.ACTIVE);
        assertThat(result.retryCount()).isZero();
    }

    @Test
    @DisplayName("update: status tidak di kirim -> status lama tetap")
    void update_statusNull_statusTetap() {
        when(batchJobsRepository.findById(1L)).thenReturn(Optional.of(existingJob));
        when(batchJobsRepository.save(any(Batchjob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BatchJobsRequest request = new BatchJobsRequest("Sync Data v2", "REPORT", null);

        BatchJobResponse result = batchJobsService.update(request, 1L);
        assertThat(result.status()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("update: id tidak ada -> ResourceNotFoundException, save tidak dipanggil")
    void update_idtidakAda() {
        when(batchJobsRepository.findById(99L)).thenReturn(Optional.empty());
        BatchJobsRequest request = new BatchJobsRequest("Sync Data", "SYNC", null);

        assertThatThrownBy(() -> batchJobsService.update(request, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(batchJobsRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete: id ada -> repository.delete dipanggil")
    void delete_Ada() {
        when(batchJobsRepository.findById(1L)).thenReturn(Optional.of(existingJob));

        batchJobsService.delete(1L);
        verify(batchJobsRepository).delete(existingJob);
    }

    @Test
    @DisplayName("delete: id tidak ada -> ")
    void delete_tidakAda() {
        when(batchJobsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchJobsService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(batchJobsRepository, never()).delete(any());
    }
}
