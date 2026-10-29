package com.contoh.test.controller;


import com.contoh.test.controllers.BatchJobController;
import com.contoh.test.controllers.SchedulerController;
import com.contoh.test.dto.BatchJobResponse;
import com.contoh.test.dto.BatchJobsRequest;
import com.contoh.test.dto.SchedulerStatusResponse;
import com.contoh.test.exception.ResourceNotFoundException;
import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import com.contoh.test.service.BatchJobsService;
import com.contoh.test.service.SchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

@WebMvcTest(SchedulerController.class)
public class SchedulerControllerTest {
    private static final LocalDateTime LAST_RUN = LocalDateTime.of(2026, 10, 29, 10, 0, 30);
    private static final LocalDateTime NEXT_RUN = LocalDateTime.of(2026, 10, 29, 10, 1, 30);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchedulerService schedulerService;

    @Test
    @DisplayName("POST /start -> 200, status RUNNING")
    void start() throws Exception {
        when(schedulerService.start())
                .thenReturn(new SchedulerStatusResponse("RUNNING", 60, null, NEXT_RUN));

        mockMvc.perform(post("/api/scheduler/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.intervalSeconds").value(60))
                .andExpect(jsonPath("$.lastRun").value(nullValue()))
                .andExpect(jsonPath("$.nextRun").value("2026-10-29T10:01:30"));

        verify(schedulerService).start();
    }

    @Test
    @DisplayName("POST /stop -> 200, status STOPPED")
    void stop() throws Exception {
        when(schedulerService.stop())
                .thenReturn(new SchedulerStatusResponse("STOPPED", 60, null, null));

        mockMvc.perform(post("/api/scheduler/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.nextRun").value(nullValue()));

        verify(schedulerService).stop();
    }

    @Test
    @DisplayName("GET /status -> 200, berisi lastRun & nextRun")
    void getStatus() throws Exception { // jangan beri nama status(): bentrok dengan status() milik MockMvc
        when(schedulerService.status())
                .thenReturn(new SchedulerStatusResponse("RUNNING", 60, LAST_RUN, NEXT_RUN));

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.lastRun").value("2026-10-29T10:00:30"))
                .andExpect(jsonPath("$.nextRun").value("2026-10-29T10:01:30"));
    }

    @Test
    @DisplayName("Quartz error -> 500 dengan pesan dari GlobalExceptionHandler")
    void start_errorQuartz() throws Exception {
        when(schedulerService.start())
                .thenThrow(new SchedulerException("Scheduler sedang shutdown"));

        mockMvc.perform(post("/api/scheduler/start"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Scheduler error: Scheduler sedang shutdown"));
    }

    @Test
    @DisplayName("start harus POST -> GET /start ditolak 405")
    void start_methodSalah() throws Exception {
        mockMvc.perform(get("/api/scheduler/start"))
                .andExpect(status().isMethodNotAllowed());

        verify(schedulerService, never()).start();
    }

    @WebMvcTest(BatchJobController.class)
    public static class BatchJobControllerTest {
        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private BatchJobsService service;

        private BatchJobResponse job(Long id, String name, Status status) {
            Batchjob job = new Batchjob();
            job.setId(id);
            job.setJobName(name);
            job.setJobType("SYNC");
            job.setStatus(status);
            job.setRetryCount(0);
            job.setCreatedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
            return new BatchJobResponse(job.getJobName(), job.getJobType(), job.getStatus(), job.getRetryCount(), job.getCreatedAt());
        }

        @Test
        @DisplayName("POST valid -> 201 Created")
        void create_valid() throws Exception {
            when(service.create(any(BatchJobsRequest.class)))
                    .thenReturn(job(1L, "Sync Data", Status.ACTIVE));

            mockMvc.perform(post("/api/batch-jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"jobName" : "Sync Data", "jobType": "SYNC"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.jobName").value("Sync Data"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.retryCount").value(0));
        }

        @Test
        @DisplayName("POST jobName kosong -> 400, service tidak di panggil")
        public void create_jobNameKosong() throws Exception {
            mockMvc.perform(post("/api/batch-jobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"jobName" : "", "jobType": "SYNC"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("jobName")));

            verify(service, never()).create(any());
        }

        @Test
        @DisplayName("GET /{id} ada -> 200")
        void getById_ada() throws Exception {
            when(service.findById(1L)).thenReturn(job(1L, "Sync Data", Status.ACTIVE));

            mockMvc.perform(get("/api/batch-jobs/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobName").value("Sync Data"));
        }

        @Test
        @DisplayName("GET /{id} tidak ada -> 404")
        void getById_tidakAda() throws Exception {
            when(service.findById(99L))
                    .thenThrow(new ResourceNotFoundException("BatchJob dengan id 99 tidak ditemukan"));

            mockMvc.perform(get("/api/batch-jobs/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("BatchJob dengan id 99 tidak ditemukan"));
        }

        @Test
        @DisplayName("PUT valid -> 200")
        void update_valid() throws Exception {
            when(service.update(any(BatchJobsRequest.class), eq(1L)))
                    .thenReturn(job(1L, "Sync Data v2", Status.INACTIVE));

            mockMvc.perform(put("/api/batch-jobs/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"jobName": "Sync Data v2", "jobType": "SYNC", "status": "INACTIVE"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobName").value("Sync Data v2"))
                    .andExpect(jsonPath("$.status").value("INACTIVE"));
        }

        @Test
        @DisplayName("PUT status tidak dikenal -> 400")
        void update_statusTidakValid() throws Exception {
            mockMvc.perform(put("/api/batch-jobs/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"jobName": "Sync Data", "status": "RUNNING"}
                                    """))
                    .andExpect(status().isBadRequest());

            verify(service, never()).update(any(), anyLong());
        }


        @Test
        @DisplayName("DELETE ada -> 204 No Content")
        void delete_ada() throws Exception {
            mockMvc.perform(delete("/api/batch-jobs/1"))
                    .andExpect(status().isNoContent());

            verify(service).delete(1L);
        }

        @Test
        @DisplayName("DELETE tidak ada -> 404")
        void delete_tidakAda() throws Exception {
            doThrow(new ResourceNotFoundException("BatchJob dengan id 99 tidak ditemukan"))
                    .when(service).delete(99L);

            mockMvc.perform(delete("/api/batch-jobs/99"))
                    .andExpect(status().isNotFound());
        }

    }
}
