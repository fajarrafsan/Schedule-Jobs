package com.contoh.test.controller;

import com.contoh.test.controllers.BatchJobController;
import com.contoh.test.dto.BatchJobsSearchCriteria;
import com.contoh.test.dto.PageResponse;
import com.contoh.test.model.Batchjob;
import com.contoh.test.model.Status;
import com.contoh.test.service.BatchJobsService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@WebMvcTest(BatchJobController.class)
public class BatchJobsControllerSearchTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchJobsService service;

    @Test
    @DisplayName("tanpa parameter -> pakai default: page 0, size 10, createdAt, asc")
    void search_default() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/batch-jobs"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(service).search(new BatchJobsSearchCriteria(null, null, null, null, null, 0, 10, "createdAt", "desc"));
    }

    @Test
    @DisplayName("semua parameter -> terbaca benar (enum status & tanggal format ISO)")
    void search_semuaParameter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/batch-jobs")
                        .param("jobName", "sync")
                        .param("status", "ACTIVE")
                        .param("jobType", "SYNC")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "jobName")
                        .param("sortDirection", "asc"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(service).search(new BatchJobsSearchCriteria(
                "sync", Status.ACTIVE, "SYNC",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                1, 5, "jobName", "asc"));
    }

    @Test
    @DisplayName("response -> bentuk JSON PageResponse")
    void search_bentukResponse() throws Exception {
        Batchjob job = new Batchjob();
        job.setId(1L);
        job.setJobName("Sync Data");
        job.setJobType("SYNC");
        job.setStatus(Status.ACTIVE);
        job.setRetryCount(0);
        job.setCreatedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
        Mockito.when(service.search(Mockito.any())).thenReturn(new PageResponse<>(List.of(job), 0, 10, 1, 1, true));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/batch-jobs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].jobName").value("Sync Data"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("status tidak dikenal -> 400, service tidak dipanggil")
    void search_statusTidakValid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/batch-jobs").param("status", "RUNNING"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        Mockito.verify(service, Mockito.never()).search(Mockito.any());
    }

    @Test
    @DisplayName("format tanggal salah -> 400, service tidak dipanggil")
    void search_formatTanggalSalah() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/batch-jobs").param("startDate", "01-10-2026"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        Mockito.verify(service, Mockito.never()).search(Mockito.any());
    }

}
